package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.dto.User
import ru.netology.nework.entity.toPostMyWallEntity
import ru.netology.nework.entity.toPostUserWallDto
import ru.netology.nework.entity.toPostUserWallEntity
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.PostUserWallRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostUserWallViewModel @Inject constructor(
    private val repository: PostUserWallRepository,
    private val postUserWallDao: PostUserWallDao,
    private val authorIdDao: AuthorIdDao,
    private val userDao: UserDao,
    val auth: AppAuth,
) : ViewModel() {
    private var user = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
    )
    private val mediaObserver = MediaLifecycleObserver()
    private var authorId = 0L
    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState
//    private val cachedPost: Flow<PagingData<Post>> = repository
//        .getData(authorId)
//        .cachedIn(viewModelScope)
//
//    val dataPostUserWall: Flow<PagingData<Post>> = auth.authStateFlow
//        .flatMapLatest { (myId, _) ->
//            cachedPost
////                .map { pagingData ->
////                pagingData.map { post ->
////                    post.copy(ownedByMe = post.authorId == myId)
////                }
////            }
//        }

//    val dataPostUserWall: Flow<List<Post>> = auth.authStateFlow
//        .flatMapLatest { repository.data }

//    val dataUserWall: Flow<User> = auth.authStateFlow
//        .flatMapLatest { userDao.getUserFlow(authorIdDao.getAuthorId().id).map { it.toUserDto() } }

//    val dataUserWall: LiveData<User> = auth.authStateFlow
//        .flatMapLatest { userDao.getUserFlow(authorId).map { it.toUserDto() } }
//        .asLiveData(Dispatchers.IO)

    private val _errorWall403 = SingleLiveEvent<Unit>()
    val errorWall403: LiveData<Unit>
        get() = _errorWall403

    private val _errorWall404 = SingleLiveEvent<Unit>()
    val errorWall404: LiveData<Unit>
        get() = _errorWall404

    private var oldPosts = emptyList<Post>()

//    init {
//        loadPosts()
//    }

    fun cachedPost(authorId: Long): Flow<PagingData<Post>> = repository
        .getData(authorId)
        .cachedIn(viewModelScope)

    fun dataPostUserWall(authorId: Long): Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cachedPost(authorId)
//                .map { pagingData ->
//                pagingData.map { post ->
//                    post.copy(ownedByMe = post.authorId == myId)
//                }
//            }
        }

    fun dataUserWall(authorId: Long): LiveData<User> = auth.authStateFlow
        .flatMapLatest { userDao.getUserFlow(authorId).map { it.toUserDto() } }
        .asLiveData(Dispatchers.IO)

//    fun initializeAuthorId(id: Long) {
//        authorId = id
//        repository.initializeAuthorIdRep(id)
//    }

    fun loadPosts(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postUserWallDao.getAll().toPostUserWallDto()
            }

            authorId = id

            try {
                _dataState.value = FeedModelState(loading = true)
                repository.getAll(id)
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                postUserWallDao.insertPosts(oldPosts.toPostUserWallEntity())
                e.printStackTrace()
            }
        }
    }

//    fun refreshPosts() {
//        viewModelScope.launch {
//            CoroutineScope(Dispatchers.IO).launch {
//                oldPosts = postUserWallDao.getAll().toPostUserWallDto()
//            }
//
//            try {
//                _dataState.value = FeedModelState(loading = true)
//                repository.getAll()
//                _dataState.value = FeedModelState()
//            } catch (e: Exception) {
//                postUserWallDao.insertPosts(oldPosts.toPostUserWallEntity())
//                e.printStackTrace()
//            }
//        }
//    }

    fun likeById(post: Post) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postUserWallDao.getAll().toPostUserWallDto()
            }

            val postLikedByMe = oldPosts.find { it.id == post.id }?.likedByMe
            postUserWallDao.likeById(post.id, saveLikeOwnerIds(post))
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

    fun saveAuthorId(id: Long) {
        viewModelScope.launch {
            authorIdDao.saveId(id)
        }
    }

    fun removeAuthorId() {
        viewModelScope.launch {
            authorIdDao.removeId()
        }
    }

    private fun saveLikeOwnerIds(post: Post): Set<Long> {
        val listLikeOwnerIds = post.likeOwnerIds.toMutableSet()
        listLikeOwnerIds.add(post.id)
        return listLikeOwnerIds.toSet()
    }

//    fun getUser(id: Long): User {
//        viewModelScope.launch {
//            val job = CoroutineScope(Dispatchers.IO).launch {
//                user = userDao.getUser(id).toUserDto()
//            }
////            val deferred = async {
////                userDao.getUser(id).toUserDto()
////            }
////
////            user = deferred.await()
//            job.join()
//        }
//
//        print(user)
//        return user
//    }
}
