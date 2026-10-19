package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.CardUsersBinding
import ru.netology.nework.dto.User

class UserAdapter(
    private val onInteractionUserListener: OnInteractionUserListener,
    private val auth: AppAuth
) : ListAdapter<User, UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CardUsersBinding.inflate(layoutInflater, parent, false)
        return UserViewHolder(binding, onInteractionUserListener, auth = auth)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }
}