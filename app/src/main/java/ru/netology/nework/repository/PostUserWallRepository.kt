package ru.netology.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Post

interface PostUserWallRepository {
    fun getData(authorId: Long): Flow<PagingData<Post>>
    suspend fun likeById(id: Long, postLikedByMe: Boolean?, authorId: Long)
}