package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.model.MediaModel
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import ru.netology.nework.dao.PostMyWallDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.User
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.entity.toPostMyWallDto
import ru.netology.nework.entity.toPostMyWallEntity
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.PostMyWallRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostMyWallViewModel @Inject constructor(
    private val repository: PostMyWallRepository,
    private val postMyWallDao: PostMyWallDao,
    private val userDao: UserDao,
    private val auth: AppAuth,
) : ViewModel() {
    var empty = Post(
        id = 0,
        author = "Me123",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
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
    private val mediaObserver = MediaLifecycleObserver()
    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    private val cachedPost: Flow<PagingData<Post>> = repository
        .data
        .cachedIn(viewModelScope)

    val dataPostMyWall: Flow<PagingData<Post>> = auth.authStateFlow
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

    private val _errorMyWall403 = SingleLiveEvent<Unit>()
    val errorMyWall403: LiveData<Unit>
        get() = _errorMyWall403

    private val _errorMyWall404 = SingleLiveEvent<Unit>()
    val errorMyWall404: LiveData<Unit>
        get() = _errorMyWall404

    private val _errorMyWall415 = SingleLiveEvent<Unit>()
    val errorMyWall415: LiveData<Unit>
        get() = _errorMyWall415

    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

//    private var oldPost = empty
    private var oldPosts = emptyList<Post>()
    var statusMedia = false
    var listMentionedUser = emptySet<Long>()
    var listMapUser = emptyMap<Long, UserPreview>()
    var coordinates = Coordinates(lat = 0.0, long = 0.0)

    fun dataMyUserWall(authorId: Long): Flow<User> = auth.authStateFlow
        .flatMapLatest { userDao.getUserFlow(authorId).map { it.toUserDto() } }

    fun likeById(post: Post) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postMyWallDao.getAll().toPostMyWallDto()
            }

            val postLikedByMe = oldPosts.find { it.id == post.id }?.likedByMe
            postMyWallDao.likeById(post.id, saveLikeOwnerIds(post), saveUsers(post))

            try {
                repository.likeById(post.id, postLikedByMe)
            } catch (_: ErrorCode403) {
                postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                _errorMyWall403.value = Unit
            } catch (_: ErrorCode404) {
                postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                _errorMyWall404.value = Unit
            } catch (e: Exception) {
                postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                e.printStackTrace()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldPosts = postMyWallDao.getAll().toPostMyWallDto()
            }

            postMyWallDao.removeById(id)

            try {
                repository.removeById(id)
            } catch (_: ErrorCode403) {
                postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                _errorMyWall403.value = Unit
            } catch (e: Exception) {
                postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveContent(content: String) {
        edited.value?.let {
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldPosts = postMyWallDao.getAll().toPostMyWallDto()
                }

                val data = Instant.now().toString()
                var postServer = empty
                var post = it.copy(
                    published = data,
                    content = content,
                    ownedByMe = true
                )

                if (!listMentionedUser.isEmpty() && !listMapUser.isEmpty()) {
                    post = it.copy(
                        mentionIds = listMentionedUser,
                        users = listMapUser
                    )
                }

                if (coordinates.lat != 0.0 || coordinates.long != 0.0) {
                    post = post.copy(coords = coordinates)
                }

                postMyWallDao.save(PostMyWallEntity.fromPostMyWallDto(post))
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

//                    if (!statusMedia) {
//                        postServer = repository.save(post)
//                    } else {
//                        _media.value?.file?.let { file ->
//                            _media.value?.attachmentType?.let { attachmentType ->
//                                postServer = repository.saveWithAttachment(
//                                    post,
//                                    MediaUpload(file),
//                                    attachmentType
//                                )
//                            }
//                        }
//                    }

                    if (post.id == 0L) {
//                    oldPost = oldPosts.first()
                        postMyWallDao.changeIdPostById(
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
                        statusMedia = false
                    }
                } catch (_: ErrorCode403) {
                    postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                    _errorMyWall403.value = Unit
//                    if (post.id == 0L && _media.value == noMedia) {
//                        dao.removeById(0L)
//                        return@launch
//                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (_: ErrorCode415) {
                    postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                    _errorMyWall415.value = Unit
//                    if (post.id == 0L && _media.value == noMedia) {
//                        dao.removeById(oldPost.id)
//                        return@launch
//                    } else dao.insertPosts(oldPosts.toPostEntity())
                } catch (e: Exception) {
                    postMyWallDao.insertPosts(oldPosts.toPostMyWallEntity())
                    e.printStackTrace()
//                    if (post.id == 0L && _media.value == noMedia) {
//                        dao.removeById(oldPost.id)
//                        return@launch
//                    } else dao.insertPosts(oldPosts.toPostEntity())
                }
            }
        }
        edited.value = empty
    }

    fun editById(post: Post) {
        edited.value = post
    }

    fun changeMedia(uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        if (uri != null) {
            statusMedia = true
        }
        _media.value = MediaModel(uri, file, attachmentType)
    }

    fun playButtonSong(id: Long) {
        viewModelScope.launch {
            postMyWallDao.playButtonSong(id)
        }
    }

    fun playButtonVideo(id: Long) {
        viewModelScope.launch {
            postMyWallDao.playButtonVideo(id)
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
//        mediaObserver.stop()
//        mediaObserver.apply {
//            mediaPlayer?.setDataSource(
//                post.attachment?.url
//            )
//        }.play()
    }

    fun pauseVideo() {
        viewModelScope.launch {
            mediaObserver.pause()
        }
    }

    fun mentionUsers(listMentioned: Set<Long>, listMap: Map<Long, UserPreview>) {
        listMapUser = listMap
        listMentionedUser = listMentioned
    }

    fun addLocation(coords: Coordinates) {
        coordinates = coords
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
