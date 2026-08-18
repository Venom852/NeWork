package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.error.ErrorCode403
import ru.netology.nework.error.ErrorCode415
import ru.netology.nework.model.MediaModel
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    private val noPhoto = MediaModel()
    private val _authState = MutableLiveData(AuthState())
    val authState: LiveData<AuthState>
        get() = _authState
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<MediaModel>
        get() = _photo

    private val _errorPost403 = SingleLiveEvent<Unit>()
    val errorPost403: LiveData<Unit>
        get() = _errorPost403

    private val _errorPost415 = SingleLiveEvent<Unit>()
    val errorPost415: LiveData<Unit>
        get() = _errorPost415

    fun signUp(login: String, userName: String, password: String) {
        viewModelScope.launch {
            try {
                when (_photo.value) {
                    noPhoto -> _authState.value = repository.signUp(userName, login, password)
                    else -> _photo.value?.file?.let { file ->
                        _authState.value = repository.signUpWithAPhoto(
                            userName,
                            login,
                            password,
                            MediaUpload(file)
                        )
                    }
                }
            } catch (_: ErrorCode403) {
                _errorPost403.value = Unit
            } catch (_: ErrorCode415) {
                _errorPost415.value = Unit
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = MediaModel(uri, file)
    }
}