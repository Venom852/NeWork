package ru.netology.nework.repository

import androidx.paging.ExperimentalPagingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.JobDao
import ru.netology.nework.dto.Job
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.UnknownError
import javax.inject.Inject
import javax.inject.Singleton
import ru.netology.nework.entity.toJobDto
import ru.netology.nework.entity.toJobEntity
import kotlin.time.ExperimentalTime

@Singleton
@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class JobRepositoryImpl @Inject constructor(
    private val jobDao: JobDao,
    private val apiService: ApiService,
    private val authorIdDao: AuthorIdDao,
) : JobRepository {
    override val data: Flow<List<Job>> = jobDao.getAllFlow().map { it.toJobDto() }

    var userId = 0L

    override suspend fun getAll() {
        try {
            val job = CoroutineScope(Dispatchers.IO).launch {
                userId = authorIdDao.getAuthorId()
            }
            job.join()

            val response = apiService.getAllJobs(userId)

            if (response.isSuccessful) {
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                jobDao.insertJobs(body.toJobEntity())

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
}