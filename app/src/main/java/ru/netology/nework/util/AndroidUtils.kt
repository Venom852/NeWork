package ru.netology.nework.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.Group

object AndroidUtils {
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,  0)
    }

    private fun Group.setAllOnClickListener(listener: (View) -> Unit) {
        referencedIds.forEach { _ ->
            rootView.setOnClickListener(listener)
        }
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