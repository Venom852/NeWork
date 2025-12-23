package ru.netology.nework.adapter

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.CardUsersBinding
import ru.netology.nework.dto.User
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
import ru.netology.nework.fragment.UserFragment.Companion.status
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.userFragmentBundle
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener

class UserViewHolder(
    private val binding: CardUsersBinding,
    private val onInteractionUserListener: OnInteractionUserListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(user: User) {
        with(binding) {
            nameUser.text = user.name
            loginUser.text = user.login

            radioButton.visibility = if (status == CHOOSING_MENTIONED_USER_POST
                || status == CHOOSING_SPEAKERS_USER) View.VISIBLE else View.GONE

            //TODO(Проверить запрос)
            val url = "${BuildConfig.BASE_URL}/avatars/${user.avatar}"
            val options = RequestOptions()

            Glide.with(binding.avatarUser)
                .load(url)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(binding.avatarUser)

            radioButton.setOnClickListener {
                onInteractionUserListener.onRadioButton(user)
            }

            groupUser.setAllOnClickListener {
                findNavController(it).navigate(
                    R.id.action_userFragment_to_yourProfileFragment,
                    Bundle().apply {
                        userFragmentBundle = gson.toJson(user)
                    })
            }
        }
    }
}