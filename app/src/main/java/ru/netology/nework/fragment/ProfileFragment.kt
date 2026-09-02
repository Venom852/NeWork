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
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import ru.netology.nework.dao.UserDao
import ru.netology.nework.databinding.ConfirmationOfExitBinding
import ru.netology.nework.databinding.FragmentProfileBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.fragment.NewJobFragment.Companion.NEW_JOB
import ru.netology.nework.fragment.NewPostFragment.Companion.EDITING_NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewPostFragment.Companion.statusFragment
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.JobMyViewModel
import ru.netology.nework.viewmodel.JobViewModel
import ru.netology.nework.viewmodel.PostMyWallViewModel
import ru.netology.nework.viewmodel.PostUserWallViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth
    @Inject
    lateinit var userDao: UserDao

    companion object {
        const val YOUR = "your"
        const val USER = "user"
        const val ALLOW = "allow"
        const val PROHIBIT = "prohibit"
        var Bundle.userFragmentBundle by StringArg
        var Bundle.postFragmentBundle by StringArg
        var Bundle.eventFragmentBundle by StringArg
        var Bundle.statusProfileFragment by StringArg
        var Bundle.statusPermissionToCross by StringArg
        var statusProfile = YOUR
        var permissionToCross = ALLOW
    }

    //    private var post = Post(
//        id = 0,
//        author = "Me",
//        authorId = 0,
//        authorAvatar = null,
//        authorJob = null,
//        content = "",
//        published = "",
//        link = null,
//        likedByMe = false,
//        toShare = false,
//        likes = 0,
//        numberViews = 0,
//        attachment = null,
//        shared = 0,
//        ownedByMe = false,
//        mentionIds = emptySet(),
//        coords = null,
//        mentionedMe = false,
//        likeOwnerIds = emptySet(),
//        users = emptyMap(),
//        playSong = false,
//        playVideo = false
//    )
//    private var event = Event(
//        id = 0,
//        author = "Me",
//        authorId = 0,
//        authorAvatar = null,
//        authorJob = null,
//        content = "",
//        published = "",
//        datetime = "",
//        type = null,
//        link = null,
//        likedByMe = false,
//        toShare = false,
//        likes = 0,
//        participants = 0,
//        numberViews = 0,
//        attachment = null,
//        shared = 0,
//        ownedByMe = false,
//        speakerIds = emptySet(),
//        coords = null,
//        participatedByMe = false,
//        likeOwnerIds = emptySet(),
//        participantsIds = emptySet(),
//        users = emptyMap(),
//        playSong = false,
//        playVideo = false
//    )
    private var user = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
    )
    private var privateStatusProfile = YOUR
    private var authorId = 0L
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        val bindingConfirmationOfExit =
            ConfirmationOfExitBinding.inflate(layoutInflater, container, false)

        val viewModelPostMyWall: PostMyWallViewModel by activityViewModels()
        val viewModelPostUserWall: PostUserWallViewModel by activityViewModels()
        val viewModelMyJob: JobMyViewModel by activityViewModels()
        val viewModelJob: JobViewModel by activityViewModels()

//        applyInset(binding.main)

        val dialog = BottomSheetDialog(requireContext())
        var conditionAdd = NEW_POST

//        arguments?.userFragmentBundle?.let {
//            user = gson.fromJson(it, User::class.java)
//            authorId = gson.fromJson(it, Long::class.java)

//            if (auth.authStateFlow.value.id != authorId) {
//            viewModelPostUserWall.saveAuthorId(authorId)
//            }

//            arguments?.userFragmentBundle = null
//        }

//        arguments?.postFragmentBundle?.let {
////            post = gson.fromJson(it, Post::class.java)
//            authorId = gson.fromJson(it, Long::class.java)
//
////            if (auth.authStateFlow.value.id != authorId) {
//            viewModelPostUserWall.saveAuthorId(authorId)
////            }
//
//            arguments?.postFragmentBundle = null
//        }

//        arguments?.eventFragmentBundle?.let {
////            event = gson.fromJson(it, Event::class.java)
//            authorId = gson.fromJson(it, Long::class.java)
//
////            if (auth.authStateFlow.value.id != authorId) {
//            viewModelPostUserWall.saveAuthorId(authorId)
////            }
//
//            arguments?.eventFragmentBundle = null
//        }

        arguments?.statusProfileFragment?.let {
            statusProfile = it
            privateStatusProfile = it
            arguments?.statusProfileFragment = null
        }

        arguments?.statusPermissionToCross?.let {
            permissionToCross = it
            arguments?.statusPermissionToCross = null
        }

//        if (status == YOUR) {
//            authorId = auth.authStateFlow.value.id
//        }

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.likeById(post.id)
                } else {
                    viewModelPostUserWall.likeById(post.id)
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
                viewModelPostMyWall.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModelPostMyWall.editById(post)
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
                    viewModelPostMyWall.playVideo(post)
                } else {
                    viewModelPostMyWall.pauseVideo()
                }

                viewModelPostMyWall.playButtonVideo(post.id)
            }

            override fun onPlaySong(post: Post) {
                if (!post.playSong) {
                    viewModelPostMyWall.playSong(post)
                } else {
                    viewModelPostMyWall.pauseSong()
                }

                viewModelPostMyWall.playButtonSong(post.id)
            }

            override fun onSaveAuthorId(authorId: Long) = Unit
        })

        val jobAdapter = JobAdapter(object : OnInteractionJobListener {
            override fun onDelete(job: Job) {
                viewModelMyJob.removeById(job.id)
            }

        })

