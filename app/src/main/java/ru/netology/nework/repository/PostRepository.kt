package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.FeedItem
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post

interface PostRepository {

    val data: Flow<PagingData<FeedItem>>
    suspend fun getAll()
    suspend fun save(post: Post): Post
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
    fun toShareById(id: Long)
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload): Post
    suspend fun upload(upload: MediaUpload): Media
    suspend fun signIn(login: String, password: String): AuthState
    suspend fun signUp(userName: String, login: String, password: String): AuthState
    suspend fun signUpWithAPhoto(userName: String, login: String, password: String, media: MediaUpload): AuthState
}