package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.netology.nework.R
import ru.netology.nework.viewmodel.PostViewModel
import ru.netology.nework.databinding.FragmentPostBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.dto.Post
import ru.netology.nework.fragment.NewPostFragment.Companion.textContentArg
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import javax.inject.Inject

@AndroidEntryPoint
class PostFragment : Fragment() {

    @Inject
    lateinit var dao: PostDao
    companion object {
        private var post = Post(
            id = 0,
            author = "Me",
            authorId = 0,
            authorAvatar = "netology",
            video = null,
            content = "",
            published = 0,
            likedByMe = false,
            toShare = false,
            likes = 0,
            attachment = null,
            shared = 0,
            numberViews = 0,
            savedOnTheServer = false,
            viewed = true,
            ownedByMe = false
        )
        private val gson = Gson()
        private var postId: Long = 0
        var Bundle.textPost by StringArg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        applyInset(binding.postFragment)
        val viewModel: PostViewModel by activityViewModels()
        val dialog = BottomSheetDialog(requireContext())

        arguments?.textPost?.let {
            post = gson.fromJson(it, Post::class.java)
            postId = post.id
            arguments?.textPost = null
        }
        with(binding) {
            setValues(binding, post)

            like.setOnClickListener {
                viewModel.likeById(post.id)
            }

            toShare.setOnClickListener {
                viewModel.toShareById(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                viewModel.removeById(post.id)
                                findNavController().navigateUp()
                                true
                            }

                            R.id.edit -> {
                                viewModel.editById(post)
                                findNavController().navigate(
                                    R.id.action_postFragment2_to_newPostFragment,
                                    Bundle().apply {
                                        textContentArg = post.content
                                    }
                                )
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.data.collectLatest {
                        CoroutineScope(Dispatchers.Default).launch {
                            post = dao.getPost(postId).toDto()
                        }
                        setValues(binding, post)
                    }
                }
            }

            imageContent.setOnClickListener {
                Navigation.findNavController(it).navigate(
                    R.id.action_postFragment2_to_photoFragment2,
                    Bundle().apply {
                        textPost = gson.toJson(post)
                    }
                )
            }

            viewModel.dataState.observe(viewLifecycleOwner) {
                if (it.errorCode300) {
                    findNavController().navigateUp()
                }
            }

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
                    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return binding.root
    }

    private fun setValues(binding: FragmentPostBinding, post: Post) {
        with(binding) {
            author.text = post.author
            content.text = post.content
            published.text = post.published.toString()
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)
            views.text = CountCalculator.calculator(post.numberViews)
            groupVideo.visibility = View.VISIBLE
            content.visibility = View.VISIBLE
            imageContent.visibility = View.VISIBLE
            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            val url = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"
            val urlAttachment = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"
            val options = RequestOptions()

            if (post.attachment == null) {
                imageContent.visibility = View.GONE
            }

            when {
                post.attachment == null -> imageContent.visibility = View.GONE
                post.attachment.uri == null -> Glide.with(binding.imageContent)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.imageContent)
                else -> imageContent.setImageURI(post.attachment.uri.toUri())
            }

            if (post.video == null) {
                groupVideo.visibility = View.GONE
            }

            if (post.content == null) {
                content.visibility = View.GONE
            }

            if (post.authorAvatar != "netology") {
                Glide.with(binding.avatar)
                    .load(url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .apply(options.circleCrop())
                    .into(binding.avatar)
            } else {
                avatar.setImageResource(R.drawable.ic_netology)
            }

            if (post.savedOnTheServer) {
                saved.setImageResource(R.drawable.ic_checked_24)
            }
        }
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