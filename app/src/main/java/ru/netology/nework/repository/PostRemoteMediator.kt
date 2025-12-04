package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.PostRemoteKeyEntity
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val appDb: AppDb,
    private val dao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
) : RemoteMediator<Int, PostEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (postRemoteKeyDao.max() == null) {
                        apiService.getLatest(state.config.initialLoadSize)
                    } else {
                        val id = postRemoteKeyDao.max()!!
                        apiService.getAfter(id, state.config.pageSize)
                    }
                }

                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBefore(id, state.config.pageSize)
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
                    if (postRemoteKeyDao.max() == null) {
                        postRemoteKeyDao.insertList(
                            listOf(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    id = body.first().id,
                                ),
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.BEFORE,
                                    id = body.last().id,
                                ),
                            )
                        )
                    } else {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.AFTER,
                                id = body.first().id,
                            )
                        )
                    }
                } else {
                    postRemoteKeyDao.insert(
                        PostRemoteKeyEntity(
                            type = PostRemoteKeyEntity.KeyType.BEFORE,
                            id = body.last().id,
                        )
                    )
                }
                dao.insertPosts(body.toEntity())
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