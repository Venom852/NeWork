package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.AttachmentType
import java.time.Instant

@Entity
data class PostEntity(
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
    val users: Map<Long, UserPreview>
) {
    fun toDto() = Post(
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
        attachment?.toDto(),
        shared,
        ownedByMe,
        mentionedMe,
        coords,
        mentionIds,
        likeOwnerIds,
        users,
    )

    companion object {
        fun fromDto(post: Post) = PostEntity(
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
            AttachmentEmbeddable.fromDto(post.attachment),
            post.shared,
            post.ownedByMe,
            post.mentionedMe,
            post.coords,
            post.mentionIds,
            post.likeOwnerIds,
            post.users,
        )
    }
}

data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
) {
    fun toDto() = Attachment(url, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url,it.type)
        }
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)