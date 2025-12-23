package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.PostMyWallRemoteKeyEntity

@Dao
interface PostMyWallRemoteKeyDao {
    @Query("SELECT COUNT(*) == 0 FROM PostMyWallRemoteKeyEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT MAX(id) FROM PostMyWallRemoteKeyEntity")
    suspend fun max(): Long?

    @Query("SELECT MIN(id) FROM PostMyWallRemoteKeyEntity")
    suspend fun min(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: PostMyWallRemoteKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(keys: List<PostMyWallRemoteKeyEntity>)

    @Query("DELETE FROM PostMyWallRemoteKeyEntity")
    suspend fun removeAll()
}