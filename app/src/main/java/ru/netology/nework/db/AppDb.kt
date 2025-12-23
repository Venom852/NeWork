package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nework.dao.AuthorIdDao
import ru.netology.nework.dao.ContentDraftDao
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.EventRemoteKeyDao
import ru.netology.nework.dao.JobDao
import ru.netology.nework.dao.JobMyDao
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.PostMyWallDao
import ru.netology.nework.dao.PostMyWallRemoteKeyDao
import ru.netology.nework.dao.PostRemoteKeyDao
import ru.netology.nework.dao.PostUserWallDao
import ru.netology.nework.dao.PostUserWallRemoteKeyDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.entity.AuthorIdEntity
import ru.netology.nework.entity.ContentDraftEntity
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.PostUserWallEntity
import ru.netology.nework.util.Converter
import ru.netology.nework.entity.PostRemoteKeyEntity
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.EventRemoteKeyEntity
import ru.netology.nework.entity.JobEntity
import ru.netology.nework.entity.JobMyEntity
import ru.netology.nework.entity.PostMyWallEntity
import ru.netology.nework.entity.PostUserRemoteKeyEntity
import ru.netology.nework.entity.PostMyWallRemoteKeyEntity
import ru.netology.nework.entity.UserEntity

@Database(
    entities = [PostEntity::class, ContentDraftEntity::class, PostRemoteKeyEntity::class, EventEntity::class, EventRemoteKeyEntity::class, UserEntity::class,
        JobEntity::class, AuthorIdEntity::class, JobMyEntity::class, PostUserRemoteKeyEntity::class, PostUserWallEntity::class, PostMyWallEntity::class,
        PostMyWallRemoteKeyEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class AppDb : RoomDatabase() {
    abstract val postDao: PostDao
    abstract val postRemoteKeyDao: PostRemoteKeyDao
    abstract val eventDao: EventDao
    abstract val eventRemoteKeyDao: EventRemoteKeyDao
    abstract val contentDraftDao: ContentDraftDao
    abstract val postMyWallDao: PostMyWallDao
    abstract val postMyWallRemoteKeyDao: PostMyWallRemoteKeyDao
    abstract val postUserWallDao: PostUserWallDao
    abstract val postUserWallRemoteKeyDao: PostUserWallRemoteKeyDao
    abstract val userDao: UserDao
    abstract val jobMyDao: JobMyDao
    abstract val jobDao: JobDao
    abstract val authorIdDao: AuthorIdDao
}