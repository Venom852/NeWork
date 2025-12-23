package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.enumeration.AttachmentType

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): List<PostEntity>

    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    fun getPost(id: Long): PostEntity

//    @Query("SELECT * FROM PostEntity WHERE viewed == 1 ORDER BY id DESC")
//    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE PostEntity Set content = :text WHERE id = :id")
    suspend fun changeContentById(id: Long, text: String)

    @Query("UPDATE PostEntity Set id = :newId, author = :newAuthor, authorId = :newAuthorId, authorAvatar = :newAuthorAvatar, authorJob = :newAuthorJob, url = :newUrl, type = :newType WHERE id = :id")
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

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else changeContentById(post.id, post.content)

    @Query(
        """
            UPDATE PostEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
                playSong = CASE WHEN playSong THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonSong(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
                playVideo = CASE WHEN playVideo THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonVideo(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)
}