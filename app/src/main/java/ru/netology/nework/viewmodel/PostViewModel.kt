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
import kotlinx.coroutines.flow.emptyFlow
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.toPostDto
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val dao: PostDao,
    auth: AppAuth,
) : ViewModel() {
    var empty = Post(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
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
    private val mediaObserver = MediaLifecycleObserver()

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

    fun likeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toPostDto()
            }
            val postLikedByMe = oldPosts.find { it.id == id }?.likedByMe
            dao.likeById(id)
            try {
                repository.likeById(id, postLikedByMe)
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
        edited.value?.let {
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldPosts = dao.getAll().toPostDto()
                }

                var post = it.copy(
                    content = content,
                    mentionIds = listMentionedUser,
                    users = listMapUser
                )
                var postServer = empty

                if (coordinates.lat != 0.0 || coordinates.long != 0.0) {
                    post = post.copy(coords = coordinates)
                }

                dao.save(PostEntity.fromPostDto(post))
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

//                    if (post.id == 0L) {
                        oldPost = oldPosts.first()
                        dao.changeIdPostById(
                            oldPost.id,
                            postServer.id,
                            postServer.author,
                            postServer.authorId,
                            postServer.authorAvatar,
                            postServer.authorJob,
                            postServer.attachment?.url,
                            postServer.attachment?.type
                        )

                        _media.value = noMedia
                        listMentionedUser = emptySet()
                        coordinates = Coordinates(0.0, 0.0)
                        listMapUser = emptyMap()
//                    }
                } catch (_: ErrorCode403) {
                    _errorPost403.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (_: ErrorCode415) {
                    _errorPost415.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toPostEntity())
                }
            }
        }
        edited.value = empty
    }

    fun editById(post: Post) {
        viewModelScope.launch {
            edited.value = post
        }
    }

    fun changeMedia(uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        viewModelScope.launch {
            _media.value = MediaModel(null, null, attachmentType)
            _media.value = MediaModel(uri, file, attachmentType)
        }
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

    fun playSong(post: Post) {
        mediaObserver.stop()
        mediaObserver.apply {
            mediaPlayer?.setDataSource(
                post.attachment?.url
            )
        }.play()
    }

    fun pauseSong() {
        viewModelScope.launch {
            mediaObserver.pause()
        }
    }

    fun playVideo(post: Post) {
        mediaObserver.stop()
        mediaObserver.apply {
            mediaPlayer?.setDataSource(
                post.attachment?.url
            )
        }.play()
    }

    fun pauseVideo() {
        viewModelScope.launch {
            mediaObserver.pause()
        }
    }

    fun mentionUsers(listMentioned: Set<Long>, listMap: Map<Long, UserPreview>) {
        viewModelScope.launch {
            listMapUser = listMap
            listMentionedUser = listMentioned
        }
    }

    fun addLocation(coords: Coordinates) {
        viewModelScope.launch {
            coordinates = coords
        }
    }
}
