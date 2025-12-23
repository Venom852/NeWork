package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import ru.netology.nework.dao.JobDao
import ru.netology.nework.dto.Job
import ru.netology.nework.entity.toJobDto
import ru.netology.nework.entity.toJobEntity
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.JobRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository,
    private val jobDao: JobDao,
    auth: AppAuth,
) : ViewModel() {
    val dataUserJob: Flow<List<Job>> = auth.authStateFlow
        .flatMapLatest { repository.data }

    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    private val _errorJob403 = SingleLiveEvent<Unit>()
    val errorJob403: LiveData<Unit>
        get() = _errorJob403

    private var oldJobs = emptyList<Job>()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    oldJobs = jobDao.getAll().toJobDto()
                }

                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                jobDao.insertJobs(oldJobs.toJobEntity())
                e.printStackTrace()
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    oldJobs = jobDao.getAll().toJobDto()
                }

                _dataState.value = FeedModelState(refreshing = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                jobDao.insertJobs(oldJobs.toJobEntity())
                e.printStackTrace()
            }
        }
    }
}
