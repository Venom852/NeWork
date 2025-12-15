package ru.netology.nework.dto

import java.time.Instant

data class Post (
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val authorJob: String?,
    val content: String,
    val published: Instant,
    val link: String?,
    val likedByMe: Boolean,
    val toShare: Boolean,
    val likes: Long,
    val numberViews: Long,
    val attachment: Attachment?,
    val shared: Long,
    val ownedByMe: Boolean,
    val mentionedMe: Boolean,
    val coords: Coordinates?,
    val mentionIds: Set<Long>,
    val likeOwnerIds: Set<Long>,
    val users: Map<Long, UserPreview>
)