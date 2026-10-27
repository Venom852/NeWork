package ru.netology.nework.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.MediaController
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
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
import ru.netology.nework.fragment.ProfileFragment.Companion.ALLOW
import ru.netology.nework.fragment.ProfileFragment.Companion.PROHIBIT
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.permissionToCross
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.profileFragmentBundle
import ru.netology.nework.fragment.ProfileFragment.Companion.statusPermissionToCross
import ru.netology.nework.lifecycle.MediaLifecycleObserver
import ru.netology.nework.util.CountCalculator
import java.time.ZonedDateTime

@SuppressLint("SetTextI18n")
class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionPostListener: OnInteractionPostListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    private val mediaObserver = MediaLifecycleObserver()
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
            if (post.attachment?.type == AttachmentType.VIDEO) {
                groupVideo.visibility = View.VISIBLE
            }

            //TODO(Настроить)
            if (post.attachment?.type == AttachmentType.AUDIO) {
                groupSong.visibility = View.VISIBLE

                mediaObserver.reset()
                mediaObserver.apply {
//                    mediaPlayer?.setDataSource(post.attachment.url)
                    mediaPlayer?.setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                }.getDuration{
                    timeSong.text = it
                }

                titleSong.text = itemView.context.getString(R.string.title_song)
            }

            like.setOnClickListener {
                onInteractionPostListener.onLike(post)
            }

            toShare.setOnClickListener {
                onInteractionPostListener.onShare(post)
            }

            cardPostConstraint.setOnClickListener {
                if (permissionToCross == ALLOW) {
                    findNavController(it).navigate(
                        R.id.action_feedFragment_to_postFragment2,
                        Bundle().apply {
                            postBundle = gson.toJson(post)
                        })
                }
            }

            avatar.setOnClickListener {
                if (permissionToCross == ALLOW) {
                    findNavController(it).navigate(
                        R.id.action_feedFragment_to_yourProfileFragment,
                        Bundle().apply {
                            statusPermissionToCross = PROHIBIT

                            if (post.ownedByMe) {
                                statusProfileFragment = YOUR
                                profileFragmentBundle = gson.toJson(post.authorId)
                            } else {
                                statusProfileFragment = USER
                                profileFragmentBundle = gson.toJson(post.authorId)
                            }
                        }
                    )
                }
            }

            imageContent.setOnClickListener {
                if (permissionToCross == ALLOW) {
                    findNavController(it).navigate(
                        R.id.action_feedFragment_to_photoFragment2,
                        Bundle().apply {
                            photoBundle = gson.toJson(post)
                            statusPhotoFragment = POST
                        }
                    )
                }
            }

            //TODO(Настроить)
            playVideo.setOnClickListener {
                playVideo.visibility = View.GONE
//                onInteractionPostListener.onPlayVideo(post, videoContent)

                val mediaController = MediaController(itemView.context)
                mediaController.setAnchorView(videoContent)
                videoContent.setMediaController(mediaController)

                videoContent.apply {
//                    setVideoURI(post.attachment?.url?.toUri())
                    setVideoURI("https://dn710207.ca.archive.org/0/items/youtube-IfeDb-O4gOg/IfeDb-O4gOg.mp4".toUri())

                    setOnPreparedListener {
                        this.start()
                    }

                    setOnCompletionListener {
                        stopPlayback()
                        playVideo.visibility = View.VISIBLE
                    }
                }
            }

            //TODO(Настроить)
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
                val intent = Intent(Intent.ACTION_VIEW, "http://${post.link}".toUri())
                it.context.startActivity(intent)
            }
        }
    }
}