package ru.netology.nework.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.dao.PostDao
import ru.netology.nework.databinding.CardCalendarBinding
import ru.netology.nework.databinding.FragmentNewEventBinding
import ru.netology.nework.databinding.SelectDateEventBinding
import ru.netology.nework.fragment.AddLocationFragment.Companion.EVENT
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import java.util.Calendar
import javax.inject.Inject

//TODO(Поменять везде viewModel)
@AndroidEntryPoint
class NewEventFragment : Fragment() {
    companion object {
        const val NEW_EVENT_KEY = "newEvent"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.statusEventAndContent by StringArg
    }

    @Inject
    lateinit var dao: PostDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewEventBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)
        val bindingSelectDateEvent =
            SelectDateEventBinding.inflate(layoutInflater, container, false)
        val bindingCardCalendar =
            CardCalendarBinding.inflate(layoutInflater, container, false)

        val dialog = BottomSheetDialog(requireContext())
        val dialogCalendar = BottomSheetDialog(requireContext())
        //TODO(Заменить viewModel)
        val viewModel: PostViewModel by activityViewModels()

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.statusEventAndContent?.let {
            val text = it
            if (text == NEW_EVENT_KEY) {
                lifecycleScope.launch {
                    //TODO(Поменять базу данных)
                    if (dao.getDraft() != null) {
                        binding.content.setText(dao.getDraft())
                        dao.removeDraft()
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
                        viewModel.changeMedia(uri, uri?.toFile())
                    }
                }
            }

        val pickAudio =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")
                    //TODO(Добавить viewModel)
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

        val pickVideo =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: $uri")
                    //TODO(Добавить viewModel)
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

//        binding.pickPhoto.setOnClickListener {
//            ImagePicker.with(this)
//                .crop()
//                .compress(2048)
//                .provider(ImageProvider.GALLERY)
//                .galleryMimeTypes(
//                    arrayOf(
//                        "image/png",
//                        "image/jpeg",
//                    )
//                )
//                .createIntent(pickPhotoLauncher::launch)
//        }
//
//        binding.takePhoto.setOnClickListener {
//            ImagePicker.with(this)
//                .crop()
//                .compress(2048)
//                .provider(ImageProvider.CAMERA)
//                .createIntent(pickPhotoLauncher::launch)
//        }

//        requireActivity().addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                menuInflater.inflate(R.menu.menu_new_post, menu)
//            }
//
//            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
//                when (menuItem.itemId) {
//                    R.id.save -> {
//                        binding.let {
//                            if (!binding.content.text.isNullOrBlank()) {
//                                viewModel.saveContent(it.content.text.toString())
//                                AndroidUtils.hideKeyboard(requireView())
//                            }
//                            viewModel.edited.value = viewModel.empty
//                        }
//                        true
//                    }
//
//                    else -> false
//                }
//
//        }, viewLifecycleOwner)

        with(binding) {
            content.requestFocus()

            save.setOnClickListener {
                if (!content.text.isNullOrBlank()) {
                    viewModel.saveContent(content.text.toString())
                    AndroidUtils.hideKeyboard(requireView())
                }
                viewModel.edited.value = viewModel.empty
                //TODO(Добавить обнуление состояний)
            }

            back.setOnClickListener {
                viewModel.edited.value = viewModel.empty
                //TODO(Добавить обнуление состояний)
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
                //TODO(Попробовать оба варианта)
//                val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
//                pickPhotoLauncher.launch(intent)

                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_audio_or_video)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickAudio -> {
                                pickAudio.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )

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
                viewModel.changeMedia(null, null)
            }
        }

        //TODO(Нужно ли менять liveData)
        viewModel.media.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.groupPhotoContainer.visibility = View.GONE
                return@observe
            }

            binding.groupPhotoContainer.visibility = View.VISIBLE
            binding.photo.setImageURI(it.uri)
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
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

        bindingErrorCode400And500.errorCode400And500.detectSwipe {
            val text = when (it) {
                SwipeDirection.Down -> "onSwipeDown"
                SwipeDirection.Left -> "onSwipeLeft"
                SwipeDirection.Right -> "onSwipeRight"
                SwipeDirection.Up -> "onSwipeUp"
            }

            if (text == "onSwipeDown") {
                dialog.dismiss()
            }
        }

        bindingSelectDateEvent.selectDateEvent.detectSwipe {
            val text = when (it) {
                SwipeDirection.Down -> "onSwipeDown"
                SwipeDirection.Left -> "onSwipeLeft"
                SwipeDirection.Right -> "onSwipeRight"
                SwipeDirection.Up -> "onSwipeUp"
            }
            //TODO(Добавить viewModel)
//            viewModel.saveDate(dateContent.text.toString())
//
//            if (bindingSelectDateEvent.online.isChecked) {
//                viewModel.saveType(EventType.ONLINE)
//            } else {
//                viewModel.saveType(EventType.OFFLINE)
//            }

            if (text == "onSwipeDown") {
                dialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.date_is_set),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        bindingSelectDateEvent.date.setEndIconOnClickListener {
            dialogCalendar.setCancelable(false)
            dialogCalendar.setContentView(bindingCardCalendar.root)
            dialogCalendar.show()
        }

        //TODO(Подумать как правильно конвертировать дату)
        bindingCardCalendar.calendarView.setOnDateChangeListener { view, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            val date = "$month/$day/$year"
            bindingSelectDateEvent.dateContent.setText(date)
            dialogCalendar.dismiss()
        }

//        binding.cancel.setOnClickListener {
//            viewModel.edited.value = viewModel.empty
//            findNavController().navigateUp()
//        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!isEmpty(binding.content.text.toString()) && !editing) {
                lifecycleScope.launch {
                    //TODO(Заменить базу данных)
                    dao.saveDraft(binding.content.text.toString())
                }
            }
            editing = false
            viewModel.edited.value = viewModel.empty
            findNavController().navigateUp()
        }

        callback.isEnabled
        return binding.root
    }
}

