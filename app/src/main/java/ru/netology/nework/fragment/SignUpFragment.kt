package ru.netology.nework.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentSignUpBinding
import ru.netology.nework.viewmodel.SignUpViewModel
import ru.netology.nework.viewmodel.UserViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SignUpFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(layoutInflater, container, false)

        val viewModel: SignUpViewModel by viewModels()
        val viewModelUser: UserViewModel by activityViewModels()

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

        with(binding) {
            loginInput.requestFocus()

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            signUp.setOnClickListener {
                if (loginInput.text.toString().isEmpty()) {
                    loginInput.error = null
                    loginInput.error = getString(R.string.empty_login)
                }

                if (nameInput.text.toString().isEmpty()) {
                    nameInput.error = null
                    nameInput.error = getString(R.string.empty_user_name)
                }

                if (passwordInput.text.toString().isEmpty()) {
                    passwordInput.error = null
                    passwordInput.error = getString(R.string.empty_password)
                }

                if (confirmationPasswordInput.text.toString().isEmpty()) {
                    confirmationPasswordInput.error = null
                    confirmationPasswordInput.error = getString(R.string.empty_password_confirmation)
                }

                if (!loginInput.text.toString().isEmpty() &&
                    !nameInput.text.toString().isEmpty() &&
                    !passwordInput.text.toString().isEmpty() &&
                    !confirmationPasswordInput.text.toString().isEmpty() &&
                    passwordInput.text.toString() == confirmationPasswordInput.text.toString()
                ) {
                    viewModel.signUp(
                        loginInput.text.toString(),
                        nameInput.text.toString(),
                        passwordInput.text.toString()
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.password_confirmation,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            choosePhoto.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_photo_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickPhoto -> {
                                ImagePicker.with(this@SignUpFragment)
                                    .crop()
                                    .compress(2048)
                                    .maxResultSize(2048, 2048)
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
                                ImagePicker.with(this@SignUpFragment)
                                    .crop()
                                    .compress(2048)
                                    .maxResultSize(2048, 2048)
                                    .provider(ImageProvider.CAMERA)
                                    .galleryMimeTypes(
                                        arrayOf(
                                            "image/png",
                                            "image/jpeg",
                                        )
                                    )
                                    .createIntent(pickPhotoLauncher::launch)

                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            avatarImage.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.choose_photo_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.pickPhoto -> {
                                ImagePicker.with(this@SignUpFragment)
                                    .crop()
                                    .compress(2048)
                                    .maxResultSize(2048, 2048)
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
                                ImagePicker.with(this@SignUpFragment)
                                    .crop()
                                    .compress(2048)
                                    .maxResultSize(2048, 2048)
                                    .provider(ImageProvider.CAMERA)
                                    .galleryMimeTypes(
                                        arrayOf(
                                            "image/png",
                                            "image/jpeg",
                                        )
                                    )
                                    .createIntent(pickPhotoLauncher::launch)

                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

        }

        viewModel.authState.observe(viewLifecycleOwner) {
            viewModelUser.refreshUsers()

            when {
                it.token != null && it.avatar != null -> {
                    auth.setAuth(it.id, it.token, it.avatar)
                    findNavController().navigateUp()
                }

                it.token != null -> {
                    auth.setAuth(it.id, it.token, null)
                    findNavController().navigateUp()
                }
            }

//            if (it.token != null) {
//                auth.setAuth(it.id, it.token, null)
//                findNavController().navigateUp()
//            }
        }

        viewModel.photo.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.avatarImage.visibility = View.GONE
                return@observe
            }

            binding.avatarImage.visibility = View.VISIBLE
            binding.choosePhoto.visibility = View.INVISIBLE
            binding.avatarImage.setImageURI(it.uri)
        }

        viewModel.errorPost403.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.login_busy, Toast.LENGTH_SHORT).show()
        }

        viewModel.errorPost415.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.incorrect_photo_format, Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}