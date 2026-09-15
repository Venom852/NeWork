package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toPostEntity
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.forEach
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.toPostDto
import ru.netology.nework.entity.toUserDto
import ru.netology.nework.entity.toUserEntity
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import ru.netology.nework.model.FeedModelState
import java.time.Instant
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val dao: PostDao,
    auth: AppAuth,
) : ViewModel() {
    var empty = Post(
        id = 0,
        author = "Me123",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        //TODO(Сделать везде дату)
        published = "",
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        mentionIds = emptySet(),
        coords = null,
        mentionedMe = false,
        likeOwnerIds = emptySet(),
        users = emptyMap(),
        playSong = false,
        playVideo = false
    )

    private val noMedia = MediaModel()

    //    private val mediaObserver = MediaLifecycleObserver()
    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    private val cachedPost: Flow<PagingData<Post>> = repository
        .data
        .cachedIn(viewModelScope)

    val dataPost: Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cachedPost.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }

//    val dataPost: Flow<List<Post>> = auth.authStateFlow
//        .flatMapLatest { (myId, _) ->
//            repository.data.map { listPost ->
//                listPost.map { post ->
//                    post.copy(ownedByMe = post.authorId == myId)
//                }
//            }
//        }

    val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _errorPost403 = SingleLiveEvent<Unit>()
    val errorPost403: LiveData<Unit>
        get() = _errorPost403

    private val _errorPost404 = SingleLiveEvent<Unit>()
    val errorPost404: LiveData<Unit>
        get() = _errorPost404

    private val _errorPost415 = SingleLiveEvent<Unit>()
    val errorPost415: LiveData<Unit>
        get() = _errorPost415

    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

    private var oldPost = empty
    private var oldPosts = emptyList<Post>()
    var listMentionedUser = emptySet<Long>()
    var listMapUser = emptyMap<Long, UserPreview>()
    var coordinates = Coordinates(lat = 0.0, long = 0.0)

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toPostDto()
            }

            try {
                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toPostEntity())
                e.printStackTrace()
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toPostDto()
            }

            try {
                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toPostEntity())
                e.printStackTrace()
            }
        }
    }

    fun likeById(post: Post) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toPostDto()
            }

            val postLikedByMe = oldPosts.find { it.id == post.id }?.likedByMe
            dao.likeById(post.id, saveLikeOwnerIds(post))

            try {
                repository.likeById(post.id, postLikedByMe)
            } catch (_: ErrorCode403) {
                dao.insertPosts(oldPosts.toPostEntity())
                _errorPost403.value = Unit
            } catch (_: ErrorCode404) {
                dao.insertPosts(oldPosts.toPostEntity())
                _errorPost404.value = Unit
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toPostEntity())
                e.printStackTrace()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toPostDto()
            }

            dao.removeById(id)

            try {
                repository.removeById(id)
            } catch (_: ErrorCode403) {
                dao.insertPosts(oldPosts.toPostEntity())
                _errorPost403.value = Unit
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toPostEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveContent(content: String) {
        edited.value?.let { newPost ->
            //TODO(Нужно ли заключить весь код в корутину)
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldPosts = dao.getAll().toPostDto()
                }

                val data = ZonedDateTime.now().toString()
                var postServer = empty
                var post = newPost.copy(
//                    published = "${data.dayOfMonth}.${data.monthValue}.${data.year} ${data.hour}:${data.minute}",
//                    published = data,
                    content = content,
                    ownedByMe = true
                )

                if (!listMentionedUser.isEmpty() && !listMapUser.isEmpty()) {
                    post = newPost.copy(
                        mentionIds = listMentionedUser,
                        users = listMapUser
                    )
                }

                if (coordinates.lat != 0.0 || coordinates.long != 0.0) {
                    post = post.copy(coords = coordinates)
                }

                dao.save(PostEntity.fromPostDto(post))
                //TODO(Нужно ли здесь использовать)
                _postCreated.value = Unit

                try {
                    when (_media.value) {
                        noMedia -> postServer = repository.save(post)
                        else -> _media.value?.file?.let { file ->
                            _media.value?.attachmentType?.let { attachmentType ->
                                postServer = repository.saveWithAttachment(
                                    post,
                                    MediaUpload(file),
                                    attachmentType
                                )
                            }
                        }
                    }

                    if (post.id == 0L) {
//                    oldPost = oldPosts.first()
                        dao.changeIdPostById(
                            0L,
                            postServer.id,
//                            postServer.author,
                            postServer.authorId,
                            postServer.authorAvatar,
                            postServer.authorJob,
                            postServer.mentionedMe,
                            postServer.attachment?.url,
                            postServer.attachment?.type
                        )

                        _media.value = noMedia
                        listMentionedUser = emptySet()
                        coordinates = Coordinates(0.0, 0.0)
                        listMapUser = emptyMap()
                    }
                } catch (_: ErrorCode403) {
//                    dao.insertPosts(oldPosts.toPostEntity())
                    _errorPost403.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(0L)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (_: ErrorCode415) {
//                    dao.insertPosts(oldPosts.toPostEntity())
                    _errorPost415.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (e: Exception) {
//                    dao.insertPosts(oldPosts.toPostEntity())
                    e.printStackTrace()
                    if (post.id == 0L) {
                        dao.removeById(0L)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                }
            }
        }
        edited.value = empty
    }

    fun editById(post: Post) {
        edited.value = post
    }

    fun changeMedia(uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        //TODO(Проверить нужно ли здесь)
        _media.value = MediaModel(null, null, attachmentType)
        _media.value = MediaModel(uri, file, attachmentType)
    }

    fun playButtonSong(id: Long) {
        viewModelScope.launch {
            dao.playButtonSong(id)
        }
    }

    fun playButtonVideo(id: Long) {
        viewModelScope.launch {
            dao.playButtonVideo(id)
        }
    }

    fun playSong(post: Post, mediaObserver: MediaLifecycleObserver) {
//        mediaObserver.stop()
        mediaObserver.apply {
            mediaPlayer?.setDataSource(
                post.attachment?.url
            )
        }.play()
    }

    fun pauseSong(mediaObserver: MediaLifecycleObserver) {
        mediaObserver.pause()
    }

    fun playVideo(post: Post) {
//        mediaObserver.stop()
//        mediaObserver.apply {
//            mediaPlayer?.setDataSource(
//                post.attachment?.url
//            )
//        }.play()
    }

    fun pauseVideo() {
//        mediaObserver.pause()
    }

    fun mentionUsers(listIdUsers: Set<Long>, listMapUsers: Map<Long, UserPreview>) {
        listMapUser = listMapUsers
        listMentionedUser = listIdUsers
    }

    fun addLocation(coords: Coordinates) {
        coordinates = coords
    }

    private fun saveLikeOwnerIds(post: Post): Set<Long> {
        val listLikeOwnerIds = post.likeOwnerIds.toMutableSet()
        listLikeOwnerIds.add(post.id)
        return listLikeOwnerIds.toSet()
    }
}
