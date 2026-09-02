package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.entity.toPostUserWallDto
import ru.netology.nework.entity.toPostUserWallEntity
import ru.netology.nework.error.ErrorCode404
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class PostUserWallRepositoryImpl @Inject constructor(
    private val dao: PostUserWallDao,
    private val authorIdDao: AuthorIdDao,
    private val apiService: ApiService,
//    appDb: AppDb,
//    postUserWallRemoteKeyDao: PostUserWallRemoteKeyDao,
) : PostUserWallRepository {
//    override val data: Flow<PagingData<Post>> = Pager(
//        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
//        pagingSourceFactory = { postUserWallDao.getPagingSource() },
//        remoteMediator = UserWallRemoteMediator(apiService, appDb, postUserWallDao, authorIdDao, postUserWallRemoteKeyDao)
//    ).flow.map {
//        it.map(PostUserWallEntity::toPostUserWallDto)
//    }

    var authorId = 0L

    override val data: Flow<List<Post>> = dao.getAllFlow().map { it.toPostUserWallDto() }

    override suspend fun getAll() {
        try {
            val job = CoroutineScope(Dispatchers.IO).launch {
                authorId = authorIdDao.getAuthorId().id
            }

            job.join()

            print(authorId)
            val response = apiService.getAllWallPosts(authorId)

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insertPosts(body.map {
                    it.copy(
                        likes = it.likeOwnerIds.count().toLong()
                    )
                }.toPostUserWallEntity())

                return
            }

            throw ApiError(response.code(), response.message())
        } catch (_: IOException) {
            throw NetworkError()
        } catch (_: Exception) {
            throw UnknownError()
        }
    }

    override suspend fun likeById(id: Long, postLikedByMe: Boolean?) {
        try {
            val job = CoroutineScope(Dispatchers.IO).launch {
                authorId = authorIdDao.getAuthorId().id
            }

            job.join()

            if (postLikedByMe != null && !postLikedByMe) {
                val response = apiService.likeByIdWallPost(authorId, id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() == 403) {
                    throw ErrorCode403()
                }

                if (response.code() == 404) {
                    throw ErrorCode404()
                }

                throw ApiError(response.code(), response.message())
            } else {
                val response = apiService.dislikeByIdWallPost(authorId, id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() == 403) {
                    throw ErrorCode403()
                }

                if (response.code() == 404) {
                    throw ErrorCode404()
                }

                throw ApiError(response.code(), response.message())
            }
        } catch (_: ErrorCode403) {
            throw ErrorCode403()
        } catch (_: ErrorCode404) {
            throw ErrorCode404()
        } catch (_: IOException) {
            throw NetworkError()
        } catch (_: Exception) {
            throw UnknownError()
        }
    }
}