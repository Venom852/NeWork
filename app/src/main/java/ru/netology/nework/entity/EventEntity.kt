package ru.netology.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.EventType
import java.time.Instant
import kotlin.Boolean
import kotlin.Long

@Entity
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
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
    @Embedded
    val attachment: AttachmentEmbeddable?,
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
) {
    fun toEventDto() = Event(
        id,
        author,
        authorId,
        authorAvatar,
        authorJob,
        content,
        published,
        datetime,
        eventType,
        link,
        likedByMe,
        toShare,
        likes,
        numberViews,
        attachment?.toAttachmentEventDto(),
        shared,
        ownedByMe,
        coords,
        speakerIds,
        likeOwnerIds,
        participantsIds,
        participatedByMe,
        users,
        playSong,
        playVideo
    )

    companion object {
        fun fromEventDto(event: Event) = EventEntity(
            event.id,
            event.author,
            event.authorId,
            event.authorAvatar,
            event.authorJob,
            event.content,
            event.published,
            event.datetime,
            event.eventType,
            event.link,
            event.likedByMe,
            event.toShare,
            event.likes,
            event.numberViews,
            AttachmentEmbeddable.fromAttachmentEventDto(event.attachment),
            event.shared,
            event.ownedByMe,
            event.coords,
            event.speakerIds,
            event.likeOwnerIds,
            event.participantsIds,
            event.participatedByMe,
            event.users,
            event.playSong,
            event.playVideo
        )
    }
}

fun List<EventEntity>.toEventDto(): List<Event> = map(EventEntity::toEventDto)
fun List<Event>.toEventEntity(): List<EventEntity> = map(EventEntity::fromEventDto)