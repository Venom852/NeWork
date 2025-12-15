package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import ru.netology.nework.databinding.CardEventBinding
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post

class EventAdapter(
    private val onInteractionEventListener: OnInteractionEventListener
) : PagingDataAdapter<Event, EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CardEventBinding.inflate(layoutInflater, parent, false)
        return EventViewHolder(binding, onInteractionEventListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}