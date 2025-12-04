package ru.netology.nework.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ErrorCode400And500
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import kotlin.concurrent.thread
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.FeedItem
import ru.netology.nework.entity.toDto
import ru.netology.nework.enumeration.AttachmentType

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val dao: PostDao,
    auth: AppAuth,
    application: Application
) : AndroidViewModel(application) {
    val empty = Post(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = "netology",
        video = null,
        content = "",
        published = 0,
        likedByMe = false,
        toShare = false,
        likes = 0,
        attachment = null,
        shared = 0,
        numberViews = 0,
        savedOnTheServer = false,
        viewed = true,
        ownedByMe = false
    )

    private val noPhoto = PhotoModel()

    private val cached: Flow<PagingData<FeedItem>> = repository
        .data
        .cachedIn(viewModelScope)

    val data: Flow<PagingData<FeedItem>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cached.map { pagingData ->
                pagingData.map { post ->
                    if (post is Post) {
                        post.copy(ownedByMe = post.authorId == myId)
                    } else {
                        post
                    }
                }
            }
        }
    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    var newerCount: Flow<Int> = emptyFlow()
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated
    private val _bottomSheet = SingleLiveEvent<Unit>()
    val bottomSheet: LiveData<Unit>
        get() = _bottomSheet
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo
    private var oldPost = empty
    private var oldPosts = emptyList<Post>()

    init {
        loadPosts()
    }

    fun browse() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.Default).launch {
                oldPosts = dao.getAll().toDto()
            }
            newerCount = repository.getNewerCount(oldPosts.first().id)

            dao.browse()
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
//                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: ErrorCode400And500) {
                dao.insertPosts(oldPosts.toEntity())
                _bottomSheet.value = Unit
            } catch (e: UnknownError) {
                _dataState.value = FeedModelState(errorCode300 = true)
            } catch (e: Exception) {
                print(e)
                dao.insertPosts(oldPosts.toEntity())
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(refreshing = true)
//                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: ErrorCode400And500) {
                dao.insertPosts(oldPosts.toEntity())
                _bottomSheet.value = Unit
            } catch (e: UnknownError) {
                _dataState.value = FeedModelState(errorCode300 = true)
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toEntity())
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun loadPostsWithoutServer() {
        _dataState.value = _dataState.value?.copy(errorCode300 = false)
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.Default).launch {
                oldPosts = dao.getAll().toDto()
            }
            val postLikedByMe = oldPosts.find { it.id == id }?.likedByMe
            dao.likeById(id)
            try {
                repository.likeById(id, postLikedByMe)
            } catch (e: ErrorCode400And500) {
                dao.insertPosts(oldPosts.toEntity())
                _bottomSheet.value = Unit
            } catch (e: UnknownError) {
                _dataState.value = FeedModelState(errorCode300 = true)
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toEntity())
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun toShareById(id: Long) = thread { repository.toShareById(id) }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.Default).launch {
                oldPosts = dao.getAll().toDto()
            }
            dao.removeById(id)
            try {
                repository.removeById(id)
            } catch (e: ErrorCode400And500) {
                dao.insertPosts(oldPosts.toEntity())
                _bottomSheet.value = Unit
            } catch (e: UnknownError) {
                _dataState.value = FeedModelState(errorCode300 = true)
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toEntity())
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun saveContent(content: String) {
        edited.value?.let {
            viewModelScope.launch {
                CoroutineScope(Dispatchers.Default).launch {
                    oldPosts = dao.getAll().toDto()
                }

                var post = it.copy(content = content)
                var postServer = empty

                if (_photo.value?.uri != null) {
                    _photo.value?.uri?.let { uri ->
                        post = post.copy(
                            attachment = Attachment(
                                url = "null",
                                type = AttachmentType.IMAGE,
                                uri = uri.toString()
                            )
                        )
                    }
                    dao.save(PostEntity.fromDto(post))
                } else {
                    dao.save(PostEntity.fromDto(post))
                }
                _postCreated.value = Unit
                try {
                    when(_photo.value) {
                        noPhoto -> postServer = repository.save(post)
                        else -> _photo.value?.file?.let { file ->
                            postServer = repository.saveWithAttachment(post, MediaUpload(file))
                        }
                    }

                    if (post.id == 0L) {
                        oldPost = oldPosts.first()
                        dao.changeIdPostById(oldPost.id, postServer.id, savedOnTheServer = true)
                        _photo.value = noPhoto
                    }
                } catch (e: ErrorCode400And500) {
                    _bottomSheet.value = Unit
                    if (post.id == 0L && _photo.value == noPhoto) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
                } catch (e: UnknownError) {
                    _dataState.value = FeedModelState(errorCode300 = true)
                    if (post.id == 0L && _photo.value == noPhoto) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                    if (post.id == 0L && _photo.value == noPhoto) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
                }
            }
        }
        edited.value = empty
    }

    fun editById(post: Post) {
        edited.value = post
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }
}
