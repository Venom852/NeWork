package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Job
import java.time.Instant

@Entity
data class JobMyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val position: String,
    val start: Instant,
    val finish: Instant?,
    val link: String?
) {
    fun toJobMyDto() = Job(
        id,
        name,
        position,
        start,
        finish,
        link
    )

    companion object {
        fun fromJobMyDto(job: Job) = JobMyEntity(
            job.id,
            job.name,
            job.position,
            job.start,
            job.finish,
            job.link
        )
    }
}

fun List<JobMyEntity>.toJobMyDto(): List<Job> = map(JobMyEntity::toJobMyDto)
fun List<Job>.toJobMyEntity(): List<JobMyEntity> = map(JobMyEntity::fromJobMyDto)