package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.JobMyEntity

@Dao
interface JobMyDao {
    @Query("SELECT * FROM JobMyEntity ORDER BY id DESC")
    fun getAllFlow(): Flow<List<JobMyEntity>>

    @Query("SELECT * FROM JobMyEntity ORDER BY id DESC")
    fun getAll(): List<JobMyEntity>

    @Query("SELECT * FROM JobMyEntity WHERE id = :id")
    fun getJob(id: Long): JobMyEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveJob(job: JobMyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobMyEntity>)

    @Query("UPDATE JobMyEntity Set id = :newId WHERE id = :id")
    suspend fun changeIdJobById(id: Long, newId: Long)

    @Query("DELETE FROM JobMyEntity WHERE id = :id")
    suspend fun removeById(id: Long)
}