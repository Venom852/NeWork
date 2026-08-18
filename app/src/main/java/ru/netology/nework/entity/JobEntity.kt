package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Job
import java.time.Instant

@Entity
data class JobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val position: String,
    val start: Instant,
    val finish: Instant?,
    val link: String?
) {
    fun toJobDto() = Job(
        id,
        name,
        position,
        start,
        finish,
        link
    )

    companion object {
        fun fromJobDto(job: Job) = JobEntity(
            job.id,
            job.name,
            job.position,
            job.start,
            job.finish,
            job.link
        )
    }
}

fun List<JobEntity>.toJobDto(): List<Job> = map(JobEntity::toJobDto)
fun List<Job>.toJobEntity(): List<JobEntity> = map(JobEntity::fromJobDto)