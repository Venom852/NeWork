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
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import androidx.paging.map
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.entity.AttachmentEmbeddable
import ru.netology.nework.entity.toDto
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val dao: PostDao,
    auth: AppAuth,
    application: Application
) : AndroidViewModel(application) {
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
        users = emptyMap()
    )

    private val noMedia = MediaModel()
//    private val noPhoto = MediaModel()
//    private val noAudio = AudioModel()
//    private val noVideo = VideoModel()

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

    //    private val _dataState = MutableLiveData(FeedModelState())
//    val dataState: LiveData<FeedModelState>
//        get() = _dataState
    var newerCount: Flow<Int> = emptyFlow()
    val edited = MutableLiveData(empty)

    //TODO(Попробовать заменить на Flow)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    //TODO(Попробовать заменить на Flow)
    private val _errorPost403 = SingleLiveEvent<Unit>()
    val errorPost403: LiveData<Unit>
        get() = _errorPost403

    //TODO(Попробовать заменить на Flow)
    private val _errorPost404 = SingleLiveEvent<Unit>()
    val errorPost404: LiveData<Unit>
        get() = _errorPost404

    //TODO(Попробовать заменить на Flow)
    private val _errorPost415 = SingleLiveEvent<Unit>()
    val errorPost415: LiveData<Unit>
        get() = _errorPost415

    //TODO(Попробовать заменить на Flow)
    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

    //    //TODO(Попробовать заменить на Flow)
//    private val _audio = MutableLiveData(noAudio)
//    val audio: LiveData<AudioModel>
//        get() = _audio
//    //TODO(Попробовать заменить на Flow)
//    private val _video = MutableLiveData(noVideo)
//    val video: LiveData<VideoModel>
//        get() = _video
    private var oldPost = empty
    private var oldPosts = emptyList<Post>()
    private var listUsers = emptySet<Long>()
    private var coordinates = Coordinates(lat = 0.0, long = 0.0)

//    init {
//        loadPosts()
//    }

//    fun browse() {
//        viewModelScope.launch {
//            CoroutineScope(Dispatchers.IO).launch {
//                oldPosts = dao.getAll().toDto()
//            }
//            newerCount = repository.getNewerCount(oldPosts.first().id)
//
//            dao.browse()
//        }
//    }

//    fun loadPosts() {
//        viewModelScope.launch {
//            try {
//                _dataState.value = FeedModelState(loading = true)
////                repository.getAll()
//                _dataState.value = FeedModelState()
//            } catch (e: ErrorCode403) {
//                dao.insertPosts(oldPosts.toEntity())
//                _bottomSheet.value = Unit
//            } catch (e: Exception) {
//                dao.insertPosts(oldPosts.toEntity())
//                _dataState.value = FeedModelState(error = true)
//            }
//        }
//    }

//    fun refreshPosts() {
//        viewModelScope.launch {
//            try {
//                _dataState.value = FeedModelState(refreshing = true)
////                repository.getAll()
//                _dataState.value = FeedModelState()
//            } catch (e: ErrorCode400And500) {
//                dao.insertPosts(oldPosts.toEntity())
//                _bottomSheet.value = Unit
//            } catch (e: UnknownError) {
//                _dataState.value = FeedModelState(errorCode300 = true)
//            } catch (e: Exception) {
//                dao.insertPosts(oldPosts.toEntity())
//                _dataState.value = FeedModelState(error = true)
//            }
//        }
//    }

//    fun loadPostsWithoutServer() {
//        _dataState.value = _dataState.value?.copy(errorCode300 = false)
//    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toDto()
            }
            val postLikedByMe = oldPosts.find { it.id == id }?.likedByMe
            dao.likeById(id)
            try {
                repository.likeById(id, postLikedByMe)
            } catch (_: ErrorCode403) {
                dao.insertPosts(oldPosts.toEntity())
                _errorPost403.value = Unit
            } catch (_: ErrorCode404) {
                dao.insertPosts(oldPosts.toEntity())
                _errorPost404.value = Unit
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toEntity())
                e.printStackTrace()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = dao.getAll().toDto()
            }
            dao.removeById(id)
            try {
                repository.removeById(id)
            } catch (_: ErrorCode403) {
                dao.insertPosts(oldPosts.toEntity())
                _errorPost403.value = Unit
            } catch (e: Exception) {
                dao.insertPosts(oldPosts.toEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveContent(content: String) {
        edited.value?.let {
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldPosts = dao.getAll().toDto()
                }

                var post = it.copy(
                    content = content,
                    mentionIds = listUsers
                )
                var postServer = empty

                if (coordinates.lat != 0.0 || coordinates.long != 0.0) {
                    post = post.copy(coords = coordinates)
                }

                dao.save(PostEntity.fromDto(post))
                _postCreated.value = Unit

                try {
                    when (_media.value) {
                        noMedia -> postServer = repository.save(post)
                        else -> _media.value?.file?.let { file ->
                            postServer = repository.saveWithAttachment(post, MediaUpload(file))
                        }
                    }

                    if (post.id == 0L) {
                        oldPost = oldPosts.first()
                        dao.changeIdPostById(oldPost.id, postServer.id, post.attachment?.url, post.attachment?.type)
                        _media.value = noMedia
                        listUsers = emptySet()
                        coordinates = Coordinates(0.0, 0.0)
                    }
                } catch (_: ErrorCode403) {
                    _errorPost403.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
                } catch (_: ErrorCode415) {
                    _errorPost415.value = Unit
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (post.id == 0L && _media.value == noMedia) {
                        dao.removeById(oldPost.id)
                        return@launch
                    } else dao.insertPosts(oldPosts.toEntity())
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

    fun mentionUsers(list: List<Long>) {
        viewModelScope.launch {
            listUsers = list.toSet()
        }
    }

    fun addLocation(coord: Coordinates) {
        viewModelScope.launch {
            coordinates = coord
        }
    }

//    fun changePhoto(uri: Uri?, file: File?) {
//        _audio.value = noAudio
//        _video.value = noVideo
//        _photo.value = MediaModel(uri, file)
//    }

//    fun changeAudio(uri: Uri?, file: File?) {
//        _photo.value = noPhoto
//        _video.value = noVideo
//        _audio.value = AudioModel(uri, file)
//    }
//
//    fun changeVideo(uri: Uri?, file: File?) {
//        _photo.value = noPhoto
//        _audio.value = noAudio
//        _video.value = VideoModel(uri, file)
//    }
}
