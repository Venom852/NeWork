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
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.entity.toPostUserWallDto
import ru.netology.nework.entity.toPostUserWallEntity
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.repository.UserWallRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserWallViewModel @Inject constructor(
    private val repository: UserWallRepository,
    private val postUserWallDao: PostUserWallDao,
    private val authorIdDao: AuthorIdDao,
    auth: AppAuth,
) : ViewModel() {
    private val cachedPost: Flow<PagingData<Post>> = repository
        .data
        .cachedIn(viewModelScope)

    val dataUserWall: Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cachedPost.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }

    private val _errorWall403 = SingleLiveEvent<Unit>()
    val errorWall403: LiveData<Unit>
        get() = _errorWall403

    private val _errorWall404 = SingleLiveEvent<Unit>()
    val errorWall404: LiveData<Unit>
        get() = _errorWall404

    private var oldPosts = emptyList<Post>()

    fun likeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postUserWallDao.getAll().toPostUserWallDto()
            }

            val postLikedByMe = oldPosts.find { it.id == id }?.likedByMe
            postUserWallDao.likeById(id)
            try {
                repository.likeById(id, postLikedByMe)
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

    fun saveAuthorId(id: Long) {
        viewModelScope.launch {
            authorIdDao.saveId(id)
        }
    }
}
