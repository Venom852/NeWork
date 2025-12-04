package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val video: String?,
    val content: String?,
    val published: Long,
    val likedByMe: Boolean,
    val toShare: Boolean,
    val numberLikes: Long,
    @Embedded
    var attachment: AttachmentEmbeddable?,
    val shared: Long,
    val numberViews: Long,
    val savedOnTheServer: Boolean,
    val viewed: Boolean,
    val ownedByMe: Boolean
) {
    fun toDto() = Post(
        id,
        author,
        authorId,
        authorAvatar,
        video,
        content,
        published,
        likedByMe,
        toShare,
        numberLikes,
        attachment?.toDto(),
        shared,
        numberViews,
        savedOnTheServer,
        viewed,
        ownedByMe
    )

    companion object {
        fun fromDto(post: Post) = PostEntity(
            post.id,
            post.author,
            post.authorId,
            post.authorAvatar,
            post.video,
            post.content,
            post.published,
            post.likedByMe,
            post.toShare,
            post.likes,
            AttachmentEmbeddable.fromDto(post.attachment),
            post.shared,
            post.numberViews,
            post.savedOnTheServer,
            post.viewed,
            post.ownedByMe
        )
    }
}

data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
    var uri: String?
) {
    fun toDto() = Attachment(url, type, uri)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url,it.type, it.uri)
        }
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)