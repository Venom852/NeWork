package ru.netology.nework.model

import ru.netology.nework.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val empty: Boolean = false,
)

data class FeedModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorCode300: Boolean = false,
    val refreshing: Boolean = false
)