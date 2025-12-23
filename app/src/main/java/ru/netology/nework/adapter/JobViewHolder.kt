package ru.netology.nework.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.R
import ru.netology.nework.databinding.CardJobBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.fragment.ProfileFragment.Companion.status
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR

class JobViewHolder(
    private val binding: CardJobBinding,
    private val onInteractionJobListener: OnInteractionJobListener,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(job: Job) {
        with(binding) {
            titleJob.text = job.name
            jobPost.text = job.position

            if (job.finish != null) {
                val text = "${job.start} - ${job.finish}"
                jobTime.text = text
            } else {
                val text = "${job.start} - ${itemView.context.getString(R.string.present_time)}"
                jobTime.text = text
            }

            if (job.link != null) link.text = job.link else link.visibility = View.GONE

            delete.visibility = if (status == YOUR) View.VISIBLE else View.GONE

            delete.setOnClickListener {
                onInteractionJobListener.onDelete(job)
            }
        }
    }
}