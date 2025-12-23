package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.PostMyWallDao
import ru.netology.nework.dao.PostMyWallRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.entity.PostMyWallRemoteKeyEntity
import ru.netology.nework.entity.toPostMyWallEntity
import ru.netology.nework.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class MyWallRemoteMediator(
    private val apiService: ApiService,
    private val appDb: AppDb,
    private val postMyWallDao: PostMyWallDao,
    private val postMyWallRemoteKeyDao: PostMyWallRemoteKeyDao,
) : RemoteMediator<Int, PostMyWallEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostMyWallEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (postMyWallRemoteKeyDao.max() == null) {
                        apiService.getLatestMyWallPosts(state.config.initialLoadSize)
                    } else {
                        val id = postMyWallRemoteKeyDao.max()!!
                        apiService.getAfterMyWallPosts(id, state.config.pageSize)
                    }
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = postMyWallRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBeforeMyWallPosts(id, state.config.pageSize)
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
                    if (postMyWallRemoteKeyDao.max() == null) {
                        postMyWallRemoteKeyDao.insertList(
                            listOf(
                                PostMyWallRemoteKeyEntity(
                                    type = PostMyWallRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id,
                                ),
                                PostMyWallRemoteKeyEntity(
                                    type = PostMyWallRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id,
                                ),
                            )
                        )
                    } else {
                        postMyWallRemoteKeyDao.insert(
                            PostMyWallRemoteKeyEntity(
                                type = PostMyWallRemoteKeyEntity.KeyType.AFTER,
                                id = body.first().id,
                            )
                        )
                    }
                } else {
                    postMyWallRemoteKeyDao.insert(
                        PostMyWallRemoteKeyEntity(
                            type = PostMyWallRemoteKeyEntity.KeyType.BEFORE,
                            id = body.last().id,
                        )
                    )
                }
                postMyWallDao.insertPosts(body.toPostMyWallEntity())
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