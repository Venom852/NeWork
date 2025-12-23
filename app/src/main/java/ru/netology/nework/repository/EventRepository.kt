package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.enumeration.AttachmentType

interface EventRepository {
    val data: Flow<PagingData<Event>>
    suspend fun save(event: Event): Event
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long, eventLikedByMe: Boolean?)
    suspend fun participateById(id: Long, eventParticipatedByMe: Boolean?)
    suspend fun saveWithAttachment(event: Event, upload: MediaUpload, attachmentType: AttachmentType): Event
    suspend fun upload(upload: MediaUpload): Media
}