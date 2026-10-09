package ru.netology.nework.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils.isEmpty
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
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.fragment.AddLocationFragment.Companion.EVENT
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class NewEventFragment : Fragment() {
    @Inject
    lateinit var contentDraftDao: ContentDraftDao

    companion object {
        const val NEW_EVENT = "newEvent"
        private var editing = false
        var Bundle.newEventFragmentBundle by StringArg
        var Bundle.statusEventFragment by StringArg
    }

    private var status = ""

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

        arguments?.newEventFragmentBundle?.let {
            binding.content.setText(it)
            editing = true
            arguments?.newEventFragmentBundle = null
        }

        arguments?.statusEventFragment?.let {
            status = it
            if (status == NEW_EVENT) {
                lifecycleScope.launch {
                    if (contentDraftDao.getDraft() != null) {
                        binding.content.setText(contentDraftDao.getDraft())
                        contentDraftDao.removeDraft()
                    }
                }
            }
            arguments?.statusEventFragment = null
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

                        Toast.makeText(requireContext(), R.string.photo_added, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        val pickVideo =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    val tempFile = File(
                        context?.cacheDir,
                        "upload_${System.currentTimeMillis()}.tmp"
                    )

                    context?.contentResolver?.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val fileUri = Uri.fromFile(tempFile)
                    tempFile.delete()

                    viewModel.changeMedia(fileUri, fileUri.toFile(), AttachmentType.VIDEO)

                    Toast.makeText(requireContext(), R.string.video_added, Toast.LENGTH_SHORT)
                        .show()
                }
            }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val audio =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        if (it.data?.data != null) {
                            val uri = it.data?.data

                            if (uri != null) {
                                val tempFile = File(
                                    context?.cacheDir,
                                    "upload_${System.currentTimeMillis()}.tmp"
                                )

                                context?.contentResolver?.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                val fileUri = Uri.fromFile(tempFile)
                                tempFile.delete()

                                viewModel.changeMedia(fileUri, fileUri.toFile(), AttachmentType.AUDIO)

                                Toast.makeText(requireContext(), R.string.audio_added, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }

        with(binding) {
            content.requestFocus()

            groupPhotoContainer.visibility = View.GONE

            save.setOnClickListener {
                if (!content.text.isNullOrBlank()) {
                    viewModel.saveContent(content.text.toString())

                    AndroidUtils.hideKeyboard(requireView())
                } else {
                    Toast.makeText(requireContext(), R.string.empty_event, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            back.setOnClickListener {
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
                viewModel.listSpeakersUsers = emptySet()
                viewModel.listMapUsers = emptyMap()
                viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
                viewModel.dateTime = ""
                viewModel.type = EventType.NOT_ASSIGNED
                viewModel.statusMedia = false

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
            if (it.uri == null) {
                binding.groupPhotoContainer.visibility = View.GONE
                return@observe
            }

            if (it.attachmentType == AttachmentType.IMAGE) {
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

                    if (dateContent.length in 14..16) {
                        dateContent = "$dateContent:00.000Z"

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

        bindingCardCalendar.calendarView.setOnDateChangeListener { _, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            date = "$month/$day/$year"

            var monthString = month.toString()
            var dayString = day.toString()

            when {
                monthString.length != 2 && dayString.length != 2 -> {
                    monthString = "0$monthString"
                    dayString = "0$dayString"
                    dateEvent = "$year-$monthString-$dayString"
                }

                monthString.length != 2 -> {
                    monthString = "0$monthString"
                    dateEvent = "$year-$monthString-$dayString"
                }

                dayString.length != 2 -> {
                    dayString = "0$dayString"
                    dateEvent = "$year-$monthString-$dayString"
                }

                else -> dateEvent = "$year-$monthString-$dayString"
            }

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
            }
            editing = false
            viewModel.edited.value = viewModel.empty
            viewModel.changeMedia(null, null, null)
            viewModel.listSpeakersUsers = emptySet()
            viewModel.listMapUsers = emptyMap()
            viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
            viewModel.dateTime = ""
            viewModel.type = EventType.NOT_ASSIGNED
            viewModel.statusMedia = false

            findNavController().navigateUp()
        }

        callback.isEnabled
        return binding.root
    }
}

