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
import ru.netology.nework.databinding.FragmentEventBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.extensions.DrawableImageProvider
import ru.netology.nework.extensions.ImageInfo
import ru.netology.nework.fragment.NewEventFragment.Companion.statusEventAndContent
import ru.netology.nework.fragment.PhotoFragment.Companion.EVENT
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.UserFragment.Companion.LIKE
import ru.netology.nework.fragment.UserFragment.Companion.PARTICIPANTS
import ru.netology.nework.fragment.UserFragment.Companion.SPEAKERS
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

//TODO(Заменить везде viewModel)
@AndroidEntryPoint
class EventFragment : Fragment() {
    //TODO(Заменить на event)
    @Inject
    lateinit var dao: PostDao

    companion object {
        var Bundle.eventBundle by StringArg
    }

    private var event = Event(
        id = 0,
        author = "Me",
        authorId = 0,
        authorAvatar = null,
        authorJob = null,
        content = "",
        published = Instant.now(),
        datetime = Instant.now(),
        type = null,
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        speakerIds = emptySet(),
        coords = null,
        participatedByMe = false,
        likeOwnerIds = emptySet(),
        participantsIds = emptySet(),
        users = emptyMap()
    )
    private val gson = Gson()
    private var eventId = 0L
    private var numberUsers = 0
    private lateinit var yandexMap: Map
    private lateinit var mapKit: MapKit
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var binding: FragmentEventBinding
    private val smoothAnimation = Animation(Animation.Type.SMOOTH, 3F)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEventBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        //TODO(Заменить viewModel)
        val viewModel: PostViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated

        arguments?.eventBundle?.let {
            event = gson.fromJson(it, Event::class.java)
            eventId = event.id
            arguments?.eventBundle = null
        }

        with(binding) {
            setValues(binding, event)

            //TODO(Проверить)
            like.setOnClickListener {
                if (authorization) {
                    viewModel.likeById(event.id)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            toShare.setOnClickListener {
                viewModel.toShareById(event.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, event.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            //TODO(Проверить)
            participate.setOnClickListener {
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
                    R.id.action_eventFragment2_to_yourProfileFragment
                )
            }

            groupVideo.setAllOnClickListener {
                //TODO(Добавить управление видео)
            }

            playSong.setOnClickListener {
                //TODO(Добавить управление звуком)
            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, event.link?.toUri())
                it.context.startActivity(intent)
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            listSpeakers.setOnClickListener {
                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = SPEAKERS
                    }
                )
            }

            listLikeUsers.setOnClickListener {
                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = LIKE
                    }
                )
            }

            listParticipants.setOnClickListener {
                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = PARTICIPANTS
                    }
                )
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.remove -> {
                                viewModel.removeById(event.id)
                                findNavController().navigateUp()
                                true
                            }

                            R.id.edit -> {
                                viewModel.editById(event)
                                findNavController().navigate(
                                    R.id.action_eventFragment2_to_newEventFragment,
                                    Bundle().apply {
                                        statusEventAndContent = event.content
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
                    //TODO(Заменить viewModel)
                    viewModel.dataPost.collectLatest {
                        CoroutineScope(Dispatchers.Default).launch {
                            //TODO(Проверить, нужно ли будет убирать запрос из базы данных)
                            event = dao.getPost(eventId).toDto()
                        }
                        setValues(binding, event)
                    }
                }
            }

            imageContent.setOnClickListener {
                Navigation.findNavController(it).navigate(
                    R.id.action_eventFragment2_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(event)
                        statusPhotoFragment = EVENT
                    }
                )
            }

            //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки)
            viewModel.dataState.observe(viewLifecycleOwner) {
                if (it.errorCode300) {
                    findNavController().navigateUp()
                }
            }

            //TODO(Нужно ли менять liveData)
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
                    R.id.action_eventFragment2_to_signInFragment2
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

        if (event.coords?.lat != null && event.coords?.long != null) {
            addMarker(yandexMap, Point(event.coords!!.lat, event.coords!!.long))
            moveToMarker(yandexMap, Point(event.coords!!.lat, event.coords!!.long))
        }

    }

    private fun setValues(binding: FragmentEventBinding, event: Event) {
        with(binding) {
            author.text = event.author
            authorJob.text = event.authorJob
            content.text = event.content
            eventDate.text = event.published.toString()
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare
            like.text = CountCalculator.calculator(event.likes)
            toShare.text = CountCalculator.calculator(event.shared)

            map.visibility = View.GONE
            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE

            //TODO(Проверить, можно ли заменить весь код одной строчкой)
            speakersOne.visibility = View.GONE
            speakersTwo.visibility = View.GONE
            speakersThree.visibility = View.GONE
            speakersFour.visibility = View.GONE
            speakersFive.visibility = View.GONE
            listSpeakers.visibility = View.GONE
//            groupSpeakers.visibility = View.GONE

            //TODO(Проверить, можно ли заменить весь код одной строчкой)
            likeUserOne.visibility = View.GONE
            likeUserTwo.visibility = View.GONE
            likeUserThree.visibility = View.GONE
            likeUserFour.visibility = View.GONE
            likeUserFive.visibility = View.GONE
            listLikeUsers.visibility = View.GONE
//            groupLike.visibility = View.GONE
            //TODO(Проверить, можно ли заменить весь код одной строчкой)
            participantsOne.visibility = View.GONE
            participantsTwo.visibility = View.GONE
            participantsThree.visibility = View.GONE
            participantsFour.visibility = View.GONE
            participantsFive.visibility = View.GONE
            listParticipants.visibility = View.GONE
//            groupMentioned.visibility = View.GONE

            menu.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE
            if (event.link != null) link.text = event.link else link.visibility = View.GONE

            //TODO(Проверить адрес url)
            val url = "${BuildConfig.BASE_URL}/avatars/${event.authorAvatar}"
            val urlAttachment = "${BuildConfig.BASE_URL}/media/${event.attachment?.url}"
            val options = RequestOptions()

            Glide.with(binding.avatar)
                .load(url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(binding.avatar)

            if (event.type == EventType.ONLINE) {
                eventStatus.text = context?.getString(R.string.online)
            } else {
                eventStatus.text = context?.getString(R.string.offline)
            }

            if (event.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(binding.imageContent)
                    .load(urlAttachment)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.imageContent)
            }

            if (event.coords != null) {
                map.visibility = View.VISIBLE
            }

            if (event.attachment?.type == AttachmentType.VIDEO) {
                groupVideo.visibility = View.VISIBLE
                //TODO(Добавить отображение видео)
            }

            if (event.attachment?.type == AttachmentType.AUDIO) {
                groupSong.visibility = View.VISIBLE
                //TODO(Добавить отображение звука)
            }

            if (!event.likeOwnerIds.isEmpty()) {
                numberUsers = 0

                //TODO(Доделать код, добавить загрузку аватарок)
                event.likeOwnerIds.forEach {
                    when (numberUsers) {
                        0 -> {
                            likeUserOne.visibility = View.VISIBLE
                            Glide.with(binding.likeUserOne)
                                .load(url)
                                .error(R.drawable.ic_error_24)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(binding.likeUserOne)
                            event.users[it]?.avatar
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

            if (!event.participantsIds.isEmpty()) {

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