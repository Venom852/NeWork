package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.JobMyDao
import ru.netology.nework.dto.Job
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import ru.netology.nework.entity.toJobMyDto
import ru.netology.nework.entity.toJobMyEntity
import ru.netology.nework.error.AppError
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class JobMyRepositoryImpl @Inject constructor(
    private val jobMyDao: JobMyDao,
    private val apiService: ApiService,
) : JobMyRepository {
    override val data: Flow<List<Job>> = jobMyDao.getAllFlow().map { it.toJobMyDto() }

    override suspend fun getAll() {
        try {
            val response = apiService.getAllMyJobs()

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                jobMyDao.insertJobs(body.toJobMyEntity())

                return
            }

            if (response.code() == 403) {
                throw ErrorCode403
            }

            throw ApiError(response.code(), response.message())
        } catch (_: ErrorCode403) {
            throw ErrorCode403
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(job: Job): Job {
        try {
            val response = apiService.saveMyJob(job)

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
            val response = apiService.removeByIdMyJob(id)

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
}