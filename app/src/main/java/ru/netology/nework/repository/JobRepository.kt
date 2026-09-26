package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType

interface JobRepository {
    //    fun getNewerCount(id: Long): Flow<Int>
    val data: Flow<List<Job>>
    suspend fun getAll(userId: Long)
}