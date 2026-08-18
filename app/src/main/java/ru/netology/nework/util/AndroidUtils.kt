package ru.netology.nework.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.Group
import com.google.gson.Gson
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.PostEntity

object AndroidUtils {
    private val gson = Gson()
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,  0)
    }

    fun Group.setAllOnClickListener(listener: (View) -> Unit) {
        referencedIds.forEach { _ ->
            rootView.setOnClickListener(listener)
        }
    }

    fun toJsonUserPreview(userPreview: UserPreview): String {
        return gson.toJson(userPreview)
    }

    fun toUserPreview(string: String): UserPreview {
        return gson.fromJson(string, UserPreview::class.java)
    }

    fun View.focusAndShowKeyboard() {
        /**
         * This is to be called when the window already has focus.
         */
        fun View.showTheKeyboardNow() {
            if (isFocused) {
                post {
                    // We still post the call, just in case we are being notified of the windows focus
                    // but InputMethodManager didn't get properly setup yet.
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    }
}