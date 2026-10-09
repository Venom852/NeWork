package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.enumeration.AttachmentType

@Dao
interface PostMyWallDao {
    @Query("SELECT * FROM PostMyWallEntity ORDER BY id DESC")
    fun getAll(): List<PostMyWallEntity>

    @Query("SELECT * FROM PostMyWallEntity ORDER BY id DESC")
    fun getAllFlow(): Flow<List<PostMyWallEntity>>

    @Query("SELECT * FROM PostMyWallEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, PostMyWallEntity>

    @Query("SELECT COUNT(*) == 0 FROM PostMyWallEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostMyWallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostMyWallEntity>)

    @Query("UPDATE PostMyWallEntity Set content = :text, mentionIds = :newMentionIds, " +
            "users = :newUsers, coords = :newCoordinates WHERE id = :id")
    suspend fun changeContentById(
        id: Long,
        text: String,
        newMentionIds: Set<Long>,
        newUsers: Map<Long, UserPreview>,
        newCoordinates: Coordinates?
    )

    @Query("UPDATE PostMyWallEntity Set id = :newId, " +
//            "author = :newAuthor, " +
            "authorId = :newAuthorId," +
            " authorAvatar = :newAuthorAvatar, authorJob = :newAuthorJob, mentionedMe = :newMentionedMe," +
            " url = :newUrl, type = :newType WHERE id = :id")
    suspend fun changeIdPostById(
        id: Long,
        newId: Long,
//        newAuthor: String,
        newAuthorId: Long,
        newAuthorAvatar: String?,
        newAuthorJob: String?,
        newMentionedMe: Boolean,
        newUrl: String?,
        newType: AttachmentType?
    )

    suspend fun save(post: PostMyWallEntity) =
        if (post.id == 0L) insert(post) else changeContentById(post.id, post.content,
            post.mentionIds, post.users, post.coords)

    @Query(
        """
            UPDATE PostMyWallEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END,
                likeOwnerIds = :likeOwnerIds,
                users = :users
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long, likeOwnerIds: Set<Long>, users: Map<Long, UserPreview>)

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