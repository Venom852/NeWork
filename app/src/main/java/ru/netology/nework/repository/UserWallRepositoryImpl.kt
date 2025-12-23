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
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.dao.PostUserWallRemoteKeyDao
import ru.netology.nework.entity.PostUserWallEntity
import ru.netology.nework.error.ErrorCode404
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class UserWallRepositoryImpl @Inject constructor(
    private val postUserWallDao: PostUserWallDao,
    private val authorIdDao: AuthorIdDao,
    private val apiService: ApiService,
    appDb: AppDb,
    postUserWallRemoteKeyDao: PostUserWallRemoteKeyDao,
) : UserWallRepository {
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
        pagingSourceFactory = { postUserWallDao.getPagingSource() },
        remoteMediator = UserWallRemoteMediator(apiService, appDb, postUserWallDao, authorIdDao, postUserWallRemoteKeyDao)
    ).flow.map {
        it.map(PostUserWallEntity::toPostUserWallDto)
    }

    var authorId = 0L

    override suspend fun likeById(id: Long, postLikedByMe: Boolean?) {
        try {
            val job = CoroutineScope(Dispatchers.IO).launch {
                authorId = authorIdDao.getAuthorId()
            }

            job.join()

            if (postLikedByMe != null && !postLikedByMe) {
                val response = apiService.likeByIdWallPost(authorId, id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() == 403) {
                    throw ErrorCode403
                }

                if (response.code() == 404) {
                    throw ErrorCode404
                }

                throw ApiError(response.code(), response.message())
            } else {
                val response = apiService.dislikeByIdWallPost(authorId, id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() == 403) {
                    throw ErrorCode403
                }

                if (response.code() == 404) {
                    throw ErrorCode404
                }

                throw ApiError(response.code(), response.message())
            }
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: ErrorCode404) {
            throw ErrorCode404
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }
}