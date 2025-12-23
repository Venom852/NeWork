package ru.netology.nework.dto

import ru.netology.nework.enumeration.EventType
import java.time.Instant

data class Event (
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val authorJob: String?,
    val content: String,
    val published: Instant,
    val datetime: Instant,
    val eventType: EventType?,
    val link: String?,
    val likedByMe: Boolean,
    val toShare: Boolean,
    val likes: Long,
    val numberViews: Long,
    val attachment: Attachment?,
    val shared: Long,
    val ownedByMe: Boolean,
    val coords: Coordinates?,
    val speakerIds: Set<Long>,
    val likeOwnerIds: Set<Long>,
    val participantsIds: Set<Long>,
    val participatedByMe: Boolean,
    val users: Map<Long, UserPreview>,
    val playSong: Boolean,
    val playVideo: Boolean
)