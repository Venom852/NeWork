package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.OnInteractionUserListener
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentUserBinding
import ru.netology.nework.dto.User
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import kotlin.getValue

@AndroidEntryPoint
class UserFragment : Fragment() {
    companion object {
        const val CHOOSING_MENTIONED_USER = "choosingMentionedUser"
        const val CHOOSING_SPEAKERS_USER = "choosingSpeakersUser"
        const val LIKE = "like"
        const val MENTIONED = "mentioned"
        const val PARTICIPANTS = "participants"
        const val SPEAKERS = "speakers"
        var status = ""

        var Bundle.statusUserFragment by StringArg
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentUserBinding.inflate(layoutInflater, container, false)
        val listUsers = mutableListOf<Long>()
        val viewModelPost: PostViewModel by activityViewModels()

        applyInset(binding.root)

        val userAdapter = UserAdapter(object : OnInteractionUserListener {
            override fun onRadioButton(user: User) {
                //TODO(Проверить работу логики и функции remove)
                if (listUsers.any {it == user.id}) {
                    listUsers.remove(user.id)
                } else {
                    listUsers.add(user.id)
                }
            }
        })

        binding.cardUser.adapter = userAdapter

        arguments?.statusUserFragment?.let {
            status = it
            arguments?.statusUserFragment = null
        }

        when (status) {
            CHOOSING_MENTIONED_USER, CHOOSING_SPEAKERS_USER -> binding.toolbar.title = context?.getString(R.string.choose_users)

//            CHOOSING_SPEAKERS_USER -> binding.toolbar.title = context?.getString(R.string.choose_users)

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
            back.setOnClickListener {
                findNavController().navigateUp()
            }

            save.setOnClickListener {
                //TODO(Добавить viewModel)
                if (status == CHOOSING_MENTIONED_USER) {
                    Toast.makeText(requireContext(), R.string.users_mentioned, Toast.LENGTH_SHORT).show()
                    viewModelPost.mentionUsers(listUsers)
                } else {

                }

                findNavController().navigateUp()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //TODO(Добавить viewModel)
                when (status) {
//                    CHOOSING -> viewModelPost.dataPost.collectLatest(userAdapter::submitData)

                    LIKE ->

                    MENTIONED ->

                    PARTICIPANTS ->

                    SPEAKERS ->
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userAdapter.loadStateFlow.collectLatest { state ->
                    binding.srlUsers.isRefreshing =
                        state.refresh is LoadState.Loading
                }
            }
        }

        binding.srlUsers.setOnRefreshListener(userAdapter::refresh)

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