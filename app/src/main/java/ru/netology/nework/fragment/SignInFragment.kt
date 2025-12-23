package ru.netology.nework.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentSignInBinding
import ru.netology.nework.viewmodel.SignInViewModel
import ru.netology.nework.viewmodel.UserViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SignInFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignInBinding.inflate(layoutInflater, container, false)

        val viewModel: SignInViewModel by viewModels()
        val viewModelUser: UserViewModel by activityViewModels()

        with(binding) {
            login.requestFocus()

            back.setOnClickListener {
                findNavController().navigateUp()
            }

            signIn.setOnClickListener {
                if (loginInput.text.toString().isEmpty()) {
                    loginInput.error = null
                    loginInput.error = getString(R.string.empty_login)
                }

                if (passwordInput.text.toString().isEmpty()) {
                    passwordInput.error = null
                    passwordInput.error = getString(R.string.empty_password)
                }

                if (!loginInput.text.toString().isEmpty() && !passwordInput.text.toString().isEmpty()) {
                    viewModel.signIn(loginInput.text.toString(), passwordInput.text.toString())
                }
            }

            noRegistration.setOnClickListener {
                findNavController().navigate(
                    R.id.action_signInFragment2_to_signUpFragment2,
                )
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
//            if (it.token != null && it.avatar != null) {
//                auth.setAuth(it.id, it.token, it.avatar)
//                findNavController().navigateUp()
//            }
        }

        viewModel.errorPost400.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.wrong_login_or_password, Toast.LENGTH_SHORT).show()
        }

        viewModel.errorPost404.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), R.string.user_is_not_registered, Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}