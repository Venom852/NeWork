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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ErrorCode400And500Binding
import ru.netology.nework.databinding.FragmentSignUpBinding
import ru.netology.nework.util.SwipeDirection
import ru.netology.nework.util.detectSwipe
import ru.netology.nework.viewmodel.SignUpViewModel
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
        val bindingErrorCode400And500 =
            ErrorCode400And500Binding.inflate(layoutInflater, container, false)

        applyInset(binding.root)

        val dialog = BottomSheetDialog(requireContext())
        val viewModel: SignUpViewModel by viewModels()

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
                    loginInput.error = getString(R.string.empty_login)
                    loginInput.error = null
//                    Toast.makeText(requireContext(), R.string.empty_login, Toast.LENGTH_SHORT).show()
                }

                if (nameInput.text.toString().isEmpty()) {
                    nameInput.error = getString(R.string.empty_user_name)
                    nameInput.error = null
//                    Toast.makeText(requireContext(), R.string.empty_user_name, Toast.LENGTH_SHORT).show()
                }

                if (passwordInput.text.toString().isEmpty()) {
                    passwordInput.error = getString(R.string.empty_password)
                    passwordInput.error = null
//                    Toast.makeText(requireContext(), R.string.empty_password, Toast.LENGTH_SHORT).show()
                }

                if (confirmationPasswordInput.text.toString().isEmpty()) {
                    confirmationPasswordInput.error = getString(R.string.empty_password_confirmation)
                    confirmationPasswordInput.error = null
//                    Toast.makeText(requireContext(), R.string.empty_password_confirmation, Toast.LENGTH_SHORT).show()
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

        //TODO(Нужно ли менять liveData)
        viewModel.authState.observe(viewLifecycleOwner) {
            if (it.token != null) {
                auth.setAuth(it.id, it.token)
                findNavController().navigateUp()
            }
        }

        //TODO(Нужно ли менять liveData)
        viewModel.photo.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.avatarImage.visibility = View.GONE
                return@observe
            }

            binding.avatarImage.visibility = View.VISIBLE
            binding.choosePhoto.visibility = View.GONE
            binding.avatarImage.setImageURI(it.uri)
        }

        //TODO(Нужно ли менять liveData, и нужна ли обработка ошибки)
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.errorCode300) {
                binding.signUpFragment.isVisible = false
                binding.errorCode300.error300Group.isVisible = true
            } else {
                binding.signUpFragment.isVisible = true
                binding.errorCode300.error300Group.isVisible = false
            }

            if (state.error) {
                Snackbar.make(binding.root, R.string.something_went_wrong, Snackbar.LENGTH_LONG)
                    .show()
            }
        }

        //TODO(Нужно ли менять liveData, настроить ошибку, добавить Toast)
        viewModel.bottomSheet.observe(viewLifecycleOwner) {
            dialog.setCancelable(false)
            dialog.setContentView(bindingErrorCode400And500.root)
            dialog.show()
        }

        binding.errorCode300.buttonError.setOnClickListener {
            binding.signUpFragment.isVisible = true
            binding.errorCode300.error300Group.isVisible = false
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