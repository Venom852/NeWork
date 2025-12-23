package ru.netology.nework.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.netology.nework.R
import ru.netology.nework.adapter.PostAdapter
import ru.netology.nework.viewmodel.PostViewModel
import ru.netology.nework.adapter.OnInteractionPostListener
import ru.netology.nework.adapter.OnInteractionEventListener
import ru.netology.nework.adapter.OnInteractionUserListener
import ru.netology.nework.databinding.AuthorizationDialogBoxBinding
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewEventFragment.Companion.NEW_EVENT_KEY
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
import ru.netology.nework.adapter.EventAdapter
import ru.netology.nework.adapter.PostLoadingStateAdapter
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ConfirmationOfExitBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.fragment.NewEventFragment.Companion.statusEventAndContent
import ru.netology.nework.fragment.NewPostFragment.Companion.EDITING_NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.statusFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.UserViewModel
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
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)
        val bindingConfirmationOfExit =
            ConfirmationOfExitBinding.inflate(layoutInflater, container, false)

        applyInset(binding.main)

        val viewModelPost: PostViewModel by activityViewModels()
        val viewModelEvent: EventViewModel by activityViewModels()
        val viewModelUser: UserViewModel by activityViewModels()
//        val viewModelSignIn: SignInViewModel by activityViewModels()
//        val viewModelSignUp: SignUpViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated
        var conditionAdd = NEW_POST

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
                if (authorization) {
                    viewModelPost.likeById(post.id)
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
            }

            override fun onRemove(post: Post) {
                viewModelPost.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModelPost.editById(post)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        newPostFragmentBundle = post.content
                        statusFragment = EDITING_NEW_POST
                    }
                )
            }

            override fun onPlayVideo(post: Post) {
                if (!post.playSong) {
                    viewModelPost.playVideo(post)
                } else {
                    viewModelPost.pauseVideo()
                }

                viewModelPost.playButtonVideo(post.id)
            }

            override fun onPlaySong(post: Post) {
                if (!post.playSong) {
                    viewModelPost.playSong(post)
                } else {
                    viewModelPost.pauseSong()
                }

                viewModelPost.playButtonSong(post.id)
            }
        })

        val eventAdapter = EventAdapter(object : OnInteractionEventListener {
            override fun onLike(event: Event) {
                if (authorization) {
                    viewModelEvent.likeById(event.id)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            override fun onShare(event: Event) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, event.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            override fun onRemove(event: Event) {
                viewModelEvent.removeById(event.id)
            }

            override fun onEdit(event: Event) {
                viewModelEvent.editById(event)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newEventFragment3,
                    Bundle().apply {
                        statusEventAndContent = event.content
                    }
                )
            }

            override fun onPlayVideo(event: Event) {
                if (!event.playSong) {
                    viewModelEvent.playVideo(event)
                } else {
                    viewModelEvent.pauseVideo()
                }

                viewModelEvent.playButtonVideo(event.id)
            }

            override fun onPlaySong(event: Event) {
                if (!event.playSong) {
                    viewModelEvent.playSong(event)
                } else {
                    viewModelEvent.pauseSong()
                }

                viewModelEvent.playButtonSong(event.id)
            }

            override fun onParticipate(event: Event) {
                if (authorization) {
                    viewModelEvent.participateById(event.id)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }
        })

        val userAdapter = UserAdapter(object : OnInteractionUserListener {
            override fun onRadioButton(user: User) = Unit
        })

        with(binding) {
            srlMainPosts.visibility = View.VISIBLE
            srlMainEvent.visibility = View.GONE
            srlMainUsers.visibility = View.GONE
            progress.visibility = View.GONE

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

            cardEvent.adapter = eventAdapter.withLoadStateHeaderAndFooter(
                header = PostLoadingStateAdapter(object :
                    PostLoadingStateAdapter.OnInteractionListener {
                    override fun onRetry() {
                        eventAdapter.retry()
                    }
                }),
                footer = PostLoadingStateAdapter(object :
                    PostLoadingStateAdapter.OnInteractionListener {
                    override fun onRetry() {
                        eventAdapter.retry()
                    }
                })
            )

            cardUsers.adapter = userAdapter

            srlMainPosts.setOnRefreshListener(postAdapter::refresh)
            srlMainEvent.setOnRefreshListener(eventAdapter::refresh)

            add.setOnClickListener {
                if (authorization) {
                    when (conditionAdd) {
                        NEW_POST -> {
                            findNavController().navigate(
                                R.id.action_feedFragment_to_newPostFragment,
                                Bundle().apply {
                                    newPostFragmentBundle = NEW_POST
                                }
                            )
                        }

                        NEW_EVENT_KEY -> {
                            findNavController().navigate(
                                R.id.action_feedFragment_to_newEventFragment,
                                Bundle().apply {
                                    statusEventAndContent = NEW_EVENT_KEY
                                }
                            )
                        }
                    }
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            menuAuth.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.auth_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.signIn -> {
                                findNavController().navigate(
                                    R.id.action_feedFragment_to_signInFragment2
                                )
                                true
                            }

                            R.id.signUp -> {
                                findNavController().navigate(
                                    R.id.action_feedFragment_to_signUpFragment2
                                )
                                true
                            }

                            R.id.yourProfile -> {
                                if (authorization) {
                                    findNavController().navigate(
                                        R.id.action_feedFragment_to_yourProfileFragment,
                                        Bundle().apply {
                                            statusProfileFragment = YOUR
                                        }
                                    )
                                } else {
                                    dialog.setCancelable(false)
                                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                                    dialog.show()
                                }
                                true
                            }

                            R.id.signOut -> {
                                dialog.setCancelable(false)
                                dialog.setContentView(bindingConfirmationOfExit.root)
                                dialog.show()
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            bottomNavigation.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.posts -> {
                        conditionAdd = NEW_POST
                        applyInset(binding.main)

                        srlMainPosts.visibility = View.VISIBLE
                        srlMainEvent.visibility = View.GONE
                        srlMainUsers.visibility = View.GONE
                        progress.visibility = View.GONE

                        true
                    }
                    R.id.events -> {
                        conditionAdd = NEW_EVENT_KEY
                        applyInset(binding.cardEvent)

                        srlMainPosts.visibility = View.GONE
                        srlMainEvent.visibility = View.VISIBLE
                        srlMainUsers.visibility = View.GONE
                        progress.visibility = View.GONE

                        true
                    }
                    R.id.users -> {
                        applyInset(binding.cardUsers)

                        srlMainPosts.visibility = View.GONE
                        srlMainEvent.visibility = View.GONE
                        srlMainUsers.visibility = View.VISIBLE
                        progress.visibility = View.VISIBLE

                        true
                    }

                    else -> false
                }
            }
        }

        with(bindingAuthorizationDialogBox) {
            logIn.setOnClickListener {
                findNavController().navigate(
                    R.id.action_feedFragment_to_signInFragment2
                )
                dialog.dismiss()
            }

            close.setOnClickListener {
                dialog.dismiss()
            }
        }

        with(bindingConfirmationOfExit) {
            close.setOnClickListener {
                dialog.dismiss()
            }

            signOut.setOnClickListener {
                auth.removeAuth()
                findNavController().navigate(R.id.nav_main)
                dialog.dismiss()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelPost.dataPost.collectLatest(postAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlMainPosts.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelEvent.dataEvent.collectLatest(eventAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlMainEvent.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.dataUser.collectLatest(userAdapter::submitList)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.dataState.collectLatest { state ->
                    binding.progress.isVisible = state.loading
                    binding.srlMainUsers.isRefreshing = state.refreshing
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.authStateFlow.collectLatest { state ->
                    binding.menuAuth.apply {
                        PopupMenu(context, this).apply {
                            inflate(R.menu.auth_menu)

                            if (menu.findItem(R.id.unauthenticated) != null && menu.findItem(R.id.authenticated) != null) {
                                menu.findItem(R.id.unauthenticated).isVisible = !authorization
                                menu.findItem(R.id.authenticated).isVisible = authorization
                            }
//                            menu.let {
//                                it.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
//                                it.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
//                            }
                        }
                    }

                    postAdapter.refresh()
                    eventAdapter.refresh()

//                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
//                    if (!authorization) {
//                        postAdapter.refresh()
//                    }
//
//                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
//                    if (!authorization) {
//                        eventAdapter.refresh()
//                    }
//
//                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
//                    if (!authorization) {
//                        userAdapter.refresh()
//                    }
                }
            }
        }

        viewModelPost.errorPost403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelPost.errorPost404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelPost.errorPost415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT).show()
        }

        viewModelEvent.errorEvent403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelEvent.errorEvent404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelEvent.errorEvent415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT).show()
        }

//        TODO(Проверить, нужна ли эта подписка)
//        viewModelSignIn.authState.observe(viewLifecycleOwner) {
//            if (authorization) {
//                postAdapter.refresh()
//            }
//
//            if (authorization) {
//                eventAdapter.refresh()
//            }
//
////            if (authorization) {
////                userAdapter.refresh()
////            }
//        }
//
//        //TODO(Проверить, нужна ли эта подписка)
//        viewModelSignUp.authState.observe(viewLifecycleOwner) {
//            if (authorization) {
//                postAdapter.refresh()
//            }
//
//            if (authorization) {
//                eventAdapter.refresh()
//            }
//
////            if (authorization) {
////                userAdapter.refresh()
////            }
//        }
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