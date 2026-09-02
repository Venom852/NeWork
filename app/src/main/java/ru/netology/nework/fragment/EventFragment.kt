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
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.databinding.AuthorizationDialogBoxBinding
import ru.netology.nework.databinding.FragmentEventBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.extensions.DrawableImageProvider
import ru.netology.nework.extensions.ImageInfo
import ru.netology.nework.fragment.NewEventFragment.Companion.statusEventAndContent
import ru.netology.nework.fragment.PhotoFragment.Companion.EVENT
import ru.netology.nework.fragment.PhotoFragment.Companion.photoBundle
import ru.netology.nework.fragment.PhotoFragment.Companion.statusPhotoFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.PROHIBIT
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.eventFragmentBundle
import ru.netology.nework.fragment.ProfileFragment.Companion.statusPermissionToCross
import ru.netology.nework.fragment.UserFragment.Companion.LIKE
import ru.netology.nework.fragment.UserFragment.Companion.PARTICIPANTS
import ru.netology.nework.fragment.UserFragment.Companion.SPEAKERS
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.fragment.UserFragment.Companion.userBundleFragment
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener
import ru.netology.nework.util.CountCalculator
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.PostUserWallViewModel
import ru.netology.nework.viewmodel.UserViewModel
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.collections.emptyMap
import kotlin.getValue

@AndroidEntryPoint
@SuppressLint("SetTextI18n")
class EventFragment : Fragment() {
    @Inject
    lateinit var eventDao: EventDao

    @Inject
    lateinit var userDao: UserDao

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
        published = "",
        datetime = "",
        type = null,
        link = null,
        likedByMe = false,
        toShare = false,
        likes = 0,
        participants = 0,
        numberViews = 0,
        attachment = null,
        shared = 0,
        ownedByMe = false,
        speakerIds = emptySet(),
        coords = null,
        participatedByMe = false,
        likeOwnerIds = emptySet(),
        participantsIds = emptySet(),
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
        val bindingAuthorizationDialogBox =
            AuthorizationDialogBoxBinding.inflate(layoutInflater, container, false)

        val viewModel: EventViewModel by activityViewModels()
        val viewModelUser: UserViewModel by activityViewModels()
        val viewModelAuth: AuthViewModel by viewModels()
        val viewModelPostUserWall: PostUserWallViewModel by activityViewModels()

        val dialog = BottomSheetDialog(requireContext())
        val authorization = viewModelAuth.authenticated
        var listUsers = ""

        arguments?.eventBundle?.let {
            event = gson.fromJson(it, Event::class.java)
            eventId = event.id
            arguments?.eventBundle = null
        }

        with(binding) {
            setValues(this, event)

            //TODO(Настроить поведение, чтобы кнопка не кликалась если нет авторизации)
            like.setOnClickListener {
                if (authorization) {
                    viewModel.likeById(event)
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
                    putExtra(Intent.EXTRA_TEXT, event.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            //TODO(Настроить поведение, чтобы кнопка не кликалась если нет авторизации)
            participate.setOnClickListener {
                if (authorization) {
                    viewModel.participateById(event)
                } else {
                    dialog.setCancelable(false)
                    dialog.setContentView(bindingAuthorizationDialogBox.root)
                    dialog.show()
                }
            }

            //TODO(Настроить)
            avatar.setOnClickListener {
                viewModelPostUserWall.saveAuthorId(event.authorId)

                findNavController().navigate(
                    R.id.action_eventFragment2_to_yourProfileFragment,
                    Bundle().apply {
                        statusPermissionToCross = PROHIBIT

                        if (event.ownedByMe) {
                            statusProfileFragment = YOUR
                        } else {
                            statusProfileFragment = USER
//                            eventFragmentBundle = gson.toJson(event.authorId)
                        }
                    }
                )
            }

            //TODO(Настроить)
//            groupVideo.setAllOnClickListener {
//                if (!event.playSong) {
//                    viewModel.playVideo(event)
//                } else {
//                    viewModel.pauseVideo()
//                }
//
//                viewModel.playButtonVideo(event.id)
//            }

            //TODO(Настроить)
//            playSong.setOnClickListener {
//                if (!event.playSong) {
//                    viewModel.playSong(event)
//                } else {
//                    viewModel.pauseSong()
//                }
//
//                viewModel.playButtonSong(event.id)
//            }

            link.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, "http://${event.link}".toUri())
                it.context.startActivity(intent)
            }

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            //TODO(Настроить)
            listSpeakers.setOnClickListener {
//                event.speakerIds.forEach {
//                    listUsers = "$listUsers$it,"
//                }

                viewModelUser.saveUsers(event.speakerIds)

                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = SPEAKERS
//                        userBundleFragment = gson.toJson(listUsers)
                    }
                )
            }

