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
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.model.MediaModel
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.toEventDto
import ru.netology.nework.entity.toEventEntity
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.error.ErrorCode415
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import ru.netology.nework.repository.EventRepository
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val eventDao: EventDao,
    auth: AppAuth,
) : ViewModel() {
    var empty = Event(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
        datetime = Instant.now(),
        eventType = null,
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        speakerIds = emptySet(),
        coords = null,
        participatedByMe = false,
        likeOwnerIds = emptySet(),
        participantsIds = emptySet(),
        users = emptyMap(),
        playSong = false,
        playVideo = false
    )

    private val noMedia = MediaModel()
    private val mediaObserver = MediaLifecycleObserver()
    private val cachedPost: Flow<PagingData<Event>> = eventRepository
        .data
        .cachedIn(viewModelScope)

    val dataEvent: Flow<PagingData<Event>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cachedPost.map { pagingData ->
                pagingData.map { event ->
                    event.copy(ownedByMe = event.authorId == myId)
                }
            }
        }

    val edited = MutableLiveData(empty)

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit>
        get() = _eventCreated

    private val _errorEvent403 = SingleLiveEvent<Unit>()
    val errorEvent403: LiveData<Unit>
        get() = _errorEvent403

    private val _errorEvent404 = SingleLiveEvent<Unit>()
    val errorEvent404: LiveData<Unit>
        get() = _errorEvent404

    private val _errorEvent415 = SingleLiveEvent<Unit>()
    val errorEvent415: LiveData<Unit>
        get() = _errorEvent415

    private val _media = MutableLiveData(noMedia)
    val media: LiveData<MediaModel>
        get() = _media

    private var oldEvent = empty
    private var oldEvents = emptyList<Event>()
    var listSpeakersUsers = emptySet<Long>()
    var listMapUsers = emptyMap<Long, UserPreview>()
    var coordinates = Coordinates(lat = 0.0, long = 0.0)
    var dateTime = ""
    var type = EventType.NOT_ASSIGNED

    fun likeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldEvents = eventDao.getAll().toEventDto()
            }

            val eventLikedByMe = oldEvents.find { it.id == id }?.likedByMe
            eventDao.likeById(id)

            try {
                eventRepository.likeById(id, eventLikedByMe)
            } catch (_: ErrorCode403) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                _errorEvent403.value = Unit
            } catch (_: ErrorCode404) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                _errorEvent404.value = Unit
            } catch (e: Exception) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                e.printStackTrace()
            }
        }
    }

    fun participateById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldEvents = eventDao.getAll().toEventDto()
            }

            val eventParticipatedByMe = oldEvents.find { it.id == id }?.participatedByMe
            eventDao.likeById(id)

            try {
                eventRepository.participateById(id, eventParticipatedByMe)
            } catch (_: ErrorCode403) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                _errorEvent403.value = Unit
            } catch (_: ErrorCode404) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                _errorEvent404.value = Unit
            } catch (e: Exception) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                e.printStackTrace()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldEvents = eventDao.getAll().toEventDto()
            }

            eventDao.removeById(id)

            try {
                eventRepository.removeById(id)
            } catch (_: ErrorCode403) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                _errorEvent403.value = Unit
            } catch (e: Exception) {
                eventDao.insertEvents(oldEvents.toEventEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveContent(content: String) {
        edited.value?.let {
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldEvents = eventDao.getAll().toEventDto()
                }

                var eventServer = empty
                var event = it.copy(
                    content = content,
                    speakerIds = listSpeakersUsers,
                    users = listMapUsers
                )

                if (coordinates.lat != 0.0 || coordinates.long != 0.0) {
                    event = event.copy(coords = coordinates)
                }

                if (type != EventType.NOT_ASSIGNED) {
                    event = event.copy(eventType = type)
                }

                if (dateTime != "") {
                    event = event.copy(datetime = Instant.parse(dateTime))
                }

                eventDao.save(EventEntity.fromEventDto(event))
                _eventCreated.value = Unit

                try {
                    when (_media.value) {
                        noMedia -> eventServer = eventRepository.save(event)
                        else -> _media.value?.file?.let { file ->
                            _media.value?.attachmentType?.let { attachmentType ->
                                eventServer = eventRepository.saveWithAttachment(
                                    event,
                                    MediaUpload(file),
                                    attachmentType
                                )
                            }
                        }
                    }

//                    if (event.id == 0L) {
                        oldEvent = oldEvents.first()
                        eventDao.changeIdEventById(
                            oldEvent.id,
                            eventServer.id,
                            eventServer.author,
                            eventServer.authorId,
                            eventServer.authorAvatar,
                            eventServer.authorJob,
                            eventServer.attachment?.url,
                            eventServer.attachment?.type
                        )

                        _media.value = noMedia
                        listSpeakersUsers = emptySet()
                        coordinates = Coordinates(0.0, 0.0)
                        listMapUsers = emptyMap()
                        dateTime = ""
                        type = EventType.NOT_ASSIGNED
//                    } else {
//
//                    }
                } catch (_: ErrorCode403) {
                    _errorEvent403.value = Unit
                    if (event.id == 0L && _media.value == noMedia) {
                        eventDao.removeById(oldEvent.id)
                        return@launch
                    } else eventDao.insertEvents(oldEvents.toEventEntity())
                } catch (_: ErrorCode415) {
                    _errorEvent415.value = Unit
                    if (event.id == 0L && _media.value == noMedia) {
                        eventDao.removeById(oldEvent.id)
                        return@launch
                    } else eventDao.insertEvents(oldEvents.toEventEntity())
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (event.id == 0L && _media.value == noMedia) {
                        eventDao.removeById(oldEvent.id)
                        return@launch
                    } else eventDao.insertEvents(oldEvents.toEventEntity())
                }
            }
        }
        edited.value = empty
    }

    fun editById(event: Event) {
        viewModelScope.launch {
            edited.value = event
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
            eventDao.playButtonSong(id)
        }
    }

    fun playButtonVideo(id: Long) {
        viewModelScope.launch {
            eventDao.playButtonVideo(id)
        }
    }

    fun playSong(event: Event) {
        mediaObserver.stop()
        mediaObserver.apply {
            mediaPlayer?.setDataSource(
                event.attachment?.url
            )
        }.play()
    }

    fun pauseSong() {
        viewModelScope.launch {
            mediaObserver.pause()
        }
    }

    fun playVideo(event: Event) {
        mediaObserver.stop()
        mediaObserver.apply {
            mediaPlayer?.setDataSource(
                event.attachment?.url
            )
        }.play()
    }

    fun pauseVideo() {
        viewModelScope.launch {
            mediaObserver.pause()
        }
    }

    fun speakersAdded(listSpeakers: Set<Long>, listMap: Map<Long, UserPreview>) {
        viewModelScope.launch {
            listMapUsers = listMap
            listSpeakersUsers = listSpeakers
        }
    }

    fun addLocation(coords: Coordinates) {
        viewModelScope.launch {
            coordinates = coords
        }
    }

    fun saveDate(dateContent: String, eventType: EventType) {
        viewModelScope.launch {
            dateTime = dateContent
            type = eventType
        }
    }
}
