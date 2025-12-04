package ru.netology.nework.dto

sealed class FeedItem {
    abstract val id: Long
}

data class Post(
    override val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val video: String?,
    val content: String?,
    val published: Long,
    val likedByMe: Boolean,
    val toShare: Boolean,
    val likes: Long,
    val attachment: Attachment?,
    val shared: Long,
    val numberViews: Long,
    val savedOnTheServer: Boolean,
    val viewed: Boolean,
    val ownedByMe: Boolean
) : FeedItem()

data class Ad(
    override val id: Long,
    val url: String,
    val image: String,
) : FeedItem()

data class DatePost(
    override val id: Long,
    val date: String
) : FeedItem()