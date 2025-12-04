package ru.netology.nework.fragment

import android.annotation.SuppressLint
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
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.viewmodel.PostViewModel
import ru.netology.nework.adapter.OnInteractionListener
import ru.netology.nework.databinding.AuthorizationDialogBoxBinding
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST_KEY
import ru.netology.nework.fragment.NewPostFragment.Companion.textContentArg
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.SignInViewModel
import ru.netology.nework.viewmodel.SignUpViewModel
import kotlin.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.adapter.PostLoadingStateAdapter
import ru.netology.nework.auth.AppAuth
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)

        applyInset(binding.main)

        val viewModel: PostViewModel by activityViewModels()
        val viewModelSignIn: SignInViewModel by activityViewModels()
        val viewModelSignUp: SignUpViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                if (post.ownedByMe) {
                    viewModel.likeById(post.id)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
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
                viewModel.toShareById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.editById(post)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textContentArg = post.content
                    }
                )
            }
        })

        binding.main.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PostLoadingStateAdapter(object : PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }),
            footer = PostLoadingStateAdapter(object : PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    adapter.retry()
                }
            }))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest(adapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    binding.srl.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        binding.srl.setOnRefreshListener(adapter::refresh)

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.errorCode300) {
                binding.main.isVisible = false
                binding.errorCode300.error300Group.isVisible = true
            } else {
                binding.main.isVisible = true
                binding.errorCode300.error300Group.isVisible = false
            }
            binding.srl.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.something_went_wrong, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { viewModel.loadPosts() }
                    .show()
            }
        }

//        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
//            print(state)
//            binding.browse.isVisible = true
//        }

        viewModel.bottomSheet.observe(viewLifecycleOwner) {
            dialog.setCancelable(false)
            dialog.setContentView(bindingErrorCode400And500.root)
            dialog.show()
        }

        viewModelSignIn.authState.observe(viewLifecycleOwner) {
            if (authorization) {
                adapter.refresh()
            }
        }

        viewModelSignUp.authState.observe(viewLifecycleOwner) {
            if (authorization) {
                adapter.refresh()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.authStateFlow.collectLatest { state ->
                    if (!authorization) {
                        adapter.refresh()
                    }
                }
            }
        }

        binding.add.setOnClickListener {
            if (authorization) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textContentArg = NEW_POST_KEY
                    }
                )
            } else {
                dialog.setCancelable(false)
                dialog.setContentView(bindingAuthorizationDialogBox.root)
                dialog.show()
            }
        }

//        binding.browse.setOnClickListener {
//            viewModel.browse()
//            binding.browse.isVisible = false
//        }

        binding.errorCode300.buttonError.setOnClickListener {
            viewModel.loadPostsWithoutServer()
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
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
            }
        }

        bindingAuthorizationDialogBox.logIn.setOnClickListener {
            findNavController().navigate(
                R.id.action_feedFragment_to_signInFragment2
            )
            dialog.dismiss()
        }

        bindingAuthorizationDialogBox.close.setOnClickListener {
            dialog.dismiss()
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