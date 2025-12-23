package ru.netology.nework.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import ru.netology.nework.dao.UserDao
import ru.netology.nework.dto.User
import ru.netology.nework.entity.toUserDto
import ru.netology.nework.entity.toUserEntity
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.UserRepository

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val userDao: UserDao,
    auth: AppAuth,
) : ViewModel() {
    var empty = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
    )

    val dataUser: Flow<List<User>> = auth.authStateFlow
        .flatMapLatest { repository.data }
    var dataListUser = emptyFlow<List<User>>()

    private val _dataState = MutableStateFlow(FeedModelState())
    val dataState: Flow<FeedModelState>
        get() = _dataState

    private var oldUsers = emptyList<User>()
    private val listUsers = mutableListOf<User>()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    oldUsers = userDao.getAllUser().toUserDto()
                }

                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                userDao.insertUsers(oldUsers.toUserEntity())
                e.printStackTrace()
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    oldUsers = userDao.getAllUser().toUserDto()
                }

                _dataState.value = FeedModelState(refreshing = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: Exception) {
                userDao.insertUsers(oldUsers.toUserEntity())
                e.printStackTrace()
            }
        }
    }

    fun saveUsers(users: Set<Long>) {
        viewModelScope.launch {
            try {
                users.forEach {
                    async {
                        listUsers.add(userDao.getUser(it).toUserDto())
                    }.await()
                }

                dataListUser = flow { listUsers.toList() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
