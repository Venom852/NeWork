package ru.netology.nework.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.FragmentPhotoBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class PhotoFragment : Fragment() {

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
        val binding = FragmentPhotoBinding.inflate(layoutInflater, container, false)
        val viewModel: PostViewModel by activityViewModels()
        applyInset(binding.photoFragment)

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

            back.setOnClickListener {
                findNavController().navigateUp()
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
        }

        return binding.root
    }

    private fun setValues(binding: FragmentPhotoBinding, post: Post) {
        with(binding) {
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)
            views.text = CountCalculator.calculator(post.numberViews)

            val urlAttachment = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"

            when {
                post.attachment == null -> photo.visibility = View.GONE
                post.attachment.uri == null -> Glide.with(binding.photo)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.photo)
                else -> photo.setImageURI(post.attachment.uri.toUri())
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