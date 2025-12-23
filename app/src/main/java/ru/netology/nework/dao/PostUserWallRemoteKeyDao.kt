package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.PostUserRemoteKeyEntity

@Dao
interface PostUserWallRemoteKeyDao {
    @Query("SELECT COUNT(*) == 0 FROM PostUserRemoteKeyEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT MAX(id) FROM PostUserRemoteKeyEntity")
    suspend fun max(): Long?

    @Query("SELECT MIN(id) FROM PostUserRemoteKeyEntity")
    suspend fun min(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: PostUserRemoteKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(keys: List<PostUserRemoteKeyEntity>)

    @Query("DELETE FROM PostUserRemoteKeyEntity")
    suspend fun removeAll()
}