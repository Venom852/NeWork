package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Job

interface JobMyRepository {
    suspend fun getAll()
    val data: Flow<List<Job>>
    suspend fun save(job: Job): Job
    suspend fun removeById(id: Long)
}