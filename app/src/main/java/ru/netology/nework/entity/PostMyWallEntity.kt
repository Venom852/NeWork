package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserPreview
import java.time.Instant
import kotlin.Boolean

@Entity
data class PostMyWallEntity(
    @PrimaryKey(autoGenerate = true)
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
    @Embedded
    val attachment: AttachmentEmbeddable?,
    val shared: Long,
    val ownedByMe: Boolean,
    val mentionedMe: Boolean,
    val coords: Coordinates?,
    val mentionIds: Set<Long>,
    val likeOwnerIds: Set<Long>,
    val users: Map<Long, UserPreview>,
    val playSong: Boolean,
    val playVideo: Boolean
) {
    fun toPostMyWallDto() = Post(
        id,
        author,
        authorId,
        authorAvatar,
        authorJob,
        content,
        published,
        link,
        likedByMe,
        toShare,
        likes,
        numberViews,
        attachment?.toAttachmentPostDto(),
        shared,
        ownedByMe,
        mentionedMe,
        coords,
        mentionIds,
        likeOwnerIds,
        users,
        playSong,
        playVideo
    )

    companion object {
        fun fromPostMyWallDto(post: Post) = PostMyWallEntity(
            post.id,
            post.author,
            post.authorId,
            post.authorAvatar,
            post.authorJob,
            post.content,
            post.published,
            post.link,
            post.likedByMe,
            post.toShare,
            post.likes,
            post.numberViews,
            AttachmentEmbeddable.fromAttachmentPostDto(post.attachment),
            post.shared,
            post.ownedByMe,
            post.mentionedMe,
            post.coords,
            post.mentionIds,
            post.likeOwnerIds,
            post.users,
            post.playSong,
            post.playVideo
        )
    }
}

fun List<PostMyWallEntity>.toPostMyWallDto(): List<Post> = map(PostMyWallEntity::toPostMyWallDto)
fun List<Post>.toPostMyWallEntity(): List<PostMyWallEntity> = map(PostMyWallEntity::fromPostMyWallDto)