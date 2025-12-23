package ru.netology.nework.entity

import ru.netology.nework.dto.Attachment
import ru.netology.nework.enumeration.AttachmentType

data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
) {
    fun toAttachmentPostDto() = Attachment(url, type)
    fun toAttachmentEventDto() = Attachment(url, type)

    companion object {
        fun fromAttachmentPostDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url,it.type)
        }

        fun fromAttachmentEventDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url,it.type)
        }
    }
}