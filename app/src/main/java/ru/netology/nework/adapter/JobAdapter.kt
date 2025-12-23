package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.netology.nework.databinding.CardJobBinding
import ru.netology.nework.dto.Job

class JobAdapter(
    private val onInteractionJobListener: OnInteractionJobListener
) : ListAdapter<Job, JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CardJobBinding.inflate(layoutInflater, parent, false)
        return JobViewHolder(binding, onInteractionJobListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}