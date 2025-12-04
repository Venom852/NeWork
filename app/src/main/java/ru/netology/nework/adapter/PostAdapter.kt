package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.CardAdBinding
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.databinding.DatePostBinding
import ru.netology.nework.BuildConfig
import ru.netology.nework.adapter.PostDiffCallback
import ru.netology.nework.adapter.PostViewHolder
import ru.netology.nework.dto.Ad
import ru.netology.nework.dto.DatePost
import ru.netology.nework.dto.FeedItem
import ru.netology.nework.dto.Post

class PostAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback()) {
    private val typeAd = 0
    private val typePost = 1
    private val typeDate = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Ad -> typeAd
            is Post -> typePost
            is DatePost -> typeDate
            null -> typePost
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeAd -> AdViewHolder(
                CardAdBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            typePost -> PostViewHolder(
                CardPostBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
            typeDate -> DatePostViewHolder(
                DatePostBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw IllegalArgumentException("unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let {
            when (it) {
                is Post -> (holder as? PostViewHolder)?.bind(it)
                is Ad -> (holder as? AdViewHolder)?.bind(it)
                is DatePost -> (holder as? DatePostViewHolder)?.bind(it)
            }
        }
    }

    class AdViewHolder(
        private val binding: CardAdBinding,
        private val onInteractionListener: OnInteractionListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: Ad) {
            binding.apply {
                val url = "${BuildConfig.BASE_URL}/media/${ad.image}"
                Glide.with(binding.image)
                    .load(url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.image)
                image.setOnClickListener {
                    onInteractionListener.onAdClick(ad)
                }
            }
        }
    }

    class DatePostViewHolder(
        private val binding: DatePostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(datePost: DatePost) {
            binding.apply {
                dateText.text = datePost.date
            }
        }
    }
}