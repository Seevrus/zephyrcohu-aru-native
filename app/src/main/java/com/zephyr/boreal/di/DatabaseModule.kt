package com.zephyr.boreal.di

import android.content.Context
import androidx.room.Room
import com.zephyr.boreal.data.local.BorealDatabase
import com.zephyr.boreal.data.local.UserDao
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
      ).build()

  @Provides
  fun provideUserDao(database: BorealDatabase): UserDao = database.userDao()
}
