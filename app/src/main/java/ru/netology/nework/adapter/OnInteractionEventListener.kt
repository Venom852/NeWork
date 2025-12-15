package ru.netology.nework.adapter

import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post

interface OnInteractionEventListener {
    fun onLike(event: Event)
    fun onShare(event: Event)
    fun onParticipate(event: Event)
    fun onRemove(event: Event)
    fun onEdit(event: Event)
    fun onPlayVideo(event: Event)
    fun onPlaySong(event: Event)
}