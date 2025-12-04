package ru.netology.nework.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.fragment.NewPostFragment.Companion.textArg
import androidx.core.view.MenuProvider
import ru.netology.nework.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.AppActivityBinding
import ru.netology.nework.databinding.ConfirmationOfExitBinding
import javax.inject.Inject
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.installations.FirebaseInstallations

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {
    @Inject
    lateinit var auth: AppAuth
//    @Inject
//    lateinit var firebaseMessaging: FirebaseMessaging
//    @Inject
//    lateinit var firebaseInstallations: FirebaseInstallations
//    @Inject
//    lateinit var googleApiAvailability: GoogleApiAvailability

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = AppActivityBinding.inflate(layoutInflater)
        val bindingConfirmationOfExit = ConfirmationOfExitBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)

        setContentView(binding.root)
        applyInset(binding.root)
//        requestNotificationsPermission()

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text.isNullOrBlank()) {
                Snackbar.make(
                    binding.root,
                    R.string.error_empty_content,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(android.R.string.ok) {
                        finish()
                    }.show()
                return@let
            } else {
                findNavController(R.id.fragmentContainer).navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
            }
        }

        viewModel.data.observe(this) {
            invalidateOptionsMenu()
        }

//        firebaseInstallations.id.addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                println("some stuff happened: ${task.exception}")
//                return@addOnCompleteListener
//            }
//
//            val token = task.result
//            println(token)
//        }
//
//        firebaseMessaging.token.addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                println("some stuff happened: ${task.exception}")
//                return@addOnCompleteListener
//            }
//
//            val token = task.result
//            println(token)
//        }
//
//        checkGoogleApiAvailability()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.auth_menu, menu)

                menu.let {
                    it.setGroupVisible(R.id.unauthenticated, !viewModel.authenticated)
                    it.setGroupVisible(R.id.authenticated, viewModel.authenticated)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.signIn -> {
                        findNavController(R.id.fragmentContainer).navigate(
                            R.id.action_feedFragment_to_signInFragment2
                        )
                        true
                    }

                    R.id.signUp -> {
                        findNavController(R.id.fragmentContainer).navigate(
                            R.id.action_feedFragment_to_signUpFragment2
                        )
                        true
                    }

                    R.id.signOut -> {
                        dialog.setCancelable(false)
                        dialog.setContentView(bindingConfirmationOfExit.root)
                        dialog.show()
                        true
                    }

                    else -> false
                }
        })

        bindingConfirmationOfExit.close.setOnClickListener {
            dialog.dismiss()
        }

        bindingConfirmationOfExit.signOut.setOnClickListener {
            auth.removeAuth()
            findNavController(R.id.fragmentContainer)
                .navigate(R.id.nav_main)
            dialog.dismiss()
        }
    }

//    private fun requestNotificationsPermission() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            return
//        }
//
//        val permission = Manifest.permission.POST_NOTIFICATIONS
//
//        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
//            return
//        }
//
//        requestPermissions(arrayOf(permission), 1)
//    }
//
//    private fun checkGoogleApiAvailability() {
//        with(googleApiAvailability) {
//            val code = isGooglePlayServicesAvailable(this@AppActivity)
//            if (code == ConnectionResult.SUCCESS) {
//                return@with
//            }
//            if (isUserResolvableError(code)) {
//                getErrorDialog(this@AppActivity, code, 9000)?.show()
//                return
//            }
//            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
//                .show()
//        }
//
//        firebaseMessaging.token.addOnSuccessListener {
//            println(it)
//        }
//    }

    private fun applyInset(main: View) {
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}