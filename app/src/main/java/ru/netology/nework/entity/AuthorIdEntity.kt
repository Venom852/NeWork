package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AuthorIdEntity (
    @PrimaryKey
    val id: Long
)