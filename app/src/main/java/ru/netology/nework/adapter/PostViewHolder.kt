package ru.netology.nework.adapter

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.fragment.PhotoFragment.Companion.POST
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.PostFragment.Companion.postBundle
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.postFragmentBundle
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionPostListener: OnInteractionPostListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        with(binding) {
            author.text = post.author
            content.text = post.content
            published.text = post.published.toString()
            playSong.isChecked = post.playSong
            playVideo.isChecked = post.playVideo
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)

            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
            if (post.link != null) link.text = post.link else link.visibility = View.GONE

            //TODO(Проверить запрос)
            val url = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"
            val urlAttachment = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"
            val options = RequestOptions()

            Glide.with(binding.avatar)
                .load(url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(binding.avatar)

            if (post.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(binding.imageContent)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.imageContent)
            }

            if (post.attachment?.type == AttachmentType.VIDEO) {
                groupVideo.visibility = View.VISIBLE

                videoContent.setVideoURI(post.attachment.url.toUri())
            }

            if (post.attachment?.type == AttachmentType.AUDIO) {
                groupSong.visibility = View.VISIBLE

                val songFile = post.attachment.url.toUri().toFile()

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(songFile.absolutePath)
                val durationStr =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                val duration = durationStr?.toIntOrNull() ?: 0
                val title = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_TITLE
                ) ?: "noName"
                retriever.release()

                titleSong.text = title
                timeSong.text = duration.toString()
            }

            like.setOnClickListener {
                onInteractionPostListener.onLike(post)
            }

            toShare.setOnClickListener {
                onInteractionPostListener.onShare(post)
            }

            groupPost.setAllOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_postFragment2,
                    Bundle().apply {
                        postBundle = gson.toJson(post)
                    })
            }

            avatar.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_yourProfileFragment,
                    Bundle().apply {
                        if (post.ownedByMe) {
                            statusProfileFragment = YOUR
                        } else {
                            statusProfileFragment = USER
                            postFragmentBundle = gson.toJson(post.authorId)
                        }
                    }
                )
            }

            imageContent.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(post)
                        statusPhotoFragment = POST
                    }
                )
            }

            groupVideo.setAllOnClickListener {
                onInteractionPostListener.onPlayVideo(post)
            }

            playSong.setOnClickListener {
                onInteractionPostListener.onPlaySong(post)
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                onInteractionPostListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionPostListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, post.link?.toUri())
                it.context.startActivity(intent)
            }
        }
    }
}