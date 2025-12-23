package ru.netology.nework.dao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.db.AppDb

@InstallIn(SingletonComponent::class)
@Module
object DaoModule {
    @Provides
    fun providePostDao(appDb: AppDb): PostDao = appDb.postDao

    @Provides
    fun providePostRemoteKeyDao(appDb: AppDb): PostRemoteKeyDao = appDb.postRemoteKeyDao

    @Provides
    fun provideEventDao(appDb: AppDb): EventDao = appDb.eventDao

    @Provides
    fun provideEventRemoteKeyDao(appDb: AppDb): EventRemoteKeyDao = appDb.eventRemoteKeyDao

    @Provides
    fun provideContentDraftDao(appDb: AppDb): ContentDraftDao = appDb.contentDraftDao

    @Provides
    fun providePostMyWallDao(appDb: AppDb): PostMyWallDao = appDb.postMyWallDao

    @Provides
    fun providePostMyWallRemoteKeyDao(appDb: AppDb): PostMyWallRemoteKeyDao = appDb.postMyWallRemoteKeyDao

    @Provides
    fun providePostUserWallDao(appDb: AppDb): PostUserWallDao = appDb.postUserWallDao

    @Provides
    fun providePostUserWallRemoteKeyDao(appDb: AppDb): PostUserWallRemoteKeyDao = appDb.postUserWallRemoteKeyDao

    @Provides
    fun provideUserDao(appDb: AppDb): UserDao = appDb.userDao

    @Provides
    fun provideJobMyDao(appDb: AppDb): JobMyDao = appDb.jobMyDao

    @Provides
    fun provideJobDao(appDb: AppDb): JobDao = appDb.jobDao

    @Provides
    fun provideAuthorIdDao(appDb: AppDb): AuthorIdDao = appDb.authorIdDao
}