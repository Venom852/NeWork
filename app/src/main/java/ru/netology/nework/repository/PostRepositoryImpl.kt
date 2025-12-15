package ru.netology.nework.repository

import android.app.Application
import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.toEntity
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
import androidx.paging.TerminalSeparatorType
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.dto.Ad
import ru.netology.nework.dto.FeedItem
import ru.netology.nework.dto.DatePost
import ru.netology.nework.entity.toDto
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import java.time.*

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: ApiService,
    appDb: AppDb,
    postRemoteKeyDao: PostRemoteKeyDao,
    private val application: Application
) : PostRepository {
    override val data: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
        pagingSourceFactory = { dao.getPagingSource() },
        remoteMediator = PostRemoteMediator(apiService, appDb, dao, postRemoteKeyDao)
    ).flow.map {
        it.map(PostEntity::toDto)
            .insertSeparators(TerminalSeparatorType.SOURCE_COMPLETE) { previousOne, previousTwo ->
                val publishedOne = LocalDateTime.parse(
                    Instant.ofEpochSecond(
                        previousOne?.published ?: 0
                    ).toString().dropLast(1))
                val publishedTwo = LocalDateTime.parse(
                    Instant.ofEpochSecond(
                        previousTwo?.published ?: 0
                    ).toString().dropLast(1))
                val timeNow = LocalDateTime.now()
                val twentyFourHours = timeNow.minus(Duration.ofHours(24))
                val fortEightHours = timeNow.minus(Duration.ofHours(48))

                when {
                    previousOne == null && publishedTwo.compareTo(twentyFourHours) == 1 ||
                            previousOne == null && publishedTwo.compareTo(twentyFourHours) == 0
                        -> DatePost(
                        Random.nextLong(),
                        application.getString(R.string.today)
                    )

                    publishedOne.compareTo(twentyFourHours) == 1 &&
                            publishedTwo.compareTo(twentyFourHours) == -1 ||
                            publishedOne.compareTo(fortEightHours) == 0 -> DatePost(
                        Random.nextLong(),
                        application.getString(R.string.yesterday)
                    )

                    publishedOne.compareTo(fortEightHours) == 1 &&
                            publishedTwo.compareTo(fortEightHours) == -1 -> DatePost(
                        Random.nextLong(),
                        application.getString(R.string.on_last_week)
                    )

                    else -> null
                }
            }
            .insertSeparators { previous, _ ->
                if (previous is Post) {
                    if (previous.id.rem(5) == 0L) {
                        Ad(Random.nextLong(), "https://netology.ru", "figma.jpg")
                    } else null
                } else null
            }
    }

    override suspend fun getAll() {
        try {
            val response = apiService.getAll()

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                var newBody = emptyList<Post>()
                var posts = emptyList<Post>()
                CoroutineScope(Dispatchers.Default).launch {
                    posts = dao.getAll().toDto()
                }

                posts.map {
                    newBody = body.map { postServer ->
                        if (postServer.id == it.id) postServer.copy(viewed = true) else postServer
                    }
                }

                dao.insertPosts(body.map { it.copy(savedOnTheServer = true) }.toEntity())
                return
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewer(id)
            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insertPosts(body.map { it.copy(savedOnTheServer = true) }.toEntity())
                emit(body.size)
                return@flow
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun likeById(id: Long, postLikedByMe: Boolean?) {
        try {
            if (postLikedByMe != null && !postLikedByMe) {
                val response = apiService.likeById(id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() in 400..599) {
                    throw ErrorCode403
                }

                throw ApiError(response.code(), response.message())
            } else {
                val response = apiService.dislikeById(id)

                if (response.isSuccessful) {
                    return
                }

                if (response.code() in 400..599) {
                    throw ErrorCode403
                }

                throw ApiError(response.code(), response.message())
            }
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post): Post {
        try {
            val response = apiService.save(post)

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                return body
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: AppError) {
            throw e
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = apiService.removeById(id)

            if (response.isSuccessful) {
                return
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload): Post {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(
                attachment = Attachment(
                    media.id,
                    AttachmentType.IMAGE,
                    post.attachment?.uri
                )
            )
            return save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
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

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signIn(login: String, password: String): AuthState {
        try {
            val response = apiService.updateUser(login, password)
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signUp(userName: String, login: String, password: String): AuthState {
        try {
            val response = apiService.registerUser(login, password, userName)
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signUpWithAPhoto(
        userName: String,
        login: String,
        password: String,
        media: MediaUpload
    ): AuthState {
        try {

            val media = MultipartBody.Part.createFormData(
                "file", media.file.name, media.file.asRequestBody()
            )
            val response = apiService.registerWithPhoto(
                login.toRequestBody("text/plain".toMediaType()),
                password.toRequestBody("text/plain".toMediaType()),
                userName.toRequestBody("text/plain".toMediaType()),
                media
            )
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
            }

            if (response.code() in 400..599) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (e: ErrorCode403) {
            throw ErrorCode403
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}