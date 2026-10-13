package ru.netology.nework.dto

import java.io.File

//data class Media(val id: String)
data class Media(val url: String)

data class MediaUpload(val file: File)