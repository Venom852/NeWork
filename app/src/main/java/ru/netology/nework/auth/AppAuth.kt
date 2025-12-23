package ru.netology.nework.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val idKey = "id"
    private val tokenKey = "token"
    private val avatarKey = "token"
    private val _authStateFlow: MutableStateFlow<AuthState>

    init {
        val id = prefs.getLong(idKey, 0)
        val token = prefs.getString(tokenKey, null)
        val avatar = prefs.getString(avatarKey, null)

        if (id == 0L || token == null) {
            _authStateFlow = MutableStateFlow(AuthState())

            //TODO(Проверить изменение функции)
            with(prefs.edit()) {
                clear()
                apply()
            }
        } else {
            _authStateFlow = MutableStateFlow(AuthState(id, token, avatar))
        }
    }

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String, avatar: String?) {
        _authStateFlow.value = AuthState(id, token, avatar)

        //TODO(Проверить изменение функции)
        with(prefs.edit()) {
            putLong(idKey, id)
            putString(tokenKey, token)

            if (avatar != null) {
                putString(avatarKey, avatar)
            }

            apply()
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()

        //TODO(Проверить изменение функции)
        with(prefs.edit()) {
            clear()
            commit()
        }
    }
}

data class AuthState(val id: Long = 0, val token: String? = null, val avatar: String? = null)