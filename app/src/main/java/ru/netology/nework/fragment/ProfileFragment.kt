package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.adapter.JobAdapter
import ru.netology.nework.adapter.OnInteractionPostListener
import ru.netology.nework.adapter.OnInteractionJobListener
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.adapter.PostLoadingStateAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ConfirmationOfExitBinding
import ru.netology.nework.databinding.FragmentProfileBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.fragment.NewJobFragment.Companion.NEW_JOB
import ru.netology.nework.fragment.NewPostFragment.Companion.EDITING_NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewPostFragment.Companion.statusFragment
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.JobMyViewModel
import ru.netology.nework.viewmodel.JobViewModel
import ru.netology.nework.viewmodel.MyWallViewModel
import ru.netology.nework.viewmodel.UserWallViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    companion object {
        const val YOUR = "your"
        const val USER = "user"
        var Bundle.userFragmentBundle by StringArg
        var Bundle.postFragmentBundle by StringArg
        var Bundle.eventFragmentBundle by StringArg
        var Bundle.statusProfileFragment by StringArg
        var status = ""
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
        published = Instant.now(),
        datetime = Instant.now(),
        eventType = null,
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
        users = emptyMap(),
        playSong = false,
        playVideo = false
    )

    private var user = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
    )

    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        val bindingConfirmationOfExit =
            ConfirmationOfExitBinding.inflate(layoutInflater, container, false)

        val viewModelMyWall: MyWallViewModel by activityViewModels()
        val viewModelUserWall: UserWallViewModel by activityViewModels()
        val viewModelJobMy: JobMyViewModel by activityViewModels()
        val viewModelJob: JobViewModel by activityViewModels()

        applyInset(binding.main)

        val dialog = BottomSheetDialog(requireContext())
        var conditionAdd = NEW_POST

        arguments?.userFragmentBundle?.let {
            user = gson.fromJson(it, User::class.java)

            if (auth.authStateFlow.value.id != user.id) {
                viewModelUserWall.saveAuthorId(user.id)
                status = USER
            } else {
                status = YOUR
            }

            arguments?.userFragmentBundle = null
        }

        arguments?.postFragmentBundle?.let {
            post = gson.fromJson(it, Post::class.java)

            viewModelUserWall.saveAuthorId(post.id)

            arguments?.postFragmentBundle = null
        }

        arguments?.eventFragmentBundle?.let {
            event = gson.fromJson(it, Event::class.java)

            viewModelUserWall.saveAuthorId(event.id)

            arguments?.eventFragmentBundle = null
        }

        arguments?.statusProfileFragment?.let {
            status = it
            arguments?.statusProfileFragment = null
        }

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
                if (status == YOUR) {
                    viewModelMyWall.likeById(post.id)
                } else {
                    viewModelUserWall.likeById(post.id)
                }
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            override fun onRemove(post: Post) {
                viewModelMyWall.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModelMyWall.editById(post)
                findNavController().navigate(
                    R.id.action_yourProfileFragment_to_newPostFragment2,
                    Bundle().apply {
                        newPostFragmentBundle = post.content
                        statusFragment = EDITING_NEW_POST_WALL

                    }
                )
            }

            override fun onPlayVideo(post: Post) {
                if (!post.playSong) {
                    viewModelMyWall.playVideo(post)
                } else {
                    viewModelMyWall.pauseVideo()
                }

                viewModelMyWall.playButtonVideo(post.id)
            }

            override fun onPlaySong(post: Post) {
                if (!post.playSong) {
                    viewModelMyWall.playSong(post)
                } else {
                    viewModelMyWall.pauseSong()
                }

                viewModelMyWall.playButtonSong(post.id)
            }
        })

        val jobAdapter = JobAdapter(object : OnInteractionJobListener {
            override fun onDelete(job: Job) {
                viewModelJobMy.removeById(job.id)
            }

        })

        binding.main.adapter = postAdapter.withLoadStateHeaderAndFooter(
            header = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    postAdapter.retry()
                }
            }),
            footer = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    postAdapter.retry()
                }
            })
        )

        binding.job.adapter = jobAdapter

        //TODO(Изменить запрос)
        var url = ""
        val userAvatar = auth.authStateFlow.value.avatar

        when {
            status == YOUR -> url = "${BuildConfig.BASE_URL}/avatars/${userAvatar}"

            post.authorAvatar != null -> url = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"

            event.authorAvatar != null -> url = "${BuildConfig.BASE_URL}/avatars/${event.authorAvatar}"

            user.avatar != null -> url = "${BuildConfig.BASE_URL}/avatars/${user.avatar}"
        }

        Glide.with(binding.photo)
            .load(url)
            .error(R.drawable.ic_error_24)
            .timeout(10_000)
            .into(binding.photo)

        with(binding) {
            srlPosts.setOnRefreshListener(postAdapter::refresh)

            if (status == USER) {
                toolbar.title = "${user.name}/${user.login}"
                logOut.visibility = View.GONE
                add.visibility = View.GONE
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            logOut.setOnClickListener {
                dialog.setCancelable(false)
                dialog.setContentView(bindingConfirmationOfExit.root)
                dialog.show()
            }

            add.setOnClickListener {
                if (conditionAdd == NEW_POST) {
                    findNavController().navigate(
                        R.id.action_yourProfileFragment_to_newPostFragment,
                        Bundle().apply {
                            newPostFragmentBundle = NEW_POST_WALL
                        }
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_yourProfileFragment_to_newJobFragment
                    )
                }
            }

            wallButton.setOnClickListener {
                applyInset(binding.main)
                conditionAdd = NEW_POST
                srlPosts.visibility = View.VISIBLE
                srlJobs.visibility = View.GONE
            }

            jobsButton.setOnClickListener {
                applyInset(binding.job)
                conditionAdd = NEW_JOB
                srlPosts.visibility = View.GONE
                srlJobs.visibility = View.VISIBLE
            }

            srlJobs.setOnRefreshListener {
                if (status == YOUR) {
                    viewModelJobMy.refreshUsers()
                } else {
                    viewModelJob.refreshUsers()
                }
            }
        }

        with(bindingConfirmationOfExit) {
            close.setOnClickListener {
                dialog.dismiss()
            }

            signOut.setOnClickListener {
                auth.removeAuth()
                findNavController().navigateUp()
                dialog.dismiss()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (status == YOUR) {
                    viewModelMyWall.dataMyWall.collectLatest(postAdapter::submitData)
                } else {
                    viewModelUserWall.dataUserWall.collectLatest(postAdapter::submitData)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlPosts.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (status == YOUR) {
                    viewModelJobMy.dataMyJob.collectLatest(jobAdapter::submitList)
                } else {
                    viewModelJob.dataUserJob.collectLatest(jobAdapter::submitList)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (status == YOUR) {
                    viewModelJobMy.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                } else {
                    viewModelJobMy.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                }
            }
        }

        viewModelMyWall.errorMyWall403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelMyWall.errorMyWall404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelMyWall.errorMyWall415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
        }

        viewModelUserWall.errorWall403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelUserWall.errorWall404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelJobMy.errorMyJob403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelJob.errorJob403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        return binding.root
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