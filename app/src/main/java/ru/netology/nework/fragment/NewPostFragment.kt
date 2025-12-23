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
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.fragment.AddLocationFragment.Companion.POST
import ru.netology.nework.fragment.AddLocationFragment.Companion.WALL
import ru.netology.nework.fragment.AddLocationFragment.Companion.statusAddLocationFragment
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_WALL
import ru.netology.nework.fragment.UserFragment.Companion.statusUserFragment
import ru.netology.nework.viewmodel.MyWallViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {
    companion object {
        const val NEW_POST = "newPost"
        const val NEW_POST_WALL = "newPostWall"
        const val EDITING_NEW_POST = "editingNewPost"
        const val EDITING_NEW_POST_WALL = "editingNewPostWall"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.newPostFragmentBundle by StringArg
        var Bundle.statusFragment by StringArg
    }

    @Inject
    lateinit var contentDraftDao: ContentDraftDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(layoutInflater, container, false)

        var status = ""

        val viewModel: PostViewModel by activityViewModels()
        val viewModelMyWall: MyWallViewModel by activityViewModels()

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.newPostFragmentBundle?.let {
            status = it
            if (status == NEW_POST || status == NEW_POST_WALL) {
                lifecycleScope.launch {
                    if (contentDraftDao.getDraft() != null) {
                        binding.content.setText(contentDraftDao.getDraft())
                        contentDraftDao.removeDraft()
                    }
                }
            } else {
                binding.content.setText(it)
                editing = true
            }
            arguments?.newPostFragmentBundle = null
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
                        } else {
                            viewModelMyWall.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)
                        }
                    }
                }
            }

        val pickAudio =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

                if (uri != null) {
                    Log.d("Audio", "Selected URI: $uri")

                    if (status == NEW_POST || status == EDITING_NEW_POST) {
                        viewModel.changeMedia(uri, uri.toFile(), AttachmentType.AUDIO)
                    } else {
                        viewModelMyWall.changeMedia(uri, uri.toFile(), AttachmentType.AUDIO)
                    }
                } else {
                    Log.d("Audio", "No media selected")
                }
            }

        val pickVideo =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Log.d("Video", "Selected URI: $uri")

                    if (status == NEW_POST || status == EDITING_NEW_POST) {
                        viewModel.changeMedia(uri, uri.toFile(), AttachmentType.VIDEO)
                    } else {
                        viewModelMyWall.changeMedia(uri, uri.toFile(), AttachmentType.VIDEO)
                    }
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

                            if (status == NEW_POST || status == EDITING_NEW_POST) {
                                viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.AUDIO)
                            } else {
                                viewModelMyWall.changeMedia(uri, uri?.toFile(), AttachmentType.AUDIO)
                            }
                        } else {
                            Log.d("Audio", "No media selected")
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
                }
                //TODO(Проверить поведение)
                if (status == NEW_POST || status == EDITING_NEW_POST) {
                    viewModel.edited.value = viewModel.empty
                    viewModel.changeMedia(null, null, null)
                } else {
                    viewModelMyWall.edited.value = viewModel.empty
                    viewModelMyWall.changeMedia(null, null, null)
                }

            }

            back.setOnClickListener {
                if (status == NEW_POST || status == EDITING_NEW_POST) {
                    viewModel.edited.value = viewModel.empty
                    viewModel.changeMedia(null, null, null)
                    viewModel.listMentionedUser = emptySet<Long>()
                    viewModel.listMapUser = emptyMap<Long, UserPreview>()
                    viewModel.coordinates = Coordinates(lat = 0.0, long = 0.0)
                } else {
                    viewModelMyWall.edited.value = viewModel.empty
                    viewModelMyWall.changeMedia(null, null, null)
                    viewModelMyWall.listMentionedUser = emptySet<Long>()
                    viewModelMyWall.listMapUser = emptyMap<Long, UserPreview>()
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
            if (it.attachmentType == AttachmentType.IMAGE) {
                if (it.uri == null) {
                    binding.groupPhotoContainer.visibility = View.GONE
                    return@observe
                }

                binding.groupPhotoContainer.visibility = View.VISIBLE
                binding.photo.setImageURI(it.uri)
            }
        }

        viewModelMyWall.media.observe(viewLifecycleOwner) {
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
//                lifecycleScope.launch {
//                    contentDraftDao.saveDraft(binding.content.text.toString())
//                }
            }
            editing = false

            if (status == NEW_POST || status == EDITING_NEW_POST) {
                viewModel.edited.value = viewModel.empty
            } else {
                viewModelMyWall.edited.value = viewModel.empty
            }

            findNavController().navigateUp()
        }

        callback.isEnabled
        return binding.root
    }
}

