package ru.netology.nework.adapter

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
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
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener

class EventViewHolder(
    private val binding: CardEventBinding,
    private val onInteractionEventListener: OnInteractionEventListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(event: Event) {
        with(binding) {
            author.text = event.author
            content.text = event.content
            published.text = event.published.toString()
            eventDate.text = event.datetime.toString()
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare
            like.text = CountCalculator.calculator(event.likes)
            toShare.text = CountCalculator.calculator(event.shared)

//            content.visibility = View.VISIBLE
//            link.visibility = View.GONE
            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            menu.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE
            if (event.link != null) link.text = event.link else link.visibility = View.GONE

            //TODO(Проверить запрос)
            val url = "${BuildConfig.BASE_URL}/avatars/${event.authorAvatar}"
            val urlAttachment = "${BuildConfig.BASE_URL}/media/${event.attachment?.url}"
            val options = RequestOptions()

            Glide.with(binding.avatar)
                .load(url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(binding.avatar)

//            when {
//                post.attachment == null -> imageContent.visibility = View.GONE
//                post.attachment.uri == null -> Glide.with(binding.imageContent)
//                    .load(urlAttachment)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .into(binding.imageContent)
//                else -> imageContent.setImageURI(post.attachment.uri.toUri())
//            }

            if (event.type == EventType.ONLINE) {
                eventStatus.text = itemView.context.getString(R.string.online)
            } else {
                eventStatus.text = itemView.context.getString(R.string.offline)
            }

            if (event.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(binding.imageContent)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.imageContent)
            }

            if (event.attachment?.type == AttachmentType.VIDEO) {
                groupVideo.visibility = View.VISIBLE
                //TODO
            }

            if (event.attachment?.type == AttachmentType.AUDIO) {
                groupSong.visibility = View.VISIBLE
                //TODO
            }

//            if (post.link != null) {
//                link.visibility = View.VISIBLE
//                link.text = post.link
//            }

//            if (post.video == null) {
//                groupVideo.visibility = View.GONE
//            }

//            if (post.content == null) {
//                content.visibility = View.GONE
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

            groupEvent.setAllOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_eventFragment22,
                    Bundle().apply {
                        eventBundle = gson.toJson(event)
                    })
            }

            avatar.setOnClickListener {
                findNavController(it).navigate(
                    R.id.action_feedFragment_to_yourProfileFragment
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

            groupVideo.setAllOnClickListener {
                onInteractionEventListener.onPlayVideo(event)
            }

            playSong.setOnClickListener {
                onInteractionEventListener.onPlaySong(event)
            }

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
                val intent = Intent(Intent.ACTION_VIEW, event.link?.toUri())
                it.context.startActivity(intent)
            }
        }
    }
}