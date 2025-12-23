package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.User
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import ru.netology.nework.dao.UserDao
import ru.netology.nework.entity.toUserDto
import ru.netology.nework.entity.toUserEntity
import ru.netology.nework.error.ErrorCode400
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
) : UserRepository {
    override val data: Flow<List<User>> = userDao.getAllFlow().map { it.toUserDto() }

    override suspend fun getAll() {
        try {
            val response = apiService.getAllUsers()

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                userDao.insertUsers(body.toUserEntity())

                return
            }

            throw ApiError(response.code(), response.message())
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signIn(login: String, password: String): AuthState {
        try {
            val response = apiService.updateUser(login, password)
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
            }

            if (response.code() == 400) {
                throw ErrorCode400
            }

            if (response.code() == 404) {
                throw ErrorCode404
            }

            throw ApiError(response.code(), response.message())
        } catch (_: ErrorCode400) {
            throw ErrorCode400
        } catch (_: ErrorCode404) {
            throw ErrorCode404
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun signUp(userName: String, login: String, password: String): AuthState {
        try {
            val response = apiService.registerUser(login, password, userName)
            if (response.isSuccessful) {
                return response.body() ?: throw ApiError(response.code(), response.message())
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