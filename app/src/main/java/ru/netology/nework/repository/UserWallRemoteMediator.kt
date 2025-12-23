package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.dao.PostUserWallRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostUserRemoteKeyEntity
import ru.netology.nework.entity.PostUserWallEntity
import ru.netology.nework.entity.toPostUserWallEntity
import ru.netology.nework.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class UserWallRemoteMediator(
    private val apiService: ApiService,
    private val appDb: AppDb,
    private val postUserWallDao: PostUserWallDao,
    private val authorIdDao: AuthorIdDao,
    private val postUserWallRemoteKeyDao: PostUserWallRemoteKeyDao,
) : RemoteMediator<Int, PostUserWallEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostUserWallEntity>
    ): MediatorResult {
        var authorId = 0L

        try {
            val job = CoroutineScope(Dispatchers.IO).launch {
                authorId = authorIdDao.getAuthorId()
            }

            job.join()

            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (postUserWallRemoteKeyDao.max() == null) {
                        apiService.getLatestWallPosts(authorId, state.config.initialLoadSize)
                    } else {
                        val id = postUserWallRemoteKeyDao.max()!!
                        apiService.getAfterWallPosts(authorId, id, state.config.pageSize)
                    }
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = postUserWallRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBeforeWallPosts(authorId, id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )

            if (body.isEmpty()) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            appDb.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    if (postUserWallRemoteKeyDao.max() == null) {
                        postUserWallRemoteKeyDao.insertList(
                            listOf(
                                PostUserRemoteKeyEntity(
                                    type = PostUserRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id,
                                ),
                                PostUserRemoteKeyEntity(
                                    type = PostUserRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id,
                                ),
                            )
                        )
                    } else {
                        postUserWallRemoteKeyDao.insert(
                            PostUserRemoteKeyEntity(
                                type = PostUserRemoteKeyEntity.KeyType.AFTER,
                                id = body.first().id,
                            )
                        )
                    }
                } else {
                    postUserWallRemoteKeyDao.insert(
                        PostUserRemoteKeyEntity(
                            type = PostUserRemoteKeyEntity.KeyType.BEFORE,
                            id = body.last().id,
                        )
                    )
                }
                postUserWallDao.insertPosts(body.toPostUserWallEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            return MediatorResult.Error(e)
        }
    }
}