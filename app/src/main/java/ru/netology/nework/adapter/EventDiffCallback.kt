package ru.netology.nework.adapter

import androidx.recyclerview.widget.DiffUtil
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
}