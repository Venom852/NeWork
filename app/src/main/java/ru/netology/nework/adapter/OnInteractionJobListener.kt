package ru.netology.nework.adapter

import ru.netology.nework.dto.Job

interface OnInteractionJobListener {
    fun onDelete(job: Job)
}