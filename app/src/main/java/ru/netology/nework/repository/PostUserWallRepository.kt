package ru.netology.nework.repository

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User

interface PostUserWallRepository {
//    val data: Flow<PagingData<Post>>
//    val data: Flow<List<Post>>
    fun getData(authorId: Long): Flow<PagingData<Post>>
//    fun initializeAuthorIdRep(authorId: Long)
    suspend fun getAll(authorId: Long)
    suspend fun likeById(id: Long, postLikedByMe: Boolean?, authorId: Long)
}