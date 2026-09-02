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
import ru.netology.nework.fragment.ProfileFragment.Companion.PROHIBIT
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_POST
import ru.netology.nework.fragment.UserFragment.Companion.status
import ru.netology.nework.fragment.ProfileFragment.Companion.USER
import ru.netology.nework.fragment.ProfileFragment.Companion.YOUR
import ru.netology.nework.fragment.ProfileFragment.Companion.statusPermissionToCross
import ru.netology.nework.fragment.ProfileFragment.Companion.statusProfileFragment
import ru.netology.nework.fragment.ProfileFragment.Companion.userFragmentBundle
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_MENTIONED_USER_WALL
import ru.netology.nework.fragment.UserFragment.Companion.CHOOSING_SPEAKERS_USER
import ru.netology.nework.fragment.UserFragment.Companion.LIKE
import ru.netology.nework.fragment.UserFragment.Companion.MENTIONED
import ru.netology.nework.fragment.UserFragment.Companion.PARTICIPANTS
import ru.netology.nework.fragment.UserFragment.Companion.SPEAKERS
import ru.netology.nework.util.AndroidUtils.setAllOnClickListener

class UserViewHolder(
    private val binding: CardUsersBinding,
    private val onInteractionUserListener: OnInteractionUserListener,
    private val gson: Gson = Gson()
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(user: User) {
        with(binding) {
            val options = RequestOptions()

            nameUser.text = user.name
            loginUser.text = user.login

            radioButton.visibility = if (status == CHOOSING_MENTIONED_USER_POST
                || status == CHOOSING_SPEAKERS_USER
            ) View.VISIBLE else View.GONE

            Glide.with(avatarUser)
                .load(user.avatar)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .apply(options.circleCrop())
                .into(avatarUser)

            if (user.avatar == null) {
                avatarUser.setImageResource(R.drawable.ic_netology)
            }

            radioButton.setOnClickListener {
                onInteractionUserListener.onRadioButton(user)
            }


//            groupUser.setAllOnClickListener {
//                if (status == CHOOSING_MENTIONED_USER_POST
//                    || status == CHOOSING_SPEAKERS_USER) {
//                    findNavController(it).navigate(
//                        R.id.action_feedFragment_to_yourProfileFragment,
//                        Bundle().apply {
//                            userFragmentBundle = gson.toJson(user)
//                        })
//                } else {
//                    findNavController(it).navigate(
//                        R.id.action_userFragment_to_yourProfileFragment,
//                        Bundle().apply {
//                            userFragmentBundle = gson.toJson(user)
//                        })
//                }
//            }

            userConstraint.setOnClickListener {
                //TODO(Настроить)
//                if (status == CHOOSING_MENTIONED_USER_POST || status == CHOOSING_SPEAKERS_USER
//                    || status == CHOOSING_MENTIONED_USER_WALL || status == LIKE
//                    || status == MENTIONED || status == PARTICIPANTS || status == SPEAKERS) {
//
//                    if (user.name == "Me") {
//                        onInteractionUserListener.onSaveAuthorId(user.id)
//
//                        findNavController(it).navigate(
//                            R.id.action_userFragment_to_yourProfileFragment,
//                            Bundle().apply {
//                                statusProfileFragment = YOUR
//                            })
//                    } else {
//                        onInteractionUserListener.onSaveAuthorId(user.id)
//
//                        findNavController(it).navigate(
//                            R.id.action_userFragment_to_yourProfileFragment,
//                            Bundle().apply {
////                                userFragmentBundle = gson.toJson(user.id)
//                                statusProfileFragment = USER
//                            })
//                    }
//                } else {
//                    if (user.name == "Me") {
//                        onInteractionUserListener.onSaveAuthorId(user.id)
//
//                        findNavController(it).navigate(
//                            R.id.action_feedFragment_to_yourProfileFragment,
//                            Bundle().apply {
//                                statusProfileFragment = YOUR
//                            })
//                    } else {
//                        onInteractionUserListener.onSaveAuthorId(user.id)
//
//                        findNavController(it).navigate(
//                            R.id.action_feedFragment_to_yourProfileFragment,
//                            Bundle().apply {
////                                userFragmentBundle = gson.toJson(user.id)
//                                statusProfileFragment = USER
//                            })
//                    }
//                }

                onInteractionUserListener.onSaveAuthorId(user.id)

                if (status == CHOOSING_MENTIONED_USER_POST || status == CHOOSING_SPEAKERS_USER
                    || status == CHOOSING_MENTIONED_USER_WALL || status == LIKE
                    || status == MENTIONED || status == PARTICIPANTS || status == SPEAKERS) {

                    findNavController(it).navigate(
                        R.id.action_userFragment_to_yourProfileFragment,
                        Bundle().apply {
                            statusPermissionToCross = PROHIBIT

                            if (user.name == "Me123") {
                                statusProfileFragment = YOUR
                            } else {
                                statusProfileFragment = USER
                            }
                        })
                } else {
                    findNavController(it).navigate(
                        R.id.action_feedFragment_to_yourProfileFragment,
                        Bundle().apply {
                            statusPermissionToCross = PROHIBIT

                            if (user.name == "Me123") {
                                statusProfileFragment = YOUR
                            } else {
                                statusProfileFragment = USER
                            }
                        })
                }
            }
        }
    }
}