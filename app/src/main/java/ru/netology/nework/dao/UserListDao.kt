package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.UserListEntity

@Dao
interface UserListDao {
    @Query("SELECT * FROM UserListEntity ORDER BY id DESC")
    fun getAllFlow(): Flow<List<UserListEntity>>

    @Query("SELECT * FROM UserListEntity ORDER BY id DESC")
    fun getAllUser(): List<UserListEntity>

    @Query("SELECT * FROM UserListEntity WHERE id = :id")
    fun getUser(id: Long): UserListEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserListEntity>)

    @Query("SELECT COUNT(*) == 0 FROM UserListEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT COUNT(*) FROM UserListEntity")
    suspend fun count(): Int

    @Query("DELETE FROM UserListEntity")
    suspend fun removeUsers()
}