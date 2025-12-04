package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.entity.ContentDraftEntity
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.util.Converter
import ru.netology.nework.entity.PostRemoteKeyEntity

@Database(entities = [PostEntity::class, ContentDraftEntity::class, PostRemoteKeyEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converter::class)
abstract class AppDb : RoomDatabase() {
    abstract val postDao: PostDao
    abstract val postRemoteKeyDao: PostRemoteKeyDao
}