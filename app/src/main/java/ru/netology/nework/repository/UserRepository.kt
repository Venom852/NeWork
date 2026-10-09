package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.auth.AuthState
import ru.netology.nework.dto.MediaUpload
import ru.netology.nework.dto.User

interface UserRepository {
    val data: Flow<List<User>>
    suspend fun getAll()
    suspend fun signIn(login: String, password: String): AuthState
    suspend fun signUp(userName: String, login: String, password: String): AuthState
    suspend fun signUpWithAPhoto(userName: String, login: String, password: String, media: MediaUpload): AuthState
}