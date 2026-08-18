package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AuthState
import ru.netology.nework.error.ErrorCode400
import ru.netology.nework.error.ErrorCode404
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.util.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: UserRepository
): ViewModel() {
    private val _authState = MutableLiveData(AuthState())
    val authState: LiveData<AuthState>
        get() =_authState

    private val _errorPost400 = SingleLiveEvent<Unit>()
    val errorPost400: LiveData<Unit>
        get() = _errorPost400

    private val _errorPost404 = SingleLiveEvent<Unit>()
    val errorPost404: LiveData<Unit>
        get() = _errorPost404

    fun signIn(login: String, password: String) {
        viewModelScope.launch{
            try {
                _authState.value = repository.signIn(login, password)
            } catch (_: ErrorCode400) {
                _errorPost400.value = Unit
            } catch (_: ErrorCode404) {
                _errorPost404.value = Unit
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}