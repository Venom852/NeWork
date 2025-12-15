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
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.fragment.PhotoFragment.Companion.POST
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.PostFragment.Companion.postBundle
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
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)

//            content.visibility = View.VISIBLE
//            link.visibility = View.GONE
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

//            when {
//                post.attachment == null -> imageContent.visibility = View.GONE
//                post.attachment.uri == null -> Glide.with(binding.imageContent)
//                    .load(urlAttachment)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .into(binding.imageContent)
//                else -> imageContent.setImageURI(post.attachment.uri.toUri())
//            }

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
                //TODO
            }

            if (post.attachment?.type == AttachmentType.AUDIO) {
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
                    R.id.action_feedFragment_to_yourProfileFragment
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