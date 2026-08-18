package ru.netology.nework.adapter

import ru.netology.nework.dto.Post

interface OnInteractionPostListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onPlayVideo(post: Post)
    fun onPlaySong(post: Post)
}