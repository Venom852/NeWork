package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import ru.netology.nework.dao.JobMyDao
import ru.netology.nework.dto.Job
import ru.netology.nework.entity.JobMyEntity
import ru.netology.nework.entity.toJobMyDto
import ru.netology.nework.entity.toJobMyEntity
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.JobMyRepository
import kotlin.Long

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JobMyViewModel @Inject constructor(
    private val repository: JobMyRepository,
    private val jobMyDao: JobMyDao,
    auth: AppAuth,
) : ViewModel() {
    var empty = Job(
        id = 0L,
        name = "",
        position = "",
        start = "",
        finish = null,
        link = null
    )

    val dataMyJob: Flow<List<Job>> = auth.authStateFlow
        .flatMapLatest { repository.data }

    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    val edited = MutableLiveData(empty)

    private val _jobCreated = SingleLiveEvent<Unit>()
    val jobCreated: LiveData<Unit>
        get() = _jobCreated

    private val _errorMyJob403 = SingleLiveEvent<Unit>()
    val errorMyJob403: LiveData<Unit>
        get() = _errorMyJob403

//    private var oldJob = empty
    private var oldJobs = emptyList<Job>()
    private var dateStart = ""
    private var dateEnd: String? = ""

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    oldJobs = jobMyDao.getAll().toJobMyDto()
                }

                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                jobMyDao.insertJobs(oldJobs.toJobMyEntity())
                e.printStackTrace()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                oldJobs = jobMyDao.getAll().toJobMyDto()
            }

            jobMyDao.removeById(id)

            try {
                repository.removeById(id)
            } catch (_: ErrorCode403) {
                jobMyDao.insertJobs(oldJobs.toJobMyEntity())
                _errorMyJob403.value = Unit
            } catch (e: Exception) {
                jobMyDao.insertJobs(oldJobs.toJobMyEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveJob(titleJob: String, jobPost: String, link: String? = null) {
        edited.value?.let { newJob ->
            viewModelScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    oldJobs = jobMyDao.getAll().toJobMyDto()
                }

                var jobServer: Job
                var job = newJob.copy(
                    name = titleJob,
                    position = jobPost
                )

                if (link != null) {
                    job = job.copy(link = link)
                }

                if (dateStart != "" && dateEnd != "") {
                    job = job.copy(start = dateStart, finish = dateEnd)
                } else {
                    job = job.copy(start = dateStart)
                }

                jobMyDao.saveJob(JobMyEntity.fromJobMyDto(job))
                _jobCreated.value = Unit

                try {
                    jobServer = repository.save(job)

//                    oldJob = oldJobs.first()
                    jobMyDao.changeIdJobById(0L, jobServer.id)

                    dateStart = ""
                    dateEnd = ""
                } catch (_: ErrorCode403) {
                    _errorMyJob403.value = Unit
                    jobMyDao.insertJobs(oldJobs.toJobMyEntity())
                } catch (e: Exception) {
                    e.printStackTrace()
                    jobMyDao.insertJobs(oldJobs.toJobMyEntity())
                }
            }
        }

        edited.value = empty
    }

    fun saveDate(startDate: String, endDate: String? = null) {
        viewModelScope.launch {
            dateStart = startDate
            dateEnd = endDate
        }
    }
}
