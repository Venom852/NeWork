package ru.netology.nework.application

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import javax.inject.Inject

@HiltAndroidApp
class NeWorkApplication : Application() {
    private val appScope = CoroutineScope(Dispatchers.Default)

    @Inject
    lateinit var auth: AppAuth

    override fun onCreate() {
        super.onCreate()
        setupAuth()
        MapKitFactory.setApiKey(BuildConfig.MAPS_API_KEY)
    }

    private fun setupAuth() {
        appScope.launch {
            auth.sendPushToken()
        }
    }

}