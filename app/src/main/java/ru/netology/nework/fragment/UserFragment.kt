package ru.netology.nework.fragment

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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.OnInteractionUserListener
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUserBinding
import ru.netology.nework.dto.User
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.PostMyWallViewModel
import ru.netology.nework.viewmodel.UserViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.netology.nework.auth.AppAuth
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
@OptIn(ExperimentalCoroutinesApi::class)
class UserFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    companion object {
        const val CHOOSING_MENTIONED_USER_POST = "choosingMentionedUserPost"
        const val CHOOSING_MENTIONED_USER_WALL = "choosingMentionedUserWall"
        const val CHOOSING_SPEAKERS_USER = "choosingSpeakersUser"
        const val LIKE = "like"
        const val MENTIONED = "mentioned"
        const val PARTICIPANTS = "participants"
        const val SPEAKERS = "speakers"
        var status = ""

        var Bundle.statusUserFragment by StringArg
        var Bundle.userBundleFragment by StringArg
    }

    private val gson = Gson()
    private val listUsers = mutableListOf<User>()
    private var users = listOf<Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentUserBinding.inflate(layoutInflater, container, false)

        val viewModelPost: PostViewModel by activityViewModels()
        val viewModelMyWall: PostMyWallViewModel by activityViewModels()
        val viewModelEvent: EventViewModel by activityViewModels()
        val viewModelUser: UserViewModel by activityViewModels()

        val listIdUsers = mutableSetOf<Long>()
        val listMapUsers = mutableMapOf<Long, UserPreview>()
        val typeToken: Type = object : TypeToken<List<Long>>() {}.type

        val userAdapter = UserAdapter(object : OnInteractionUserListener {
            override fun onRadioButton(user: User) {
                if (listIdUsers.any { it == user.id }) {
                    listMapUsers.remove(user.id, UserPreview(user.name, user.avatar))
                    listIdUsers.remove(user.id)
                } else {
                    listMapUsers[user.id] = UserPreview(user.name, user.avatar)
                    listIdUsers.add(user.id)
                }
            }
        })

        arguments?.statusUserFragment?.let {
            status = it
            arguments?.statusUserFragment = null
        }

        arguments?.userBundleFragment?.let {
            users = gson.fromJson(it, typeToken)
            arguments?.userBundleFragment = null
        }

        when (status) {
            CHOOSING_MENTIONED_USER_POST, CHOOSING_MENTIONED_USER_WALL, CHOOSING_SPEAKERS_USER ->
                binding.toolbar.title = context?.getString(R.string.choose_users)

            LIKE -> {
                binding.toolbar.title = context?.getString(R.string.likers)
                binding.save.visibility = View.GONE
            }

            MENTIONED -> {
                binding.toolbar.title = context?.getString(R.string.mentioned)
                binding.save.visibility = View.GONE
            }

            PARTICIPANTS -> {
                binding.toolbar.title = context?.getString(R.string.participants)
                binding.save.visibility = View.GONE
            }

            SPEAKERS -> {
                binding.toolbar.title = context?.getString(R.string.speakers)
                binding.save.visibility = View.GONE
            }
        }

        with(binding) {
            cardUser.adapter = userAdapter

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            save.setOnClickListener {
                when (status) {
                    CHOOSING_MENTIONED_USER_POST -> {
                        Toast.makeText(requireContext(), R.string.users_mentioned, Toast.LENGTH_SHORT)
                            .show()
                        viewModelPost.mentionUsers(listIdUsers, listMapUsers)
                    }

                    CHOOSING_MENTIONED_USER_WALL -> {
                        Toast.makeText(requireContext(), R.string.users_mentioned, Toast.LENGTH_SHORT)
                            .show()
                        viewModelMyWall.mentionUsers(listIdUsers, listMapUsers)
                    }

                    CHOOSING_SPEAKERS_USER -> {
                        Toast.makeText(requireContext(), R.string.speakers_added, Toast.LENGTH_SHORT)
                            .show()
                        viewModelEvent.speakersAdded(listIdUsers, listMapUsers)
                    }
                }

                findNavController().navigateUp()
            }

            srlUsers.setOnRefreshListener {
                if (status == CHOOSING_MENTIONED_USER_POST ||
                    status == CHOOSING_MENTIONED_USER_WALL ||
                    status == CHOOSING_SPEAKERS_USER) {
                    viewModelUser.loadUsers()
                } else {
                    binding.srlUsers.isRefreshing = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (status == CHOOSING_MENTIONED_USER_POST ||
                    status == CHOOSING_MENTIONED_USER_WALL ||
                    status == CHOOSING_SPEAKERS_USER) {
                    viewModelUser.dataUser.collectLatest(userAdapter::submitList)
                } else {
                    viewModelUser.dataUser.collectLatest {list ->
                        users.forEach {userId->
                            list.forEach {
                                if (it.id == userId) {
                                    listUsers.add(it)
                                }
                            }
                        }

                        userAdapter.submitList(listUsers)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelUser.dataState.collectLatest { state ->
                    binding.progress.isVisible = state.loading
                    binding.srlUsers.isRefreshing = state.loading
                }
            }
        }

        return binding.root
    }
}