package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.PostUserWallEntity

@Dao
interface PostUserWallDao {
    @Query("SELECT * FROM PostUserWallEntity ORDER BY id DESC")
    fun getAll(): List<PostUserWallEntity>

    @Query("SELECT * FROM PostUserWallEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostUserWallEntity>

    @Query("SELECT * FROM PostUserWallEntity WHERE id = :id")
    fun getPost(id: Long): PostUserWallEntity

    @Query("SELECT COUNT(*) == 0 FROM PostUserWallEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostUserWallEntity>)

    @Query(
        """
            UPDATE PostUserWallEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long)
}