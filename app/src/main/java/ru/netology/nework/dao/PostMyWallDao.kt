package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.enumeration.AttachmentType

@Dao
interface PostMyWallDao {
    @Query("SELECT * FROM PostMyWallEntity ORDER BY id DESC")
    fun getAll(): List<PostMyWallEntity>

    @Query("SELECT * FROM PostMyWallEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostMyWallEntity>

    @Query("SELECT * FROM PostMyWallEntity WHERE id = :id")
    fun getPost(id: Long): PostMyWallEntity

//    @Query("SELECT * FROM PostEntity WHERE viewed == 1 ORDER BY id DESC")
//    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) == 0 FROM PostMyWallEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostMyWallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostMyWallEntity>)

    @Query("UPDATE PostMyWallEntity Set content = :text WHERE id = :id")
    suspend fun changeContentById(id: Long, text: String)

    @Query("UPDATE PostMyWallEntity Set id = :newId, author = :newAuthor, authorId = :newAuthorId, authorAvatar = :newAuthorAvatar, authorJob = :newAuthorJob, url = :newUrl, type = :newType WHERE id = :id")
    suspend fun changeIdPostById(
        id: Long,
        newId: Long,
        newAuthor: String,
        newAuthorId: Long,
        newAuthorAvatar: String?,
        newAuthorJob: String?,
        newUrl: String?,
        newType: AttachmentType?
    )

//    @Query("UPDATE PostEntity Set viewed = 1 WHERE viewed = 0")
//    suspend fun browse()

    suspend fun save(post: PostMyWallEntity) =
        if (post.id == 0L) insert(post) else changeContentById(post.id, post.content)

    @Query(
        """
            UPDATE PostMyWallEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long)

    @Query(
        """
            UPDATE PostMyWallEntity SET
                playSong = CASE WHEN playSong THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonSong(id: Long)

    @Query(
        """
            UPDATE PostMyWallEntity SET
                playVideo = CASE WHEN playVideo THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonVideo(id: Long)

    @Query("DELETE FROM PostMyWallEntity WHERE id = :id")
    suspend fun removeById(id: Long)
}