package ru.netology.nework.adapter

import ru.netology.nework.dto.Ad
import ru.netology.nework.dto.Post

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onAdClick(ad: Ad) {}
}