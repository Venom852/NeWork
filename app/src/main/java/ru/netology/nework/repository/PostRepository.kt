package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType

interface PostRepository {
//    val data: Flow<PagingData<Post>>
    val data: Flow<List<Post>>
    suspend fun getAll()
    suspend fun save(post: Post): Post
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType): Post
    suspend fun upload(upload: MediaUpload): Media
}