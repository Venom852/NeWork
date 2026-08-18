package ru.netology.nework.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color.RED
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toFile
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
import ru.netology.nework.dao.UserDao
import ru.netology.nework.databinding.AuthorizationDialogBoxBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.extensions.DrawableImageProvider
import ru.netology.nework.extensions.ImageInfo
import ru.netology.nework.fragment.NewPostFragment.Companion.EDITING_NEW_POST
import ru.netology.nework.fragment.NewPostFragment.Companion.newPostFragmentBundle
import ru.netology.nework.fragment.NewPostFragment.Companion.statusFragment
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.PhotoFragment.Companion.POST
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.postFragmentBundle
import ru.netology.nework.fragment.UserFragment.Companion.LIKE
import ru.netology.nework.fragment.UserFragment.Companion.MENTIONED
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.fragment.UserFragment.Companion.userBundleFragment
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.UserViewModel
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.collections.emptyMap
import kotlin.getValue

@AndroidEntryPoint
@SuppressLint("SetTextI18n")
class PostFragment : Fragment() {
    @Inject
    lateinit var postDao: PostDao
    @Inject
    lateinit var userDao: UserDao

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
        published = "",
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
        users = emptyMap(),
        playSong = false,
        playVideo = false
    )
    private var user = User(
        id = 0,
        name = "",
        login = "",
        avatar = null
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
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)

        val viewModel: PostViewModel by activityViewModels()
        val viewModelUser: UserViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated
        var listUsers = ""

        arguments?.postBundle?.let {
            post = gson.fromJson(it, Post::class.java)
            postId = post.id
            arguments?.postBundle = null
        }

        with(binding) {
            setValues(this, post)

            //TODO(Настроить поведение, чтобы кнопка не кликалась если нет авторизации)
            like.setOnClickListener {
                if (authorization) {
                    viewModel.likeById(post)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            toShare.setOnClickListener {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            //TODO(Настроить)
            avatar.setOnClickListener {
                findNavController().navigate(
                    R.id.action_postFragment2_to_yourProfileFragment,
                    Bundle().apply {
                        if (post.ownedByMe) {
                            statusProfileFragment = YOUR
                            postFragmentBundle = gson.toJson(post.authorId)
                        } else {
                            statusProfileFragment = USER
                            postFragmentBundle = gson.toJson(post.authorId)
                        }
                    }
                )
            }

            //TODO(Настроить)
//            groupVideo.setAllOnClickListener {
//                if (!post.playSong) {
//                    viewModel.playVideo(post)
//                } else {
//                    viewModel.pauseVideo()
//                }
//
//                viewModel.playButtonVideo(post.id)
//            }

            //TODO(Настроить)
//            playSong.setOnClickListener {
//                if (!post.playSong) {
//                    viewModel.playSong(post)
//                } else {
//                    viewModel.pauseSong()
//                }
//
//                viewModel.playButtonSong(post.id)
//            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, "http://${post.link}".toUri())
                it.context.startActivity(intent)
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            //TODO(Настроить)
            listLikeUsers.setOnClickListener {
//                post.likeOwnerIds.forEach {
//                    if (listUsers.isEmpty()) {
//                        listUsers = it.toString()
//                    }
//
//                    listUsers = "$listUsers,$it"
//                }

                viewModelUser.saveUsers(post.likeOwnerIds)

                findNavController().navigate(
                    R.id.action_postFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = LIKE
//                        userBundleFragment = listUsers
                    }
                )
            }

            //TODO(Настроить)
            listMentionedUsers.setOnClickListener {
//                post.mentionIds.forEach {
//                    if (listUsers.isEmpty()) {
//                        listUsers = it.toString()
//                    }
//
//                    listUsers = "$listUsers,$it"
//                }

                viewModelUser.saveUsers(post.mentionIds)

                findNavController().navigate(
                    R.id.action_postFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = MENTIONED
//                        userBundleFragment = gson.toJson(listUsers)
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
                                        newPostFragmentBundle = post.content
                                        statusFragment = EDITING_NEW_POST
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
                        CoroutineScope(Dispatchers.IO).launch {
                            post = postDao.getPost(postId).toPostDto()
                        }

                        setValues(this@with, post)
                    }
                }
            }

            //TODO(Настроить)
            imageContent.setOnClickListener {
                Navigation.findNavController(it).navigate(
                    R.id.action_postFragment2_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(post)
                        statusPhotoFragment = POST
                    }
                )
            }

            viewModel.errorPost403.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
            }

            viewModel.errorPost404.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
            }

            viewModel.errorPost415.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                    .show()
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
            val zonedDateTime = ZonedDateTime.parse(post.published)
            val options = RequestOptions()

            author.text = post.author
            authorJob.text = post.authorJob
            content.text = post.content
            link.text = post.link
            like.text = CountCalculator.calculator(post.likes)
            mention.text = CountCalculator.calculator(post.mentionIds.count().toLong())
            published.text =
                "${zonedDateTime.dayOfMonth}.${zonedDateTime.monthValue}.${zonedDateTime.year} ${zonedDateTime.hour}:${zonedDateTime.minute}"

            playSong.isChecked = post.playSong
            playVideo.isChecked = post.playVideo
            like.isChecked = post.likedByMe
            toShare.isChecked = post.toShare

            map.visibility = View.GONE
            imageContent.visibility = View.GONE
            link.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            groupLike.visibility = View.GONE
            groupMentioned.visibility = View.GONE
            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            Glide.with(avatar)
                .load(post.authorAvatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(avatar)

            if (post.authorAvatar == null || post.author == "Me") {
                avatar.setImageResource(R.drawable.ic_netology)
            }

            if (post.link != null) {
                link.visibility = View.VISIBLE
            }

            if (post.coords != null) {
                map.visibility = View.VISIBLE
            }

            if (post.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(imageContent)
                    .load(post.attachment.url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(imageContent)
            }

            //TODO(Настроить)
//            if (post.attachment?.type == AttachmentType.VIDEO) {
//                groupVideo.visibility = View.VISIBLE
//
//                videoContent.setVideoURI(post.attachment.url.toUri())
//            }

            //TODO(Настроить)
//            if (post.attachment?.type == AttachmentType.AUDIO) {
//                groupSong.visibility = View.VISIBLE
//
//                val songFile = post.attachment.url.toUri().toFile()
//
//                val retriever = MediaMetadataRetriever()
//                retriever.setDataSource(songFile.absolutePath)
//                val durationStr =
//                    retriever.extractMetadata(
//                        MediaMetadataRetriever.METADATA_KEY_DURATION
//                    )
//                val duration = durationStr?.toIntOrNull() ?: 0
//                val title = retriever.extractMetadata(
//                    MediaMetadataRetriever.METADATA_KEY_TITLE
//                ) ?: "noName"
//                retriever.release()
//
//                titleSong.text = title
//                timeSong.text = duration.toString()
//            }

            if (!post.likeOwnerIds.isEmpty()) {
                numberUsers = 0

                post.likeOwnerIds.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        user = userDao.getUser(it).toUserDto()
                    }

                    when (numberUsers) {
                        0 -> {
                            likeUserOne.visibility = View.VISIBLE

                            Glide.with(likeUserOne)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserOne)

                            ++numberUsers
                        }

                        1 -> {
                            likeUserTwo.visibility = View.VISIBLE

                            Glide.with(likeUserTwo)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserTwo)

                            ++numberUsers
                        }

                        2 -> {
                            likeUserThree.visibility = View.VISIBLE

                            Glide.with(likeUserThree)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserThree)

                            ++numberUsers
                        }

                        3 -> {
                            likeUserFour.visibility = View.VISIBLE

                            Glide.with(likeUserFour)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserFour)

                            ++numberUsers
                        }

                        4 -> {
                            likeUserFive.visibility = View.VISIBLE
                            listLikeUsers.visibility = View.VISIBLE

                            Glide.with(likeUserFive)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserFive)

                            ++numberUsers
                        }

                        else -> return@forEach
                    }
                }
            }

            if (!post.mentionIds.isEmpty()) {
                numberUsers = 0

                post.mentionIds.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        user = userDao.getUser(it).toUserDto()
                    }

                    when (numberUsers) {
                        0 -> {
                            mentionedText.visibility = View.VISIBLE
                            mention.visibility = View.VISIBLE
                            mentionedOne.visibility = View.VISIBLE

                            Glide.with(mentionedOne)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(mentionedOne)

                            ++numberUsers
                        }

                        1 -> {
                            mentionedTwo.visibility = View.VISIBLE

                            Glide.with(mentionedTwo)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(mentionedTwo)

                            ++numberUsers
                        }

                        2 -> {
                            mentionedThree.visibility = View.VISIBLE

                            Glide.with(mentionedThree)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(mentionedThree)

                            ++numberUsers
                        }

                        3 -> {
                            mentionedFour.visibility = View.VISIBLE

                            Glide.with(mentionedFour)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(mentionedFour)

                            ++numberUsers
                        }

                        4 -> {
                            mentionedFive.visibility = View.VISIBLE
                            listMentionedUsers.visibility = View.VISIBLE

                            Glide.with(mentionedFive)
                                .load(user.avatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(mentionedFive)

                            ++numberUsers
                        }

                        else -> return@forEach
                    }
                }
            }
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
}