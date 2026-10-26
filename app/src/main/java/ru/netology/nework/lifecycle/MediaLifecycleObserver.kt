package ru.netology.nework.lifecycle

import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class MediaLifecycleObserver : LifecycleEventObserver {
    var mediaPlayer: MediaPlayer? = MediaPlayer()

    fun play() {
        mediaPlayer?.setOnPreparedListener {
            it.start()
        }

        mediaPlayer?.prepareAsync()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun reset() {
        mediaPlayer?.reset()
    }

    fun getDuration(callback: (String) -> Unit) {
        var duration: String

        mediaPlayer?.setOnPreparedListener {
            val durationSeconds = it.duration / 1000

            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60

            duration = "$minutes:$seconds"
            callback(duration)
        }

        mediaPlayer?.prepareAsync()
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