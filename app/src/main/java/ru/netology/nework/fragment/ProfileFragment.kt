package ru.netology.nework.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobAdapter
import ru.netology.nework.adapter.OnInteractionPostListener
import ru.netology.nework.adapter.OnInteractionJobListener
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.adapter.PostLoadingStateAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ConfirmationOfExitBinding
import ru.netology.nework.databinding.FragmentProfileBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.fragment.NewJobFragment.Companion.NEW_JOB
import ru.netology.nework.fragment.NewPostFragment.Companion.EDITING_NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST_WALL
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewPostFragment.Companion.statusPostFragment
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.JobMyViewModel
import ru.netology.nework.viewmodel.JobViewModel
import ru.netology.nework.viewmodel.PostMyWallViewModel
import ru.netology.nework.viewmodel.PostUserWallViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
@SuppressLint("SetTextI18n")
class ProfileFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    companion object {
        const val YOUR = "your"
        const val USER = "user"
        const val ALLOW = "allow"
        const val PROHIBIT = "prohibit"
        var Bundle.profileFragmentBundle by StringArg
        var Bundle.statusProfileFragment by StringArg
        var Bundle.statusPermissionToCross by StringArg
        var statusProfile = YOUR
        var permissionToCross = ALLOW
    }

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

        val dialog = BottomSheetDialog(requireContext())
        var conditionAdd = NEW_POST

        arguments?.statusProfileFragment?.let {
            statusProfile = it
            privateStatusProfile = it
            arguments?.statusProfileFragment = null
        }

        arguments?.statusPermissionToCross?.let {
            permissionToCross = it
            arguments?.statusPermissionToCross = null
        }

        arguments?.profileFragmentBundle?.let {
            authorId = gson.fromJson(it, Long::class.java)

            if (privateStatusProfile == USER) {
                viewModelJob.loadJobs(authorId)
            }

            arguments?.profileFragmentBundle = null
        }

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.likeById(post)
                } else {
                    viewModelPostUserWall.likeById(post)
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
                        statusPostFragment = EDITING_NEW_POST_WALL

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
        })

        val jobAdapter = JobAdapter(object : OnInteractionJobListener {
            override fun onDelete(job: Job) {
                viewModelMyJob.removeById(job.id)
            }
        })

        with(binding) {
            main.adapter = postAdapter.withLoadStateHeaderAndFooter(
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
            job.adapter = jobAdapter

            srlPosts.setOnRefreshListener(postAdapter::refresh)
            srlJobs.setOnRefreshListener {
                if (privateStatusProfile == YOUR) {
                    viewModelMyJob.loadJobs()
                } else {
                    viewModelJob.loadJobs(authorId)
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (privateStatusProfile == YOUR) {
                        viewModelPostMyWall.dataMyUserWall(authorId).collectLatest {
                            user = it

                            downloadPhoto(this@with)
                        }
                    } else {
                        viewModelPostUserWall.dataUserWall(authorId).collectLatest {
                            user = it

                            downloadPhoto(this@with)

                            if (privateStatusProfile == USER) {
                                fillingToolbar(binding)
                            }

                        }
                    }
                }
            }

            back.setOnClickListener {
                permissionToCross = ALLOW

                lifecycleScope.launch{
                    viewModelPostUserWall.removeUserWall()
                    viewModelJob.removeJobUser()
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
                            statusPostFragment = NEW_POST_WALL
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
                        conditionAdd = NEW_POST

                        srlPosts.visibility = View.VISIBLE
                        srlJobs.visibility = View.GONE
                    } else {
                        conditionAdd = NEW_JOB

                        srlPosts.visibility = View.GONE
                        srlJobs.visibility = View.VISIBLE
                    }
                }

                override fun onTabUnselected(p0: TabLayout.Tab?) = Unit

                override fun onTabReselected(p0: TabLayout.Tab?) = Unit

            })
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
                if (privateStatusProfile == YOUR) {
                    viewModelPostMyWall.dataPostMyWall.collectLatest(postAdapter::submitData)
                } else {
                    viewModelPostUserWall.dataPostUserWall(authorId).collectLatest(postAdapter::submitData)
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
                        binding.srlJobs.isRefreshing = state.loading
                    }
                } else {
                    viewModelJob.dataState.collectLatest { state ->
                        binding.progress.isVisible = state.loading
                        binding.srlJobs.isRefreshing = state.loading
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

    private fun fillingToolbar(binding: FragmentProfileBinding) {
        with(binding){
            title.text = "${user.name}/${user.login}"
            logOut.visibility = View.GONE
            add.visibility = View.GONE
        }
    }

    private fun downloadPhoto(binding: FragmentProfileBinding) {
        with(binding) {
            Glide.with(photo)
                .load(user.avatar.toString())
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(photo)
        }
    }
}