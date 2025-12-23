package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException
import ru.netology.nework.error.AppError
import javax.inject.Inject
import javax.inject.Singleton
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import ru.netology.nework.dao.PostMyWallDao
import ru.netology.nework.dao.PostMyWallRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class MyWallRepositoryImpl @Inject constructor(
    private val postMyWallDao: PostMyWallDao,
    private val apiService: ApiService,
    appDb: AppDb,
    postMyWallRemoteKeyDao: PostMyWallRemoteKeyDao,
) : MyWallRepository {
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
        pagingSourceFactory = { postMyWallDao.getPagingSource() },
        remoteMediator = MyWallRemoteMediator(apiService, appDb, postMyWallDao, postMyWallRemoteKeyDao)
    ).flow.map {
        it.map(PostMyWallEntity::toPostMyWallDto)
    }

    override suspend fun likeById(id: Long, postLikedByMe: Boolean?) {
        try {
            if (postLikedByMe != null && !postLikedByMe) {
                val response = apiService.likeByIdMyWallPost(id)

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
                val response = apiService.dislikeByIdMyWallPost(id)

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

    override suspend fun save(post: Post): Post {
        try {
            val response = apiService.savePost(post)

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                return body
            }

            if (response.code() == 403) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: AppError) {
            throw e
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeByIdPost(id)

            if (response.isSuccessful) {
                return
            }

            if (response.code() == 403) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType): Post {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(
                attachment = Attachment(
                    media.id,
                    attachmentType,
                )
            )
            return save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: ErrorCode415) {
            throw ErrorCode415
        }catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
            }

            if (response.code() == 403) {
                throw ErrorCode403
            }

            if (response.code() == 415) {
                throw ErrorCode415
            }

            throw ApiError(response.code(), response.message())
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: ErrorCode415) {
            throw ErrorCode415
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }
}