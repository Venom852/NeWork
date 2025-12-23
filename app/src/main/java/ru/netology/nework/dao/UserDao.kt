package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getAllFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getAllUser(): List<UserEntity>

    @Query("SELECT * FROM UserEntity WHERE id = :id")
    fun getUser(id: Long): UserEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT COUNT(*) == 0 FROM UserEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT COUNT(*) FROM UserEntity")
    suspend fun count(): Int
}