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
    suspend fun getAll()
    val data: Flow<List<Job>>
//    suspend fun save(job: Job): Job
//    suspend fun removeById(id: Long)
//    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
//    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType): Post
//    suspend fun upload(upload: MediaUpload): Media
//    suspend fun signIn(login: String, password: String): AuthState
//    suspend fun signUp(userName: String, login: String, password: String): AuthState
//    suspend fun signUpWithAPhoto(userName: String, login: String, password: String, media: MediaUpload): AuthState
}