            //TODO(Настроить)
            listLikeUsers.setOnClickListener {
//                event.likeOwnerIds.forEach {
//                    listUsers = "$listUsers$it,"
//                }

                viewModelUser.saveUsers(event.likeOwnerIds)

                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = LIKE
//                        userBundleFragment = gson.toJson(listUsers)
                    }
                )
            }

            //TODO(Настроить)
            listParticipants.setOnClickListener {
//                event.participantsIds.forEach {
//                    listUsers = "$listUsers$it,"
//                }

                viewModelUser.saveUsers(event.participantsIds)

                findNavController().navigate(
                    R.id.action_eventFragment2_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = PARTICIPANTS
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
                    viewModel.dataEvent.collectLatest {
                        CoroutineScope(Dispatchers.IO).launch {
                            event = eventDao.getEvent(eventId).toEventDto()
                        }

                        setValues(this@with, event)
                    }
                }
            }

            //TODO(Настроить)
            imageContent.setOnClickListener {
                Navigation.findNavController(it).navigate(
                    R.id.action_eventFragment2_to_photoFragment2,
                    Bundle().apply {
                        photoBundle = gson.toJson(event)
                        statusPhotoFragment = EVENT
                    }
                )
            }

            viewModel.errorEvent403.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
            }

            viewModel.errorEvent404.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.event_not_found, Toast.LENGTH_SHORT)
                    .show()
            }

            viewModel.errorEvent415.observe(viewLifecycleOwner) {
                Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                    .show()
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
            val zonedDateTime = ZonedDateTime.parse(event.published)
            val options = RequestOptions()

            author.text = event.author
            authorJob.text = event.authorJob
            content.text = event.content
            link.text = event.link
            like.text = CountCalculator.calculator(event.likes)
            participate.text = CountCalculator.calculator(event.participants)
            eventDate.text =
                "${zonedDateTime.dayOfMonth}.${zonedDateTime.monthValue}.${zonedDateTime.year} ${zonedDateTime.hour}:${zonedDateTime.minute}"

            playSong.isChecked = event.playSong
            playVideo.isChecked = event.playVideo
            like.isChecked = event.likedByMe
            toShare.isChecked = event.toShare

            map.visibility = View.GONE
            imageContent.visibility = View.GONE
            groupVideo.visibility = View.GONE
            groupSong.visibility = View.GONE
            groupSpeakers.visibility = View.GONE
            groupLike.visibility = View.GONE
            groupParticipants.visibility = View.GONE
            menu.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE

            Glide.with(avatar)
                .load(event.authorAvatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(avatar)

            if (event.authorAvatar == null || event.author == "Me") {
                avatar.setImageResource(R.drawable.ic_netology)
            }

            if (event.link != null) {
                link.visibility = View.VISIBLE
            }

            when (event.type) {
                EventType.ONLINE -> eventStatus.text = context?.getString(R.string.online)

                EventType.OFFLINE -> eventStatus.text = context?.getString(R.string.offline)

                else -> eventStatus.visibility = View.GONE
            }

            if (event.coords != null) {
                map.visibility = View.VISIBLE
            }

            if (event.attachment?.type == AttachmentType.IMAGE) {
                imageContent.visibility = View.VISIBLE

                Glide.with(imageContent)
                    .load(event.attachment.url)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(imageContent)
            }

            //TODO(Настроить)
//            if (event.attachment?.type == AttachmentType.VIDEO) {
//                groupVideo.visibility = View.VISIBLE
//
//                videoContent.setVideoURI(event.attachment.url.toUri())
//            }

            //TODO(Настроить)
//            if (event.attachment?.type == AttachmentType.AUDIO) {
//                groupSong.visibility = View.VISIBLE
//
//                val songFile = event.attachment.url.toUri().toFile()
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

            //TODO(Проверить адрес url и работу кода)
            if (!event.speakerIds.isEmpty()) {
                numberUsers = 0

                event.speakerIds.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        user = userDao.getUser(it).toUserDto()
                    }

                    when (numberUsers) {
                        0 -> {
                            speakers.visibility = View.VISIBLE
                            speakersOne.visibility = View.VISIBLE

                            Glide.with(speakersOne)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(speakersOne)

                            ++numberUsers
                        }

                        1 -> {
                            speakersTwo.visibility = View.VISIBLE

                            Glide.with(speakersTwo)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(speakersTwo)

                            ++numberUsers
                        }

                        2 -> {
                            speakersThree.visibility = View.VISIBLE

                            Glide.with(speakersThree)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(speakersThree)

                            ++numberUsers
                        }

                        3 -> {
                            speakersFour.visibility = View.VISIBLE

                            Glide.with(speakersFour)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(speakersFour)

                            ++numberUsers
                        }

                        4 -> {
                            speakersFive.visibility = View.VISIBLE
                            listSpeakers.visibility = View.VISIBLE

                            Glide.with(speakersFive)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(speakersFive)

                            ++numberUsers
                        }

                        else -> return@forEach
                    }
                }
            }

            if (!event.likeOwnerIds.isEmpty()) {
                numberUsers = 0

                event.likeOwnerIds.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        user = userDao.getUser(it).toUserDto()
                    }

                    when (numberUsers) {
                        0 -> {
                            likeUserOne.visibility = View.VISIBLE

                            Glide.with(likeUserOne)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserOne)

                            ++numberUsers
                        }

                        1 -> {
                            likeUserTwo.visibility = View.VISIBLE

                            Glide.with(likeUserTwo)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserTwo)

                            ++numberUsers
                        }

                        2 -> {
                            likeUserThree.visibility = View.VISIBLE

                            Glide.with(likeUserThree)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(likeUserThree)

                            ++numberUsers
                        }

                        3 -> {
                            likeUserFour.visibility = View.VISIBLE

                            Glide.with(likeUserFour)
                                .load(event.authorAvatar)
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
                                .load(event.authorAvatar)
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

            if (!event.participantsIds.isEmpty()) {
                numberUsers = 0

                event.participantsIds.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        user = userDao.getUser(it).toUserDto()
                    }

                    when (numberUsers) {
                        0 -> {
                            participantsOne.visibility = View.VISIBLE

                            Glide.with(participantsOne)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(participantsOne)

                            ++numberUsers
                        }

                        1 -> {
                            participantsTwo.visibility = View.VISIBLE

                            Glide.with(participantsTwo)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(participantsTwo)

                            ++numberUsers
                        }

                        2 -> {
                            participantsThree.visibility = View.VISIBLE

                            Glide.with(participantsThree)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(participantsThree)

                            ++numberUsers
                        }

                        3 -> {
                            participantsFour.visibility = View.VISIBLE

                            Glide.with(participantsFour)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(participantsFour)

                            ++numberUsers
                        }

                        4 -> {
                            participantsFive.visibility = View.VISIBLE
                            listParticipants.visibility = View.VISIBLE

                            Glide.with(participantsFive)
                                .load(event.authorAvatar)
                                .error(R.drawable.ic_netology)
                                .timeout(10_000)
                                .apply(options.circleCrop())
                                .into(participantsFive)

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