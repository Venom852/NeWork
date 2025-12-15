package ru.netology.nework.dto

import java.time.Instant

data class Job (
    val id: Long,
    val name: String,
    val position: String,
    val start: Instant,
    val finish: Instant?,
    val link: String?
)