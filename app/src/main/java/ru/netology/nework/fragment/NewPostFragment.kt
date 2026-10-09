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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.util.StringArg
import ru.netology.nework.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.dao.ContentDraftDao
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.fragment.AddLocationFragment.Companion.POST
import ru.netology.nework.fragment.AddLocationFragment.Companion.WALL
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_WALL
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.viewmodel.PostMyWallViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {
    @Inject
    lateinit var contentDraftDao: ContentDraftDao

    companion object {
        const val NEW_POST = "newPost"
        const val NEW_POST_WALL = "newPostWall"
        const val EDITING_NEW_POST = "editingNewPost"
        const val EDITING_NEW_POST_WALL = "editingNewPostWall"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.newPostFragmentBundle by StringArg
        var Bundle.statusPostFragment by StringArg
    }

    private var status = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)

        val viewModel: PostViewModel by activityViewModels()
        val viewModelMyWall: PostMyWallViewModel by activityViewModels()

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.newPostFragmentBundle?.let {
            binding.content.setText(it)
            editing = true
            arguments?.newPostFragmentBundle = null
        }

        arguments?.statusPostFragment?.let {
            status = it
            if (status == NEW_POST || status == NEW_POST_WALL) {
                lifecycleScope.launch {
                    if (contentDraftDao.getDraft() != null) {
                        binding.content.setText(contentDraftDao.getDraft())
                        contentDraftDao.removeDraft()
                    }
                }
            }
            arguments?.statusPostFragment = null
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

                        if (status == NEW_POST || status == EDITING_NEW_POST) {
                            viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)

                            Toast.makeText(requireContext(), R.string.photo_added, Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            viewModelMyWall.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)

                            Toast.makeText(requireContext(), R.string.photo_added, Toast.LENGTH_SHORT)
                                .show()
                        }
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

                    if (status == NEW_POST || status == EDITING_NEW_POST) {
                        viewModel.changeMedia(fileUri, fileUri.toFile(), AttachmentType.VIDEO)

                        Toast.makeText(requireContext(), R.string.video_added, Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        viewModelMyWall.changeMedia(fileUri, fileUri.toFile(), AttachmentType.VIDEO)

                        Toast.makeText(requireContext(), R.string.video_added, Toast.LENGTH_SHORT)
                            .show()
                    }
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

                                if (status == NEW_POST || status == EDITING_NEW_POST) {
                                    viewModel.changeMedia(fileUri, fileUri?.toFile(), AttachmentType.AUDIO)

                                    Toast.makeText(requireContext(), R.string.audio_added, Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    viewModelMyWall.changeMedia(fileUri, fileUri?.toFile(), AttachmentType.AUDIO)

                                    Toast.makeText(requireContext(), R.string.audio_added, Toast.LENGTH_SHORT)
                                        .show()
                                }
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

                    if (status == NEW_POST || status == EDITING_NEW_POST) {
                        viewModel.saveContent(content.text.toString())
                    } else {
                        viewModelMyWall.saveContent(content.text.toString())
                    }

                    AndroidUtils.hideKeyboard(requireView())
                } else {
                    Toast.makeText(requireContext(), R.string.empty_post, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            back.setOnClickListener {
                if (status == NEW_POST || status == EDITING_NEW_POST) {
                    viewModel.edited.value = viewModel.empty
                    viewModel.changeMedia(null, null, null)
                    viewModel.listMentionedUser = emptySet()
                    viewModel.listMapUser = emptyMap()
                    viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
                    viewModel.statusMedia = false
                } else {
                    viewModelMyWall.edited.value = viewModelMyWall.empty
                    viewModelMyWall.changeMedia(null, null, null)
                    viewModelMyWall.listMentionedUser = emptySet()
                    viewModelMyWall.listMapUser = emptyMap()
                    viewModelMyWall.coordinates = Coordinates(lat = 0.0, long = 0.0)
                }

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
                    R.id.action_newPostFragment_to_userFragment,
                    Bundle().apply {
                        statusUserFragment =
                            if (status == NEW_POST || status == EDITING_NEW_POST)
                                CHOOSING_MENTIONED_USER_POST
                            else
                                CHOOSING_MENTIONED_USER_WALL
                    }
                )
            }

            addLocation.setOnClickListener {
                findNavController().navigate(
                    R.id.action_newPostFragment_to_addLocationFragment,
                    Bundle().apply {
                        statusAddLocationFragment =
                            if (status == NEW_POST || status == EDITING_NEW_POST)
                                POST
                            else
                                WALL
                    }
                )
            }

            removePhoto.setOnClickListener {
                if (status == NEW_POST || status == EDITING_NEW_POST) {
                    viewModel.changeMedia(null, null, AttachmentType.IMAGE)
                } else {
                    viewModelMyWall.changeMedia(null, null, AttachmentType.IMAGE)
                }
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

        viewModelMyWall.media.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.groupPhotoContainer.visibility = View.GONE
                return@observe
            }

            if (it.attachmentType == AttachmentType.IMAGE) {
                binding.groupPhotoContainer.visibility = View.VISIBLE
                binding.photo.setImageURI(it.uri)
            }
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModelMyWall.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
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

        viewModelMyWall.errorMyWall403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.need_to_log, Toast.LENGTH_SHORT).show()
        }

        viewModelMyWall.errorMyWall404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.post_not_found, Toast.LENGTH_SHORT).show()
        }

        viewModelMyWall.errorMyWall415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_file_format, Toast.LENGTH_SHORT)
                .show()
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

            if (status == NEW_POST || status == EDITING_NEW_POST) {
                viewModel.edited.value = viewModel.empty
                viewModel.changeMedia(null, null, null)
                viewModel.listMentionedUser = emptySet()
                viewModel.listMapUser = emptyMap()
                viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
                viewModel.statusMedia = false
            } else {
                viewModelMyWall.edited.value = viewModelMyWall.empty
                viewModelMyWall.changeMedia(null, null, null)
                viewModelMyWall.listMentionedUser = emptySet()
                viewModelMyWall.listMapUser = emptyMap()
                viewModelMyWall.coordinates = Coordinates(lat = 0.0, long = 0.0)
            }

            findNavController().navigateUp()
        }

        callback.isEnabled
        return binding.root
    }
}

