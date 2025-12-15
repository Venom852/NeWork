package ru.netology.nework.dto

data class User(
    val id: Long,
    val name: String,
    val login: String,
    val avatar: String?
)