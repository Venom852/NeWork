package ru.netology.nework.fragment

import android.content.Intent
import android.graphics.Color.RED
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.AuthorizationDialogBoxBinding
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.extensions.DrawableImageProvider
import ru.netology.nework.extensions.ImageInfo
import ru.netology.nework.fragment.NewPostFragment.Companion.statusPostAndContent
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.PhotoFragment.Companion.POST
import ru.netology.nework.fragment.UserFragment.Companion.LIKE
import ru.netology.nework.fragment.UserFragment.Companion.MENTIONED
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import ru.netology.nework.viewmodel.AuthViewModel
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.emptyMap
import kotlin.getValue

@AndroidEntryPoint
class PostFragment : Fragment() {
    @Inject
    lateinit var dao: PostDao

    companion object {
        var Bundle.postBundle by StringArg
    }

    private var post = Post(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        mentionIds = emptySet(),
        coords = null,
        mentionedMe = false,
        likeOwnerIds = emptySet(),
        users = emptyMap()
    )
    private val gson = Gson()
    private var postId = 0L
    private var numberUsers = 0
    private lateinit var yandexMap: Map
    private lateinit var mapKit: MapKit
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var binding: FragmentPostBinding
    private val smoothAnimation = Animation(Animation.Type.SMOOTH, 3F)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        val viewModel: PostViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated

        arguments?.postBundle?.let {
            post = gson.fromJson(it, Post::class.java)
            postId = post.id
            arguments?.postBundle = null
        }

