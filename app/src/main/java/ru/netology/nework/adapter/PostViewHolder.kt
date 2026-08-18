package ru.netology.nework.adapter

import android.annotation.SuppressLint
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
import java.time.ZonedDateTime

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionPostListener: OnInteractionPostListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(post: Post) {
        with(binding) {
            val zonedDateTime = ZonedDateTime.parse(post.published)
            val options = RequestOptions()

            author.text = post.author
            content.text = post.content
            link.text = post.link
            like.text = CountCalculator.calculator(post.likes)
            published.text =
                "${zonedDateTime.dayOfMonth}.${zonedDateTime.monthValue}.${zonedDateTime.year} ${zonedDateTime.hour}:${zonedDateTime.minute}"

            playSong.isChecked = post.playSong
            playVideo.isChecked = post.playVideo
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare

            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            link.visibility = View.GONE
            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            Glide.with(avatar)
                .load(post.authorAvatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(avatar)

            if (post.authorAvatar == null || post.author == "Me") {
                avatar.setImageResource(R.drawable.ic_netology)
            }

            if (post.link != null) {
                link.visibility = View.VISIBLE
            }

            if (post.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(imageContent)
                    .load(post.attachment.url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(imageContent)
            }

            //TODO(Настроить)
//            if (post.attachment?.type == AttachmentType.VIDEO) {
//                groupVideo.visibility = View.VISIBLE
//
//                videoContent.setVideoURI(post.attachment.url.toUri())
//            }
//
            //TODO(Настроить)
//            if (post.attachment?.type == AttachmentType.AUDIO) {
//                groupSong.visibility = View.VISIBLE
//
//                val songFile = post.attachment.url.toUri().toFile()
//
//                val retriever = MediaMetadataRetriever()
//                retriever.setDataSource(songFile.absolutePath)
//                val durationStr =
//                    retriever.extractMetadata(
//                        MediaMetadataRetriever.METADATA_KEY_DURATION
//                    )
//                val duration = durationStr?.toIntOrNull() ?: 0
//                val title = retriever.extractMetadata(
//                    MediaMetadataRetriever.METADATA_KEY_TITLE
//                ) ?: "noName"
//                retriever.release()
//
//                titleSong.text = title
//                timeSong.text = duration.toString()
//            }

            like.setOnClickListener {
                onInteractionPostListener.onLike(post)
            }

            toShare.setOnClickListener {
                onInteractionPostListener.onShare(post)
            }

//            groupPost.setAllOnClickListener {
//                findNavController(it).navigate(
//                    R.id.action_feedFragment_to_postFragment2,
//                    Bundle().apply {
//                        postBundle = gson.toJson(post)
//                    })
//            }

            cardPostConstraint.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_postFragment2,
                    Bundle().apply {
                        postBundle = gson.toJson(post)
                    })
            }

            //TODO(Настроить)
            avatar.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_yourProfileFragment,
                    Bundle().apply {
                        if (post.ownedByMe) {
                            statusProfileFragment = YOUR
                            postFragmentBundle = gson.toJson(post.authorId)
                        } else {
                            statusProfileFragment = USER
                            postFragmentBundle = gson.toJson(post.authorId)
                        }
                    }
                )
            }

            //TODO(Настроить)
            imageContent.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(post)
                        statusPhotoFragment = POST
                    }
                )
            }

            //TODO(Настроить)
//            groupVideo.setAllOnClickListener {
//                onInteractionPostListener.onPlayVideo(post)
//            }

            //TODO(Настроить)
//            playSong.setOnClickListener {
//                onInteractionPostListener.onPlaySong(post)
//            }

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
                val intent = Intent(Intent.ACTION_VIEW, "http://${post.link}".toUri())
                it.context.startActivity(intent)
            }
        }
    }
}