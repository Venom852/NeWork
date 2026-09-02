package ru.netology.nework.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User

interface PostUserWallRepository {
//    val data: Flow<PagingData<Post>>
    val data: Flow<List<Post>>
    suspend fun getAll()
    suspend fun likeById(id: Long, postLikedByMe: Boolean?)
}