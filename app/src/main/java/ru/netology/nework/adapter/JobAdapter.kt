package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import ru.netology.nework.databinding.CardJobBinding
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Post

class JobAdapter(
    private val onInteractionJobListener: OnInteractionJobListener
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CardJobBinding.inflate(layoutInflater, parent, false)
        return PostViewHolder(binding, onInteractionJobListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}