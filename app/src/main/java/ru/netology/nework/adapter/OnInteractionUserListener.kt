package ru.netology.nework.adapter

import ru.netology.nework.dto.User

interface OnInteractionUserListener {
    fun onRadioButton(user: User)
}