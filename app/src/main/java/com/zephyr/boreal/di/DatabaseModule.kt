package com.zephyr.boreal.di

import android.content.Context
import androidx.room.Room
import com.zephyr.boreal.data.local.BorealDatabase
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.ItemDao
import com.zephyr.boreal.data.local.dao.OtherItemDao
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.local.dao.RoundDao
import com.zephyr.boreal.data.local.dao.StoreDao
import com.zephyr.boreal.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(
    @ApplicationContext context: Context,
  ): BorealDatabase =
    Room
      .databaseBuilder(
        context,
        BorealDatabase::class.java,
        "boreal_database",
      ).fallbackToDestructiveMigration()
      .build()

  @Provides
  fun provideUserDao(database: BorealDatabase): UserDao = database.userDao()

  @Provides
  fun providePartnerDao(database: BorealDatabase): PartnerDao = database.partnerDao()

  @Provides
  fun provideItemDao(database: BorealDatabase): ItemDao = database.itemDao()

  @Provides
  fun provideOtherItemDao(database: BorealDatabase): OtherItemDao = database.otherItemDao()

  @Provides
  fun provideStoreDao(database: BorealDatabase): StoreDao = database.storeDao()

  @Provides
  fun provideRoundDao(database: BorealDatabase): RoundDao = database.roundDao()

  @Provides
  fun provideCacheMetadataDao(database: BorealDatabase): CacheMetadataDao = database.cacheMetadataDao()
}
