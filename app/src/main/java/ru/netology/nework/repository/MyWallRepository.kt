package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType

interface MyWallRepository {
    val data: Flow<PagingData<Post>>
    suspend fun save(post: Post): Post
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType): Post
    suspend fun upload(upload: MediaUpload): Media
}