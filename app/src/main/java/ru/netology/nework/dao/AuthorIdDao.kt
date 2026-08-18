package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.AuthorIdEntity


@Dao
interface AuthorIdDao {
    @Query("SELECT * FROM AuthorIdEntity ORDER BY id DESC")
    fun getAuthorId(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertId(authorIdEntity: AuthorIdEntity)

    suspend fun saveId(id: Long) = insertId(AuthorIdEntity(id))

    @Query("DELETE FROM AuthorIdEntity")
    suspend fun removeId()
}