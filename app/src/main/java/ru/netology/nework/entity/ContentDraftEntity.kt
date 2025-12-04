package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ContentDraftEntity (
    @PrimaryKey
    val contentDraft: String
)