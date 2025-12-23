package ru.netology.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.enumeration.AttachmentType

@Dao
interface EventDao {
    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun getAll(): List<EventEntity>

    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun getPagingSource(): PagingSource<Int, EventEntity>

    @Query("SELECT * FROM EventEntity WHERE id = :id")
    fun getEvent(id: Long): EventEntity

//    @Query("SELECT * FROM EventEntity WHERE viewed == 1 ORDER BY id DESC")
//    fun getAll(): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) == 0 FROM EventEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT COUNT(*) FROM EventEntity")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("UPDATE EventEntity Set content = :text WHERE id = :id")
    suspend fun changeContentById(id: Long, text: String)

    @Query("UPDATE EventEntity Set id = :newId, author = :newAuthor, authorId = :newAuthorId, authorAvatar = :newAuthorAvatar, authorJob = :newAuthorJob, url = :newUrl, type = :newType WHERE id = :id")
    suspend fun changeIdEventById(
        id: Long,
        newId: Long,
        newAuthor: String,
        newAuthorId: Long,
        newAuthorAvatar: String?,
        newAuthorJob: String?,
        newUrl: String?,
        newType: AttachmentType?
    )

//    @Query("UPDATE EventEntity Set viewed = 1 WHERE viewed = 0")
//    suspend fun browse()

    suspend fun save(event: EventEntity) =
        if (event.id == 0L) insert(event) else changeContentById(event.id, event.content)

    @Query(
        """
            UPDATE EventEntity SET
                shared = shared + 1,
                toShare = 1
            WHERE id = :id;
        """
    )
    suspend fun toShareById(id: Long)

    @Query(
        """
            UPDATE EventEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun likeById(id: Long)

    @Query(
        """
            UPDATE EventEntity SET
                playSong = CASE WHEN playSong THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonSong(id: Long)

    @Query(
        """
            UPDATE EventEntity SET
                playVideo = CASE WHEN playVideo THEN 0 ELSE 1 END
            WHERE id = :id;
        """
    )
    suspend fun playButtonVideo(id: Long)

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("DELETE FROM EventEntity")
    suspend fun removeAll()
}