package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.dto.Attachment
import ru.netology.nework.entity.AttachmentEmbeddable
import ru.netology.nework.entity.ContentDraftEntity
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

    @Query("SELECT COUNT(*) FROM PostEntity")
    suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE PostEntity Set content = :text WHERE id = :id")
    suspend fun changeContentById(id: Long, text: String)

    @Query("UPDATE PostEntity Set id = :newId, url = :url, type = :type WHERE id = :id")
    suspend fun changeIdPostById(id: Long, newId: Long, url: String?, type: AttachmentType?)

//    @Query("UPDATE PostEntity Set viewed = 1 WHERE viewed = 0")
//    suspend fun browse()

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else changeContentById(post.id, post.content.toString())

    @Query(
        """
            UPDATE PostEntity SET
                shared = shared + 1,
                toShare = 1
            WHERE id = :id;
        """
    )
    suspend fun toShareById(id: Long)

    @Query(
        """
            UPDATE PostEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("DELETE FROM PostEntity")
    suspend fun removeAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(contentDraftEntity: ContentDraftEntity)

    suspend fun saveDraft(draft: String) = insertDraft(ContentDraftEntity(contentDraft = draft))

    @Query("DELETE FROM ContentDraftEntity")
    suspend fun removeDraft()

    @Query("SELECT * FROM ContentDraftEntity")
    suspend fun getDraft(): String?
}