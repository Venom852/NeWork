package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.databinding.FragmentProfileBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.statusPostAndContent
import ru.netology.nework.util.StringArg
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import ru.netology.nework.viewmodel.AuthViewModel
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
        var Bundle.statusProfileFragment by StringArg
        var status = ""
    }

    private var user = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
    )

    private val gson = Gson()
    private var userId = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        val bindingConfirmationOfExit =
            ConfirmationOfExitBinding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        val dialog = BottomSheetDialog(requireContext())
        var conditionAdd = NEW_POST

        arguments?.userFragmentBundle?.let {
            user = gson.fromJson(it, User::class.java)
            userId = user.id
            arguments?.userFragmentBundle= null
        }

        arguments?.statusProfileFragment?.let {
            status = it
            arguments?.statusProfileFragment= null
        }

        //TODO(Добавить viewModel)
        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
//                    viewModel.likeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
//                viewModel.toShareById(post.id)
            }

            override fun onRemove(post: Post) {
//                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
//                viewModel.editById(post)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        statusPostAndContent = post.content
                    }
                )
            }

            override fun onPlayVideo(post: Post) {
                //TODO(Добавить viewModel)
            }

            override fun onPlaySong(post: Post) {
                //TODO(Добавить viewModel)
            }
        })

        val jobAdapter = JobAdapter(object : OnInteractionJobListener {
            override fun onDelete(job: Job) {
                //TODO(Добавить viewModel)
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

        binding.job.adapter = jobAdapter.withLoadStateHeaderAndFooter(
            header = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    jobAdapter.retry()
                }
            }),
            footer = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    jobAdapter.retry()
                }
            })
        )

        //TODO(Проверить запрос)
        val url = "${BuildConfig.BASE_URL}/avatars/${user.avatar}"

        Glide.with(binding.photo)
            .load(url)
            .error(R.drawable.ic_error_24)
            .timeout(10_000)
            .into(binding.photo)

        with(binding) {
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
                            statusPostAndContent = NEW_POST
                        }
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_yourProfileFragment_to_newJobFragment
                    )
                }
            }

            wallButton.setOnClickListener {
                conditionAdd = NEW_POST
                srlPosts.visibility = View.VISIBLE
                srlJobs.visibility = View.GONE
            }

            jobsButton.setOnClickListener {
                //TODO(Добавить состояние Job)
//                conditionAdd =
                srlPosts.visibility = View.GONE
                srlJobs.visibility = View.VISIBLE
            }
        }

        with (bindingConfirmationOfExit) {
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
                //TODO(Добавить viewModel)
//                viewModel.data.collectLatest(postAdapter::submitData)
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

        binding.srlPosts.setOnRefreshListener(postAdapter::refresh)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //TODO(Добавить viewModel)
//                viewModel.data.collectLatest(postAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlJobs.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        binding.srlPosts.setOnRefreshListener(jobAdapter::refresh)

        if (status == USER) {
            //TODO(Заменить текст для установки на toolbar)
            binding.toolbar.title = context?.getString(R.string.choose_users)
            binding.logOut.visibility = View.GONE
            binding.add.visibility = View.GONE
        }

        //TODO(Добавить viewModel)
        viewModel.bottomSheet.observe(viewLifecycleOwner) {
            dialog.setCancelable(false)
            dialog.setContentView(bindingErrorCode400And500.root)
            dialog.show()
        }

        bindingErrorCode400And500.errorCode400And500.detectSwipe { event ->
            val text = when (event) {
                SwipeDirection.Down -> "onSwipeDown"
                SwipeDirection.Left -> "onSwipeLeft"
                SwipeDirection.Right -> "onSwipeRight"
                SwipeDirection.Up -> "onSwipeUp"
            }

            if (text == "onSwipeDown") {
                dialog.dismiss()
            }
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