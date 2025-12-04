package ru.netology.nework.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
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
            userName.requestFocus()

            signUp.setOnClickListener {
                print(password.toString())
                print(confirmationPassword.toString())

                if (userName.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_user_name, Toast.LENGTH_SHORT).show()
                }

                if (login.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_login, Toast.LENGTH_SHORT).show()
                }

                if (password.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_password, Toast.LENGTH_SHORT).show()
                }

                if (confirmationPassword.text.toString().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.empty_password_confirmation, Toast.LENGTH_SHORT).show()
                }

                if (password.text.toString() == confirmationPassword.text.toString()) {
                    viewModel.signUp(userName.text.toString(), login.text.toString(), password.text.toString())
                } else {
                    Toast.makeText(requireContext(), R.string.password_confirmation, Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.authState.observe(viewLifecycleOwner) {
                if (it.token != null) {
                    auth.setAuth(it.id, it.token)
                    findNavController().navigateUp()
                }
            }

            viewModel.photo.observe(viewLifecycleOwner) {
                if (it.uri == null) {
                    binding.groupAvatar.visibility = View.GONE
                    return@observe
                }

                binding.groupAvatar.visibility = View.VISIBLE
                binding.avatarImage.setImageURI(it.uri)
            }

            pickPhoto.setOnClickListener {
                ImagePicker.with(requireParentFragment())
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

            takePhoto.setOnClickListener {
                ImagePicker.with(requireParentFragment())
                    .crop()
                    .compress(2048)
                    .provider(ImageProvider.CAMERA)
                    .createIntent(pickPhotoLauncher::launch)
            }
        }

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
}