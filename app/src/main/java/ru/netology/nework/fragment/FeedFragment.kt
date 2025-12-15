package ru.netology.nework.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import ru.netology.nework.fragment.NewPostFragment.Companion.statusPostAndContent
import ru.netology.nework.fragment.NewEventFragment.Companion.NEW_EVENT_KEY
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
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import javax.inject.Inject

//TODO(Проставить во всех классах аннотации hilt)
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
        val bindingConfirmationOfExit =
            ConfirmationOfExitBinding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        val viewModel: PostViewModel by activityViewModels()
        val viewModelSignIn: SignInViewModel by activityViewModels()
        val viewModelSignUp: SignUpViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        //TODO(Проверить, нужна ли переменная authorization или работать на прямую с viewModelAuth.authenticated)
        val authorization = viewModelAuth.authenticated
        var conditionAdd = NEW_POST

        val postAdapter = PostAdapter(object : OnInteractionPostListener {
            override fun onLike(post: Post) {
//                if (event.ownedByMe) {
                if (authorization) {
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
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.editById(post)
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

        //TODO(Добавить и поменять viewModel)
        val eventAdapter = EventAdapter(object : OnInteractionEventListener {
            override fun onLike(event: Event) {
//                if (event.ownedByMe) {
                if (authorization) {
                    viewModel.likeById(event.id)
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
                viewModel.removeById(event.id)
            }

            override fun onEdit(event: Event) {
                viewModel.editById(event)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        statusPostAndContent = event.content
                    }
                )
            }

            override fun onPlayVideo(event: Event) {
                //TODO(Добавить viewModel)
            }

            override fun onPlaySong(event: Event) {
                //TODO(Добавить viewModel)
            }

            override fun onParticipate(event: Event) {
//                if (event.ownedByMe) {
                if (authorization) {
                    //TODO(Добавить viewModel)
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

        binding.cardEvent.adapter = eventAdapter.withLoadStateHeaderAndFooter(
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

        binding.cardUsers.adapter = userAdapter.withLoadStateHeaderAndFooter(
            header = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    userAdapter.retry()
                }
            }),
            footer = PostLoadingStateAdapter(object :
                PostLoadingStateAdapter.OnInteractionListener {
                override fun onRetry() {
                    userAdapter.retry()
                }
            })
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dataPost.collectLatest(postAdapter::submitData)
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

        binding.srlMainPosts.setOnRefreshListener(postAdapter::refresh)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //TODO(Добавить viewModel)
//                viewModel.data.collectLatest(postAdapter::submitData)
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

        binding.srlMainEvent.setOnRefreshListener(eventAdapter::refresh)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //TODO(Добавить liveData)
//                viewModel.data.collectLatest(postAdapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlMainUsers.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        binding.srlMainUsers.setOnRefreshListener(userAdapter::refresh)

        //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки 300)
//        viewModel.dataState.observe(viewLifecycleOwner) { state ->
//            binding.progress.isVisible = state.loading
//            if (state.errorCode300) {
//                binding.main.isVisible = false
//                binding.errorCode300.error300Group.isVisible = true
//            } else {
//                binding.main.isVisible = true
//                binding.errorCode300.error300Group.isVisible = false
//            }
//            binding.srlMainPosts.isRefreshing = state.refreshing
//            if (state.error) {
//                Snackbar.make(binding.root, R.string.something_went_wrong, Snackbar.LENGTH_LONG)
//                    .setAction(R.string.retry) { viewModel.loadPosts() }
//                    .show()
//            }
//        }

//        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
//            print(state)
//            binding.browse.isVisible = true
//        }

        viewModel.errorPost403.observe(viewLifecycleOwner) {
            dialog.setCancelable(false)
            dialog.setContentView(bindingErrorCode400And500.root)
            dialog.show()
        }

        //TODO(Проверить, нужна ли эта подписка)
        viewModelSignIn.authState.observe(viewLifecycleOwner) {
            if (authorization) {
                postAdapter.refresh()
            }

            if (authorization) {
                eventAdapter.refresh()
            }

            if (authorization) {
                userAdapter.refresh()
            }
        }

        //TODO(Проверить, нужна ли эта подписка)
        viewModelSignUp.authState.observe(viewLifecycleOwner) {
            if (authorization) {
                postAdapter.refresh()
            }

            if (authorization) {
                eventAdapter.refresh()
            }

            if (authorization) {
                userAdapter.refresh()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                auth.authStateFlow.collectLatest { state ->
                    binding.menuAuth.apply {
                        PopupMenu(context, this).apply {
                            inflate(R.menu.auth_menu)
                            menu.let {
                                it.setGroupVisible(R.id.unauthenticated, !viewModelAuth.authenticated)
                                it.setGroupVisible(R.id.authenticated, viewModelAuth.authenticated)
                            }
                        }
                    }

                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
                    if (!authorization) {
                        postAdapter.refresh()
                    }

                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
                    if (!authorization) {
                        eventAdapter.refresh()
                    }

                    //TODO(Проверить, нужно ли менять здесь код при удалении подписок authState)
                    if (!authorization) {
                        userAdapter.refresh()
                    }
                }
            }
        }

        binding.add.setOnClickListener {
            if (authorization) {
                when (conditionAdd) {
                    NEW_POST -> {
                        findNavController().navigate(
                            R.id.action_feedFragment_to_newPostFragment,
                            Bundle().apply {
                                statusPostAndContent = NEW_POST
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

//        binding.browse.setOnClickListener {
//            viewModel.browse()
//            binding.browse.isVisible = false
//        }

        //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки)
//        binding.errorCode300.buttonError.setOnClickListener {
//            viewModel.loadPostsWithoutServer()
//        }

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

        bindingAuthorizationDialogBox.logIn.setOnClickListener {
            findNavController().navigate(
                R.id.action_feedFragment_to_signInFragment2
            )
            dialog.dismiss()
        }

        bindingAuthorizationDialogBox.close.setOnClickListener {
            dialog.dismiss()
        }

        binding.menuAuth.setOnClickListener {
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
                            findNavController().navigate(
                                R.id.action_feedFragment_to_yourProfileFragment,
                                Bundle().apply {
                                    statusProfileFragment = YOUR
                                }
                            )
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

        bindingConfirmationOfExit.close.setOnClickListener {
            dialog.dismiss()
        }

        bindingConfirmationOfExit.signOut.setOnClickListener {
            auth.removeAuth()
            //TODO(Проверить работу фукции, а именно navigate)
            findNavController().navigate(R.id.nav_main)
            dialog.dismiss()
        }

//        binding.posts.setOnClickListener {
//            binding.posts.isChecked = !binding.posts.isChecked
//            binding.srlMainPosts.isVisible = binding.posts.isChecked
//
//            binding.events.isChecked =
//        }

        binding.bottomNavigation.setOnClickListener {
            BottomNavigationView(it.context).apply {
                setOnItemSelectedListener {menuItem ->
                    when (menuItem.itemId) {
                        R.id.posts -> {
                            conditionAdd = NEW_POST
                            binding.srlMainPosts.visibility = View.VISIBLE
                            binding.srlMainEvent.visibility = View.GONE
                            binding.srlMainUsers.visibility = View.GONE
                            true
                        }
                        R.id.events -> {
                            conditionAdd = NEW_EVENT_KEY
                            binding.srlMainPosts.visibility = View.GONE
                            binding.srlMainEvent.visibility = View.VISIBLE
                            binding.srlMainUsers.visibility = View.GONE
                            true
                        }
                        R.id.users -> {
                            binding.srlMainPosts.visibility = View.GONE
                            binding.srlMainEvent.visibility = View.GONE
                            binding.srlMainUsers.visibility = View.VISIBLE
                            true
                        }

                        else -> false
                    }
                }
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