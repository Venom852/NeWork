package ru.netology.nework.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.view.MenuProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.dao.PostDao
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {
    companion object {
        const val NEW_POST_KEY = "newPost"
        private var editing = false
        var Bundle.textArg by StringArg
        var Bundle.textContentArg by StringArg
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

        val dialog = BottomSheetDialog(requireContext())
        val viewModel: PostViewModel by activityViewModels()

        arguments?.textArg?.let {
            binding.content.setText(it)
            arguments?.textArg = null
        }

        arguments?.textContentArg?.let {
            val text = it
            if (text == NEW_POST_KEY) {
                binding.group.visibility = View.GONE
                lifecycleScope.launch {
                    if (dao.getDraft() != null) {
                        binding.content.setText(dao.getDraft())
                        dao.removeDraft()
                    }
                }
            } else {
                binding.content.setText(text)
                binding.cancelEdit.text = binding.content.text
                editing = true
            }
            arguments?.textContentArg = null
        }

        binding.content.requestFocus()

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
                        viewModel.changePhoto(uri, uri?.toFile())
                    }
                }
            }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
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
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.CAMERA)
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.removePhoto.setOnClickListener {
            viewModel.changePhoto(null, null)
        }

        viewModel.photo.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.groupPhotoContainer.visibility = View.GONE
                return@observe
            }

            binding.groupPhotoContainer.visibility = View.VISIBLE
            binding.photo.setImageURI(it.uri)
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_new_post, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.save -> {
                        binding.let {
                            if (!binding.content.text.isNullOrBlank()) {
                                viewModel.saveContent(it.content.text.toString())
                                AndroidUtils.hideKeyboard(requireView())
                            }
                            viewModel.edited.value = viewModel.empty
                        }
                        true
                    }

                    else -> false
                }

        }, viewLifecycleOwner)

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.dataState.observe(viewLifecycleOwner) {
            if (it.errorCode300) {
                findNavController().navigateUp()
            }
        }

        viewModel.bottomSheet.observe(viewLifecycleOwner) {
            dialog.setCancelable(false)
            dialog.setContentView(bindingErrorCode400And500.root)
            dialog.show()
        }

        bindingErrorCode400And500.errorCode400And500.setOnClickListener {
            dialog.dismiss()
        }

        binding.cancel.setOnClickListener {
            viewModel.edited.value = viewModel.empty
            findNavController().navigateUp()
        }

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
}