//        binding.main.adapter = postAdapter.withLoadStateHeaderAndFooter(
//            header = PostLoadingStateAdapter(object :
//                PostLoadingStateAdapter.OnInteractionListener {
//                override fun onRetry() {
//                    postAdapter.retry()
//                }
//            }),
//            footer = PostLoadingStateAdapter(object :
//                PostLoadingStateAdapter.OnInteractionListener {
//                override fun onRetry() {
//                    postAdapter.retry()
//                }
//            })
//        )

        with(binding) {
            main.adapter = postAdapter
            job.adapter = jobAdapter
//            srlPosts.setOnRefreshListener(postAdapter::refresh)

            //TODO(Можно ли использовать во фрагменте базу данных)
//            CoroutineScope(Dispatchers.IO).launch {
//                user = userDao.getUser(authorId).toUserDto()
//            }

//            viewModelPostUserWall.getUser(authorId)

//            viewLifecycleOwner.lifecycleScope.launch {
//                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                    viewModelPostUserWall.dataUserWall.collectLatest {
//                        user = it
//                    }
//                }
//            }

            if (privateStatusProfile == YOUR) {
                viewModelPostMyWall.dataMyUserWall.observe(viewLifecycleOwner) {
                    user = it
                }
            } else {
                viewModelPostUserWall.dataUserWall.observe(viewLifecycleOwner) {
                    user = it
                }
            }

            Glide.with(photo)
                .load(user.avatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(photo)

            if (privateStatusProfile == USER) {
                toolbar.title = "${user.name}/${user.login}"
                logOut.visibility = View.GONE
                add.visibility = View.GONE
            }

            back.setOnClickListener {
                permissionToCross = ALLOW

                //TODO(Проверить)
                lifecycleScope.launch{
                    viewModelPostUserWall.removeAuthorId()
                }
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

            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(p0: TabLayout.Tab?) {
                    if (p0?.position == 0) {
//                        applyInset(binding.main)
                        conditionAdd = NEW_POST
                        srlPosts.visibility = View.VISIBLE
                        srlJobs.visibility = View.GONE
                    } else {
//                        applyInset(binding.job)
                        conditionAdd = NEW_JOB
                        srlPosts.visibility = View.GONE
                        srlJobs.visibility = View.VISIBLE
                    }
//                    if (p0?.text.toString() == R.string.wall.toString()) {
////                        applyInset(binding.main)
//                        conditionAdd = NEW_POST
//                        srlPosts.visibility = View.VISIBLE
//                        srlJobs.visibility = View.GONE
//                    } else {
////                        applyInset(binding.job)
//                        conditionAdd = NEW_JOB
//                        srlPosts.visibility = View.GONE
//                        srlJobs.visibility = View.VISIBLE
//                    }
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) = Unit

                override fun onTabReselected(p0: TabLayout.Tab?) = Unit

            })

//            wallButton.setOnClickListener {
//                applyInset(binding.main)
//                conditionAdd = NEW_POST
//                srlPosts.visibility = View.VISIBLE
//                srlJobs.visibility = View.GONE
//            }
//
//            jobsButton.setOnClickListener {
//                applyInset(binding.job)
//                conditionAdd = NEW_JOB
//                srlPosts.visibility = View.GONE
//                srlJobs.visibility = View.VISIBLE
//            }

            srlPosts.setOnRefreshListener {
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.loadPosts()
                } else {
                    viewModelPostUserWall.loadPosts()
                }
            }

            srlJobs.setOnRefreshListener {
                if (privateStatusProfile == YOUR) {
                    viewModelMyJob.loadJobs()
                } else {
                    viewModelJob.loadJobs()
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

//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                if (status == YOUR) {
//                    viewModelMyWall.dataMyWall.collectLatest(postAdapter::submitData)
//                } else {
//                    viewModelUserWall.dataUserWall.collectLatest(postAdapter::submitData)
//                }
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                postAdapter.loadStateFlow.collectLatest { state ->
//                    binding.srlPosts.isRefreshing =
//                        state.refresh is LoadState.Loading
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.dataPostMyWall.collectLatest(postAdapter::submitList)
                } else {
                    viewModelPostUserWall.dataPostUserWall.collectLatest(postAdapter::submitList)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                } else {
                    viewModelPostUserWall.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (privateStatusProfile == YOUR) {
                    viewModelMyJob.dataMyJob.collectLatest(jobAdapter::submitList)
                } else {
                    viewModelJob.dataUserJob.collectLatest(jobAdapter::submitList)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (privateStatusProfile == YOUR) {
                    viewModelMyJob.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                } else {
                    viewModelMyJob.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.refreshing
                    }
                }
            }
        }

        viewModelPostMyWall.errorMyWall403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelPostMyWall.errorMyWall404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelPostMyWall.errorMyWall415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
        }

        viewModelPostUserWall.errorWall403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelPostUserWall.errorWall404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelMyJob.errorMyJob403.observe(viewLifecycleOwner) {
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