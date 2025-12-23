package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.entity.ContentDraftEntity

@Dao
interface ContentDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(contentDraftEntity: ContentDraftEntity)

    suspend fun saveDraft(draft: String) = insertDraft(ContentDraftEntity(contentDraft = draft))

    @Query("DELETE FROM ContentDraftEntity")
    suspend fun removeDraft()

    @Query("SELECT * FROM ContentDraftEntity")
    suspend fun getDraft(): String?
}