        with(binding) {
            setValues(binding, post)

            //TODO(Проверить)
            like.setOnClickListener {
                if (authorization) {
                    viewModel.likeById(post.id)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
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

            //TODO(Проверить)
            mention.setOnClickListener {
                //TODO(Добавить viewModel)
                if (authorization) {

                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            avatar.setOnClickListener {
                findNavController().navigate(
                    R.id.action_postFragment2_to_yourProfileFragment
                )
            }

            groupVideo.setAllOnClickListener {
                //TODO(Добавить управление видео)
            }

            playSong.setOnClickListener {
                //TODO(Добавить управление звуком)
            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, post.link?.toUri())
                it.context.startActivity(intent)
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            listLikeUsers.setOnClickListener {
                findNavController().navigate(
                    R.id.action_postFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = LIKE
                    }
                )
            }

            listMentioned.setOnClickListener {
                findNavController().navigate(
                    R.id.action_postFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = MENTIONED
                    }
                )
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
                                        statusPostAndContent = post.content
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
                    viewModel.dataPost.collectLatest {
                        CoroutineScope(Dispatchers.Default).launch {
                            //TODO(Проверить, нужно ли будет убирать запрос из базы данных)
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
                        photoBundle = gson.toJson(post)
                        statusPhotoFragment = POST
                    }
                )
            }

            //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки)
            viewModel.dataState.observe(viewLifecycleOwner) {
                if (it.errorCode300) {
                    findNavController().navigateUp()
                }
            }

            viewModel.errorPost403.observe(viewLifecycleOwner) {
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

            bindingAuthorizationDialogBox.logIn.setOnClickListener {
                findNavController().navigate(
                    R.id.action_postFragment2_to_signInFragment2
                )
                dialog.dismiss()
            }

            bindingAuthorizationDialogBox.close.setOnClickListener {
                dialog.dismiss()
            }
        }
        return binding.root
    }

    //TODO(Проверить, можно ли использовать для работы с картой метод onCreate или оставить всё тут)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapView = binding.map
        val mapWindow = mapView.mapWindow

        yandexMap = mapWindow.map
        mapKit = MapKitFactory.getInstance()
        placemarkMapObject = yandexMap.mapObjects.addPlacemark()

        subscribeToLifecycle(mapView)

        if (post.coords?.lat != null && post.coords?.long != null) {
            addMarker(yandexMap, Point(post.coords!!.lat, post.coords!!.long))
            moveToMarker(yandexMap, Point(post.coords!!.lat, post.coords!!.long))
        }

    }

    private fun setValues(binding: FragmentPostBinding, post: Post) {
        with(binding) {
            author.text = post.author
            authorJob.text = post.authorJob
            content.text = post.content
            published.text = post.published.toString()
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare
            like.text = CountCalculator.calculator(post.likes)
            toShare.text = CountCalculator.calculator(post.shared)

            map.visibility = View.GONE
            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE

            //TODO(Проверить, можно ли заменить весь код одной строчкой)
            likeUserOne.visibility = View.GONE
            likeUserTwo.visibility = View.GONE
            likeUserThree.visibility = View.GONE
            likeUserFour.visibility = View.GONE
            likeUserFive.visibility = View.GONE
            listLikeUsers.visibility = View.GONE
//            groupLike.visibility = View.GONE
            //TODO(Проверить, можно ли заменить весь код одной строчкой)
            mentionedOne.visibility = View.GONE
            mentionedTwo.visibility = View.GONE
            mentionedThree.visibility = View.GONE
            mentionedFour.visibility = View.GONE
            mentionedFive.visibility = View.GONE
            listMentioned.visibility = View.GONE
//            groupMentioned.visibility = View.GONE

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
            if (post.link != null) link.text = post.link else link.visibility = View.GONE

            //TODO(Проверить адрес url)
            val url = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"
            val urlAttachment = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"
            val options = RequestOptions()

            Glide.with(binding.avatar)
                .load(url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(binding.avatar)

            if (post.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(binding.imageContent)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.imageContent)
            }

            if (post.coords != null) {
                map.visibility = View.VISIBLE
            }

            if (post.attachment?.type == AttachmentType.VIDEO) {
                groupVideo.visibility = View.VISIBLE
                //TODO(Добавить отображение видео)
            }

            if (post.attachment?.type == AttachmentType.AUDIO) {
                groupSong.visibility = View.VISIBLE
                //TODO(Добавить отображение звука)
            }

            if (!post.likeOwnerIds.isEmpty()) {
                numberUsers = 0

                //TODO(Доделать код, добавить загрузку аватарок)
                post.likeOwnerIds.forEach {
                    when (numberUsers) {
                        0 -> {
                            likeUserOne.visibility = View.VISIBLE
                            Glide.with(binding.likeUserOne)
                                .load(url)
                                .error(R.drawable.ic_error_24)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(binding.likeUserOne)
                            post.users[it]?.avatar
                        }

                        1 -> {
                            likeUserTwo.visibility = View.VISIBLE

                        }

                        2 -> {
                            likeUserThree.visibility = View.VISIBLE

                        }

                        3 -> {
                            likeUserFour.visibility = View.VISIBLE

                        }

                        4 -> {
                            likeUserFive.visibility = View.VISIBLE
                            listLikeUsers.visibility = View.VISIBLE

                        }
                    }
                    numberUsers++
                }
            }

            if (!post.mentionIds.isEmpty()) {

            }

//            if (post.attachment == null) {
//                imageContent.visibility = View.GONE
//            }
//
//            when {
//                post.attachment == null -> imageContent.visibility = View.GONE
//                post.attachment.uri == null -> Glide.with(binding.imageContent)
//                    .load(urlAttachment)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .into(binding.imageContent)
//                else -> imageContent.setImageURI(post.attachment.uri.toUri())
//            }

//            if (post.video == null) {
//                groupVideo.visibility = View.GONE
//            }
//
//            if (post.content == null) {
//                content.visibility = View.GONE
//            }

//            if (post.authorAvatar != "netology") {
//                Glide.with(binding.avatar)
//                    .load(url)
//                    .error(R.drawable.ic_error_24)
//                    .timeout(10_000)
//                    .apply(options.circleCrop())
//                    .into(binding.avatar)
//            } else {
//                avatar.setImageResource(R.drawable.ic_netology)
//            }

//            if (post.savedOnTheServer) {
//                saved.setImageResource(R.drawable.ic_checked_24)
//            }
        }
    }

    private fun moveToMarker(
        yandexMap: Map,
        target: Point
    ) {
        val currentPosition = yandexMap.cameraPosition
        yandexMap.move(
            CameraPosition(
                target, 15F, currentPosition.azimuth, currentPosition.tilt,
            ),
            smoothAnimation,
            null,
        )
    }

    private fun addMarker(yandexMap: Map, target: Point) {
        val imageProvider =
            DrawableImageProvider(requireContext(), ImageInfo(R.drawable.ic_location_pin_48, RED))

        placemarkMapObject = yandexMap.mapObjects.addPlacemark {
            it.setIcon(imageProvider)
            it.geometry = target
            it.setText(context?.getString(R.string.place_work) ?: "")
//            it.addTapListener(placemarkTapListener)
//            it.userData = context?.getString(R.string.place_work) ?: ""
        }
    }

    private fun subscribeToLifecycle(mapView: MapView) {
        viewLifecycleOwner.lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            mapKit.onStart()
                            mapView.onStart()
                        }

                        Lifecycle.Event.ON_STOP -> {
                            mapView.onStop()
                            mapKit.onStop()
                        }

                        Lifecycle.Event.ON_DESTROY -> source.lifecycle.removeObserver(this)

                        else -> Unit
                    }
                }
            }
        )
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