package ru.netology.nework.adapter

import android.widget.VideoView
import ru.netology.nework.dto.Event

interface OnInteractionEventListener {
    fun onLike(event: Event)
    fun onShare(event: Event)
    fun onParticipate(event: Event)
    fun onRemove(event: Event)
    fun onEdit(event: Event)
    fun onPlayVideo(event: Event, videoView: VideoView)
    fun onPlaySong(event: Event)
}