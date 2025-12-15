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
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.dao.PostDao
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.fragment.AddLocationFragment.Companion.POST
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {
    companion object {
        const val NEW_POST = "newPost"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.statusPostAndContent by StringArg
    }

    @Inject
    lateinit var dao: PostDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        val dialog = BottomSheetDialog(requireContext())
        val viewModel: PostViewModel by activityViewModels()

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.statusPostAndContent?.let {
            val text = it
            if (text == NEW_POST) {
                lifecycleScope.launch {
                    if (dao.getDraft() != null) {
                        binding.content.setText(dao.getDraft())
                        dao.removeDraft()
                    }
                }
            } else {
                binding.content.setText(text)
                editing = true
            }
            arguments?.statusPostAndContent = null
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
                //TODO(Проверить поведение)
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
            }

            back.setOnClickListener {
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
                findNavController().navigateUp()
            }

            choosePhoto.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_photo_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickPhoto -> {
                                ImagePicker.with(this@NewPostFragment)
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
                                ImagePicker.with(this@NewPostFragment)
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
                    R.id.action_newPostFragment_to_userFragment,
                    Bundle().apply {
                        statusUserFragment = CHOOSING_MENTIONED_USER
                    }
                )
            }

            addLocation.setOnClickListener {
                findNavController().navigate(
                    R.id.action_newPostFragment_to_addLocationFragment,
                    Bundle().apply {
                        statusAddLocationFragment = POST
                    }
                )
            }

            removePhoto.setOnClickListener {
                viewModel.changeMedia(null, null, AttachmentType.IMAGE)
            }
        }

        //TODO(Нужно ли менять liveData)
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

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки)
//        viewModel.dataState.observe(viewLifecycleOwner) {
//            if (it.errorCode300) {
//                findNavController().navigateUp()
//            }
//        }

        //TODO(Нужно ли менять liveData)
        viewModel.errorPost403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.teed_to_log, Toast.LENGTH_SHORT).show()
        }

        //TODO(Нужно ли менять liveData)
        viewModel.errorPost404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        //TODO(Нужно ли менять liveData)
        viewModel.errorPost415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT).show()
        }

//        bindingErrorCode400And500.errorCode400And500.setOnClickListener {
//            dialog.dismiss()
//        }

//        binding.cancel.setOnClickListener {
//            viewModel.edited.value = viewModel.empty
//            findNavController().navigateUp()
//        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!isEmpty(binding.content.text.toString()) && !editing) {
                lifecycleScope.launch {
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

