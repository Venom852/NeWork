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
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.fragment.NewPostFragment.Companion.NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewEventFragment.Companion.NEW_EVENT
import ru.netology.nework.viewmodel.AuthViewModel
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
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
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

//        applyInset(binding.cardPost)
//        applyInset(binding.cardEvent)
//        applyInset(binding.cardUser)

        val viewModelPost: PostViewModel by activityViewModels()
        val viewModelEvent: EventViewModel by activityViewModels()
        val viewModelUser: UserViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val popupMenu = PopupMenu(binding.menuAuth.context, binding.menuAuth).apply {
            inflate(R.menu.auth_menu)
        }
        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated
        var conditionAdd = NEW_POST

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
                //TODO(Настроить поведение, чтобы кнопка не кликалась если нет авторизации)
                if (authorization) {
                    viewModelPost.likeById(post)
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
                //TODO(Настроить поведение, чтобы кнопка не кликалась если нет авторизации)
                if (authorization) {
                    viewModelEvent.likeById(event)
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
                    viewModelEvent.participateById(event)
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
//            cardPost.adapter = postAdapter.withLoadStateHeaderAndFooter(
//                header = PostLoadingStateAdapter(object :
//                    PostLoadingStateAdapter.OnInteractionListener {
//                    override fun onRetry() {
//                        postAdapter.retry()
//                    }
//                }),
//                footer = PostLoadingStateAdapter(object :
//                    PostLoadingStateAdapter.OnInteractionListener {
//                    override fun onRetry() {
//                        postAdapter.retry()
//                    }
//                })
//            )

//            cardEvent.adapter = eventAdapter.withLoadStateHeaderAndFooter(
//                header = PostLoadingStateAdapter(object :
//                    PostLoadingStateAdapter.OnInteractionListener {
//                    override fun onRetry() {
//                        eventAdapter.retry()
//                    }
//                }),
//                footer = PostLoadingStateAdapter(object :
//                    PostLoadingStateAdapter.OnInteractionListener {
//                    override fun onRetry() {
//                        eventAdapter.retry()
//                    }
//                })
//            )

            cardPost.adapter = postAdapter
            cardEvent.adapter = eventAdapter
            cardUser.adapter = userAdapter

            srlMainPosts.setOnRefreshListener { viewModelPost.loadPosts() }
            srlMainEvents.setOnRefreshListener { viewModelEvent.loadEvents() }
            srlMainUsers.setOnRefreshListener { viewModelUser.loadUsers() }

//            srlMainPosts.setOnRefreshListener(postAdapter::refresh)
//            srlMainEvent.setOnRefreshListener(eventAdapter::refresh)

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

                        NEW_EVENT -> {
                            findNavController().navigate(
                                R.id.action_feedFragment_to_newEventFragment,
                                Bundle().apply {
                                    statusEventAndContent = NEW_EVENT
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
                popupMenu.apply {
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
                                //TODO(Нужна ли здесь проверка авторизации?)
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
//                        applyInset(binding.cardPost)

                        srlMainPosts.visibility = View.VISIBLE
                        srlMainEvents.visibility = View.GONE
                        srlMainUsers.visibility = View.GONE
//                        progress.visibility = View.GONE

                        true
                    }

                    R.id.events -> {
                        conditionAdd = NEW_EVENT
//                        applyInset(binding.cardEvent)

                        srlMainPosts.visibility = View.GONE
                        srlMainEvents.visibility = View.VISIBLE
                        srlMainUsers.visibility = View.GONE
//                        progress.visibility = View.GONE

                        true
                    }

                    R.id.users -> {
                        conditionAdd = CHOOSING_MENTIONED_USER_POST
//                        applyInset(binding.cardUser)

                        srlMainPosts.visibility = View.GONE
                        srlMainEvents.visibility = View.GONE
                        srlMainUsers.visibility = View.VISIBLE
//                        progress.visibility = View.GONE

                        true
                    }

                    else -> false
                }
            }
        }

        with(bindingAuthorizationDialogBox) {
            logIn.setOnClickListener {
                findNavController().navigate(R.id.action_feedFragment_to_signInFragment2)
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
                viewModelPost.dataPost.collectLatest(postAdapter::submitList)

//                viewModelPost.dataPost.collectLatest(postAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelPost.dataState.collectLatest { state ->
                    binding.progress.isVisible = state.loading
                    binding.srlMainPosts.isRefreshing = state.refreshing
                }

//                postAdapter.loadStateFlow.collectLatest { state ->
//                    binding.srlMainPosts.isRefreshing =
//                        state.refresh is LoadState.Loading
//                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelEvent.dataEvent.collectLatest(eventAdapter::submitList)

//                viewModelEvent.dataEvent.collectLatest(eventAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelEvent.dataState.collectLatest { state ->
                    binding.progress.isVisible = state.loading
                    binding.srlMainEvents.isRefreshing = state.refreshing
                }

//                eventAdapter.loadStateFlow.collectLatest { state ->
//                    binding.srlMainEvent.isRefreshing =
//                        state.refresh is LoadState.Loading
//                }
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
                    popupMenu.apply {
                        menu.let {
                            it.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
                            it.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
                        }
                    }

                    viewModelPost.loadPosts()
                    viewModelEvent.loadEvents()
                    viewModelUser.loadUsers()

//                    postAdapter.refresh()
//                    eventAdapter.refresh()
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
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
        }

        viewModelEvent.errorEvent403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelEvent.errorEvent404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.event_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelEvent.errorEvent415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
        }

        return binding.root
    }

    private fun applyInset(main: View) {
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
//        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
//            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
//            v.setPadding(
//                v.paddingLeft,
//                systemBars.top,
//                v.paddingRight,
//                if (isImeVisible) imeInsets.bottom else systemBars.bottom
//            )
//            insets
//        }
    }
}