package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.FragmentPhotoBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.PostViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class PhotoFragment : Fragment() {
    @Inject
    lateinit var daoPost: PostDao
    @Inject
    lateinit var daoEvent: EventDao

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
        published = "",
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
        users = emptyMap(),
        playSong = false,
        playVideo = false
    )

    private var event = Event(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = "",
        datetime = "",
        type = null,
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        participants = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        speakerIds = emptySet(),
        coords = null,
        participatedByMe = false,
        likeOwnerIds = emptySet(),
        participantsIds = emptySet(),
        users = emptyMap(),
        playSong = false,
        playVideo = false
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

        val viewModelPost: PostViewModel by activityViewModels()
        val viewModelEvent: EventViewModel by activityViewModels()

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
                    viewModelPost.likeById(post)
                } else {
                    viewModelEvent.likeById(event)
                }
            }

            toShare.setOnClickListener {
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

            //TODO(Настроить)
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (statusFragment == POST) {
                        viewModelPost.dataPost.collectLatest {
                            CoroutineScope(Dispatchers.IO).launch {
                                post = daoPost.getPost(postId).toPostDto()
                            }

                            setValuesPost(binding, post)
                        }
                    } else {
                        viewModelEvent.dataEvent.collectLatest {
                            CoroutineScope(Dispatchers.IO).launch {
                                event = daoEvent.getEvent(eventId).toEventDto()
                            }

                            setValuesEvent(binding, event)
                        }
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
            views.text = CountCalculator.calculator(post.numberViews)

            Glide.with(photo)
                .load(post.attachment?.url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(photo)
        }
    }

    private fun setValuesEvent(binding: FragmentPhotoBinding, event: Event) {
        with(binding) {
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare

            like.text = CountCalculator.calculator(event.likes)
            views.text = CountCalculator.calculator(event.numberViews)

            Glide.with(photo)
                .load(event.attachment?.url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(photo)
        }
    }
}