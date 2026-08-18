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
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.CardEventBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.fragment.EventFragment.Companion.eventBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.EVENT
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.eventFragmentBundle
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener
import java.time.ZonedDateTime
import kotlin.toString

class EventViewHolder(
    private val binding: CardEventBinding,
    private val onInteractionEventListener: OnInteractionEventListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(event: Event) {
        with(binding) {
            val zonedDateTime = ZonedDateTime.parse(event.published)
            val options = RequestOptions()

            author.text = event.author
            content.text = event.content
            link.text = event.link
            like.text = CountCalculator.calculator(event.likes)
            participate.text = CountCalculator.calculator(event.participants)
            published.text =
                "${zonedDateTime.dayOfMonth}.${zonedDateTime.monthValue}.${zonedDateTime.year} ${zonedDateTime.hour}:${zonedDateTime.minute}"
            eventDate.text =
                "${zonedDateTime.dayOfMonth}.${zonedDateTime.monthValue}.${zonedDateTime.year} ${zonedDateTime.hour}:${zonedDateTime.minute}"

            playSong.isChecked = event.playSong
            playVideo.isChecked = event.playVideo
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare
            participate.isChecked = event.participatedByMe

            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            link.visibility = View.GONE
            menu.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE

            Glide.with(avatar)
                .load(event.authorAvatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(avatar)

            if (event.authorAvatar == null || event.author == "Me") {
                avatar.setImageResource(R.drawable.ic_netology)
            }

            if (event.link != null) {
                link.visibility = View.VISIBLE
            }

            when (event.type) {
                EventType.ONLINE -> eventStatus.text = itemView.context.getString(R.string.online)

                EventType.OFFLINE -> eventStatus.text = itemView.context.getString(R.string.offline)

                else -> eventStatus.visibility = View.GONE
            }

            if (event.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(imageContent)
                    .load(event.attachment.url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(imageContent)
            }

            //TODO(Настроить)
//            if (event.attachment?.type == AttachmentType.VIDEO) {
//                groupVideo.visibility = View.VISIBLE
//
//                videoContent.setVideoURI(event.attachment.url.toUri())
//            }
//
            //TODO(Настроить)
//            if (event.attachment?.type == AttachmentType.AUDIO) {
//                groupSong.visibility = View.VISIBLE
//
//                val songFile = event.attachment.url.toUri().toFile()
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
                onInteractionEventListener.onLike(event)
            }

            toShare.setOnClickListener {
                onInteractionEventListener.onShare(event)
            }

            participate.setOnClickListener {
                onInteractionEventListener.onParticipate(event)
            }

//            groupEvent.setAllOnClickListener {
//                findNavController(it).navigate(
//                    R.id.action_feedFragment_to_eventFragment22,
//                    Bundle().apply {
//                        eventBundle = gson.toJson(event)
//                    })
//            }

            cardEventConstraint.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_eventFragment22,
                    Bundle().apply {
                        eventBundle = gson.toJson(event)
                    })
            }

            //TODO(Настроить)
            avatar.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_yourProfileFragment,
                    Bundle().apply {
                        if (event.ownedByMe) {
                            statusProfileFragment = YOUR
                            eventFragmentBundle = gson.toJson(event.id)
                        } else {
                            statusProfileFragment = USER
                            eventFragmentBundle = gson.toJson(event.id)
                        }
                    }
                )
            }

            imageContent.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(event)
                        statusPhotoFragment = EVENT
                    }
                )
            }

            //TODO(Настроить)
//            groupVideo.setAllOnClickListener {
//                onInteractionEventListener.onPlayVideo(event)
//            }
            //TODO(Настроить)
//            playSong.setOnClickListener {
//                onInteractionEventListener.onPlaySong(event)
//            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                onInteractionEventListener.onRemove(event)
                                true
                            }

                            R.id.edit -> {
                                onInteractionEventListener.onEdit(event)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, "http://${event.link}".toUri())
                it.context.startActivity(intent)
            }
        }
    }
}