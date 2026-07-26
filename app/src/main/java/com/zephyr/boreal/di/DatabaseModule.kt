package com.zephyr.boreal.di

import android.content.Context
import androidx.room.Room
import com.zephyr.boreal.data.local.BorealDatabase
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.ItemDao
import com.zephyr.boreal.data.local.dao.OtherItemDao
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.local.dao.PartnerListDao
import com.zephyr.boreal.data.local.dao.PriceListDao
import com.zephyr.boreal.data.local.dao.RoundDao
import com.zephyr.boreal.data.local.dao.StoreDao
import com.zephyr.boreal.data.local.dao.StoreDetailsDao
import com.zephyr.boreal.data.local.dao.TaxPayerDao
import com.zephyr.boreal.data.local.dao.UserDao
import com.zephyr.boreal.data.local.record.ReceiptDao
import com.zephyr.boreal.data.local.record.ReceiptDatabase
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
      ).fallbackToDestructiveMigration(true)
      .build()

  /**
   * `receipts` is a durable record, not a cache — no `.fallbackToDestructiveMigration(...)` here.
   * A version bump with no matching [androidx.room.migration.Migration] throws instead of wiping
   * data, per `rn-migration-tasks/03f-persistence-durability-classes.md`.
   */
  @Provides
  @Singleton
  fun provideReceiptDatabase(
    @ApplicationContext context: Context,
  ): ReceiptDatabase =
    Room
      .databaseBuilder(
        context,
        ReceiptDatabase::class.java,
        "receipts_database",
      ).build()

  @Provides
  fun provideUserDao(database: BorealDatabase): UserDao = database.userDao()

  @Provides
  fun providePartnerDao(database: BorealDatabase): PartnerDao = database.partnerDao()

  @Provides
  fun providePartnerListDao(database: BorealDatabase): PartnerListDao = database.partnerListDao()

  @Provides
  fun providePriceListDao(database: BorealDatabase): PriceListDao = database.priceListDao()

  @Provides
  fun provideStoreDetailsDao(database: BorealDatabase): StoreDetailsDao = database.storeDetailsDao()

  @Provides
  fun provideTaxPayerDao(database: BorealDatabase): TaxPayerDao = database.taxPayerDao()

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

  @Provides
  fun provideReceiptDao(database: ReceiptDatabase): ReceiptDao = database.receiptDao()
}
