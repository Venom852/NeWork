package ru.netology.nework.lifecycle

import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MediaLifecycleObserver : LifecycleEventObserver {
    var mediaPlayer: MediaPlayer? = MediaPlayer()

    fun play() {
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            it.start()
        }
    }

    fun pause() {
        mediaPlayer?.setOnPreparedListener {
            it.pause()
        }
    }

    fun stop() {
        mediaPlayer?.setOnPreparedListener {
            it.stop()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> mediaPlayer?.pause()
            Lifecycle.Event.ON_STOP -> {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)
            else -> Unit
        }
    }
}