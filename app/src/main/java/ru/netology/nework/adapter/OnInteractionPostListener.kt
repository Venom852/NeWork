package ru.netology.nework.adapter

import android.widget.VideoView
import ru.netology.nework.dto.Post

interface OnInteractionPostListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onPlayVideo(post: Post, videoView: VideoView)
    fun onPlaySong(post: Post)
}