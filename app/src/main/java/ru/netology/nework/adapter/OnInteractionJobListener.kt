package ru.netology.nework.adapter

import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User

interface OnInteractionJobListener {
    fun onDelete(job: Job)
}