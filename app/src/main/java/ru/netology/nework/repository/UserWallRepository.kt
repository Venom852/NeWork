package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType

interface UserWallRepository {
    val data: Flow<PagingData<Post>>
    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
}