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
import ru.netology.nework.dto.Event
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
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.EventRemoteKeyDao
import ru.netology.nework.db.AppDb
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val apiService: ApiService,
    appDb: AppDb,
    eventRemoteKeyDao: EventRemoteKeyDao,
) : EventRepository {
    override val data: Flow<PagingData<Event>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = true),
        pagingSourceFactory = { eventDao.getPagingSource() },
        remoteMediator = EventRemoteMediator(apiService, appDb, eventDao, eventRemoteKeyDao)
    ).flow.map {
        it.map(EventEntity::toEventDto)
    }

    override suspend fun likeById(id: Long, eventLikedByMe: Boolean?) {
        try {
            if (eventLikedByMe != null && !eventLikedByMe) {
                val response = apiService.likeByIdEvent(id)

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
                val response = apiService.dislikeByIdEvent(id)

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

    override suspend fun participateById(id: Long, eventParticipatedByMe: Boolean?) {
        try {
            if (eventParticipatedByMe != null && !eventParticipatedByMe) {
                val response = apiService.participateByIdEvent(id)

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
                val response = apiService.cancelParticipateByIdEvent(id)

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

    override suspend fun save(event: Event): Event {
        try {
            val response = apiService.saveEvent(event)

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
            val response = apiService.removeByIdEvent(id)

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

    override suspend fun saveWithAttachment(event: Event, upload: MediaUpload, attachmentType: AttachmentType): Event {
        try {
            val media = upload(upload)
            val eventWithAttachment = event.copy(
                attachment = Attachment(
                    media.id,
                    attachmentType,
                )
            )
            return save(eventWithAttachment)
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