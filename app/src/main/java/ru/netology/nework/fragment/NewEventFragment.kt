package ru.netology.nework.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.EventViewModel
import kotlinx.coroutines.launch
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.dao.ContentDraftDao
import ru.netology.nework.databinding.CardCalendarBinding
import ru.netology.nework.databinding.FragmentNewEventBinding
import ru.netology.nework.databinding.SelectDateEventBinding
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.fragment.AddLocationFragment.Companion.EVENT
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class NewEventFragment : Fragment() {
    @Inject
    lateinit var contentDraftDao: ContentDraftDao

    companion object {
        const val NEW_EVENT_KEY = "newEvent"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.statusEventAndContent by StringArg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewEventBinding.inflate(layoutInflater, container, false)
        val bindingSelectDateEvent =
            SelectDateEventBinding.inflate(layoutInflater, container, false)
        val bindingCardCalendar =
            CardCalendarBinding.inflate(layoutInflater, container, false)

        val dialog = BottomSheetDialog(requireContext())
        val dialogCalendar = BottomSheetDialog(requireContext())
        val viewModel: EventViewModel by activityViewModels()
        var date = ""
        var dateEvent = ""

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.statusEventAndContent?.let {
            val text = it
            if (text == NEW_EVENT_KEY) {
                lifecycleScope.launch {
                    if (contentDraftDao.getDraft() != null) {
                        binding.content.setText(contentDraftDao.getDraft())
                        contentDraftDao.removeDraft()
                    }
                }
            } else {
                binding.content.setText(text)
                editing = true
            }
            arguments?.statusEventAndContent = null
        }

        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)
                    }
                }
            }

        val pickAudio =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("Audio", "Selected URI: $uri")
                    viewModel.changeMedia(uri, uri.toFile(), AttachmentType.AUDIO)
                } else {
                    Log.d("Audio", "No media selected")
                }
            }

        val pickVideo =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("Video", "Selected URI: $uri")
                    viewModel.changeMedia(uri, uri.toFile(), AttachmentType.VIDEO)
                } else {
                    Log.d("Video", "No media selected")
                }
            }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val audio =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        if (it.data?.data != null) {
                            val uri = it.data?.data
                            Log.d("Audio", "Selected URI: $uri")
                            viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.AUDIO)
                        } else {
                            Log.d("Audio", "No media selected")
                        }
                    }
                }
            }

        with(binding) {
            content.requestFocus()

            save.setOnClickListener {
                if (!content.text.isNullOrBlank()) {
                    viewModel.saveContent(content.text.toString())
                    AndroidUtils.hideKeyboard(requireView())
                }
                //TODO(Проверить поведение)
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
            }

            back.setOnClickListener {
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
                viewModel.listSpeakersUsers = emptySet<Long>()
                viewModel.listMapUsers = emptyMap<Long, UserPreview>()
                viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
                viewModel.dateTime = ""
                viewModel.type = EventType.NOT_ASSIGNED

                findNavController().navigateUp()
            }

            choosePhoto.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_photo_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickPhoto -> {
                                ImagePicker.with(this@NewEventFragment)
                                    .crop()
                                    .compress(2048)
                                    .provider(ImageProvider.GALLERY)
                                    .galleryMimeTypes(
                                        arrayOf(
                                            "image/png",
                                            "image/jpeg",
                                        )
                                    )
                                    .createIntent(pickPhotoLauncher::launch)

                                true
                            }

                            R.id.takePhoto -> {
                                ImagePicker.with(this@NewEventFragment)
                                    .crop()
                                    .compress(2048)
                                    .provider(ImageProvider.CAMERA)
                                    .createIntent(pickPhotoLauncher::launch)

                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            attachMedia.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_audio_or_video)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickAudio -> {
                                audio.launch(intent)
//                                pickAudio.launch(
//                                    PickVisualMediaRequest(
//                                        ActivityResultContracts.PickVisualMedia.VideoOnly
//                                    )
//                                )

                                true
                            }

                            R.id.pickVideo -> {
                                pickVideo.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )

                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            chooseUsers.setOnClickListener {
                findNavController().navigate(
                    R.id.action_newEventFragment_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = CHOOSING_SPEAKERS_USER
                    }
                )
            }

            addLocation.setOnClickListener {
                findNavController().navigate(
                    R.id.action_newEventFragment_to_addLocationFragment,
                    Bundle().apply {
                        statusAddLocationFragment = EVENT
                    }
                )
            }

            addDate.setOnClickListener {
                dialog.setCancelable(false)
                dialog.setContentView(bindingSelectDateEvent.root)
                dialog.show()
            }

            removePhoto.setOnClickListener {
                viewModel.changeMedia(null, null, AttachmentType.IMAGE)
            }
        }

        viewModel.media.observe(viewLifecycleOwner) {
            if (it.attachmentType == AttachmentType.IMAGE) {
                if (it.uri == null) {
                    binding.groupPhotoContainer.visibility = View.GONE
                    return@observe
                }

                binding.groupPhotoContainer.visibility = View.VISIBLE
                binding.photo.setImageURI(it.uri)
            }
        }

        viewModel.eventCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.errorEvent403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModel.errorEvent404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.event_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModel.errorEvent415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
        }

        bindingSelectDateEvent.selectDateEvent.detectSwipe {
            val text = when (it) {
                SwipeDirection.Down -> "onSwipeDown"
                SwipeDirection.Left -> "onSwipeLeft"
                SwipeDirection.Right -> "onSwipeRight"
                SwipeDirection.Up -> "onSwipeUp"
            }

            if (text == "onSwipeDown") {
                if (!bindingSelectDateEvent.dateContent.text.isNullOrBlank()) {
                    var dateContent =
                        bindingSelectDateEvent.dateContent.text.toString().replace(date, dateEvent)
                            .replace(" ", "T")

                    if (dateContent.length == 16) {
                        dateContent = "$dateContent:00.123Z"

                        if (bindingSelectDateEvent.online.isChecked) {
                            viewModel.saveDate(dateContent, EventType.ONLINE)
                        } else {
                            viewModel.saveDate(dateContent, EventType.OFFLINE)
                        }

                        dialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.date_is_set),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        bindingSelectDateEvent.date.error = getString(R.string.enter_date)
                        bindingSelectDateEvent.date.error = null

                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.incorrect_date_and_time),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    dialog.dismiss()
                }
            }
        }

        bindingSelectDateEvent.date.setEndIconOnClickListener {
            dialogCalendar.setCancelable(false)
            dialogCalendar.setContentView(bindingCardCalendar.root)
            dialogCalendar.show()
        }

        bindingCardCalendar.calendarView.setOnDateChangeListener { view, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            date = "$month/$day/$year"
            dateEvent = "$year-$month-$day"

            bindingSelectDateEvent.dateContent.setText(date)
            dialogCalendar.dismiss()
        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!isEmpty(binding.content.text.toString()) && !editing) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        contentDraftDao.saveDraft(binding.content.text.toString())
                    }
                }
//                lifecycleScope.launch {
//                    contentDraftDao.saveDraft(binding.content.text.toString())
//                }
            }
            editing = false
            viewModel.edited.value = viewModel.empty
            findNavController().navigateUp()
        }

        callback.isEnabled
        return binding.root
    }
}

