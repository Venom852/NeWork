package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.error.ErrorCode400And500
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: PostRepository
): ViewModel() {
    private val noPhoto = PhotoModel()
    private val _authState = MutableLiveData(AuthState())
    val authState: LiveData<AuthState>
        get() =_authState
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo
    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    private val _bottomSheet = SingleLiveEvent<Unit>()
    val bottomSheet: LiveData<Unit>
        get() = _bottomSheet

    fun signUp(userName: String, login: String, password: String) {
        viewModelScope.launch{
            try {
                when (_photo.value) {
                    noPhoto -> _authState.value = repository.signUp(userName, login, password)
                    else -> _photo.value?.file?.let { file ->
                        _authState.value = repository.signUpWithAPhoto(userName, login, password, MediaUpload(file))
                    }
                }
            } catch (e: ErrorCode400And500) {
                _bottomSheet.value = Unit
            } catch (e: UnknownError) {
                _dataState.value = FeedModelState(errorCode300 = true)
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }
}