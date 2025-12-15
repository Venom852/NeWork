package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.FragmentPhotoBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class PhotoFragment : Fragment() {

    @Inject
    lateinit var dao: PostDao
    companion object {
        const val POST = "post"
        const val EVENT = "event"
        var statusFragment = ""
        var Bundle.statusPhotoFragment by StringArg
        var Bundle.photoBundle by StringArg
    }

    private var post = Post(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        mentionIds = emptySet(),
        coords = null,
        mentionedMe = false,
        likeOwnerIds = emptySet(),
        users = emptyMap()
    )

    private var event = Event(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
        datetime = Instant.now(),
        type = null,
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        speakerIds = emptySet(),
        coords = null,
        participatedByMe = false,
        likeOwnerIds = emptySet(),
        participantsIds = emptySet(),
        users = emptyMap()
    )
    private val gson = Gson()
    private var postId: Long = 0
    private var eventId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPhotoBinding.inflate(layoutInflater, container, false)
        val viewModel: PostViewModel by activityViewModels()

        applyInset(binding.photoFragment)

        arguments?.statusPhotoFragment?.let {
            statusFragment = it
            arguments?.statusPhotoFragment = null
        }

        arguments?.photoBundle?.let {
            if (statusFragment == POST) {
                post = gson.fromJson(it, Post::class.java)
                postId = post.id
            } else {
                event = gson.fromJson(it, Event::class.java)
                eventId = event.id
            }
            arguments?.photoBundle = null
        }

        with(binding) {
            if (statusFragment == POST) {
                setValuesPost(binding, post)
            } else {
                setValuesEvent(binding, event)
            }

            like.setOnClickListener {
                if (statusFragment == POST) {
                    viewModel.likeById(post.id)
                } else {
                    //TODO(Добавить viewModel)
                }
            }

            toShare.setOnClickListener {
                if (statusFragment == POST) {
                    viewModel.toShareById(post.id)
                } else {
                    //TODO(Добавить viewModel)
                }

                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (statusFragment == POST) {
                        viewModel.dataPost.collectLatest {
                            CoroutineScope(Dispatchers.Default).launch {
                                //TODO(Проверить, нужно ли будет убирать запрос из базы данных)
                                post = dao.getPost(postId).toDto()
                            }
                            setValuesPost(binding, post)
                        }
                    } else {
                        //TODO(Добавить viewModel)
                    }
                }
            }
        }

        return binding.root
    }

    private fun setValuesPost(binding: FragmentPhotoBinding, post: Post) {
        with(binding) {
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)
            views.text = CountCalculator.calculator(post.numberViews)

            val urlAttachment = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"

            Glide.with(binding.photo)
                .load(urlAttachment)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(binding.photo)

//            when {
//                post.attachment == null -> photo.visibility = View.GONE
//                post.attachment.uri == null -> Glide.with(binding.photo)
//                    .load(urlAttachment)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .into(binding.photo)
//                else -> photo.setImageURI(post.attachment.uri.toUri())
//            }
        }
    }

    private fun setValuesEvent(binding: FragmentPhotoBinding, event: Event) {
        with(binding) {
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare
            like.text = CountCalculator.calculator(event.likes)
            toShare.text = CountCalculator.calculator(event.shared)
            views.text = CountCalculator.calculator(event.numberViews)

            val urlAttachment = "${BuildConfig.BASE_URL}/media/${event.attachment?.url}"

            Glide.with(binding.photo)
                .load(urlAttachment)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(binding.photo)

//            when {
//                post.attachment == null -> photo.visibility = View.GONE
//                post.attachment.uri == null -> Glide.with(binding.photo)
//                    .load(urlAttachment)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .into(binding.photo)
//                else -> photo.setImageURI(post.attachment.uri.toUri())
//            }
        }
    }

    private fun applyInset(main: View) {
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                if (isImeVisible) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }
}