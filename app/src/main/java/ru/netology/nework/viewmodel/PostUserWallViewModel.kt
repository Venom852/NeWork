package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.dto.User
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.toPostUserWallDto
import ru.netology.nework.entity.toPostUserWallEntity
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.PostUserWallRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostUserWallViewModel @Inject constructor(
    private val repository: PostUserWallRepository,
    private val postUserWallDao: PostUserWallDao,
    private val userDao: UserDao,
    private val auth: AppAuth,
) : ViewModel() {
    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    private val _errorWall403 = SingleLiveEvent<Unit>()
    val errorWall403: LiveData<Unit>
        get() = _errorWall403

    private val _errorWall404 = SingleLiveEvent<Unit>()
    val errorWall404: LiveData<Unit>
        get() = _errorWall404

    private var oldPosts = emptyList<Post>()

    fun cachedPost(authorId: Long): Flow<PagingData<Post>> = repository
        .getData(authorId)
        .cachedIn(viewModelScope)

    fun dataPostUserWall(authorId: Long): Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { cachedPost(authorId) }

    fun dataUserWall(authorId: Long): Flow<User> = auth.authStateFlow
        .flatMapLatest { userDao.getUserFlow(authorId).map { it.toUserDto() } }

    fun likeById(post: Post) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postUserWallDao.getAll().toPostUserWallDto()
            }

            val postLikedByMe = oldPosts.find { it.id == post.id }?.likedByMe
            postUserWallDao.likeById(post.id, saveLikeOwnerIds(post), saveUsers(post))

            try {
                repository.likeById(post.id, postLikedByMe, post.authorId)
            } catch (_: ErrorCode403) {
                postUserWallDao.insertPosts(oldPosts.toPostUserWallEntity())
                _errorWall403.value = Unit
            } catch (_: ErrorCode404) {
                postUserWallDao.insertPosts(oldPosts.toPostUserWallEntity())
                _errorWall404.value = Unit
            } catch (e: Exception) {
                postUserWallDao.insertPosts(oldPosts.toPostUserWallEntity())
                e.printStackTrace()
            }
        }
    }
    fun removeUserWall() {
        viewModelScope.launch {
            postUserWallDao.removeDao()
        }
    }

    private fun saveLikeOwnerIds(post: Post): Set<Long> {
        val listLikeOwnerIds = post.likeOwnerIds.toMutableSet()
        listLikeOwnerIds.add(post.authorId)
        return listLikeOwnerIds.toSet()
    }

    private fun saveUsers(post: Post): Map<Long, UserPreview> {
        val users = post.users.toMutableMap()
        users[post.authorId] = UserPreview(post.author, post.authorAvatar)
        return users.toMap()
    }
}
