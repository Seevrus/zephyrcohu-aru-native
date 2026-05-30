package com.zephyr.boreal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zephyr.boreal.data.local.converters.ItemConverters
import com.zephyr.boreal.data.local.converters.PartnerConverters
import com.zephyr.boreal.data.local.converters.RoundConverters
import com.zephyr.boreal.data.local.converters.StoreConverters
import com.zephyr.boreal.data.local.converters.UserConverters
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

@Database(
  entities = [
    UserEntity::class,
    PartnerEntity::class,
    PartnerListEntity::class,
    ItemEntity::class,
    OtherItemEntity::class,
    StoreEntity::class,
    RoundEntity::class,
    CacheMetadataEntity::class,
    PriceListEntity::class,
    StoreDetailsEntity::class,
    TaxPayerEntity::class,
  ],
  version = 14,
  exportSchema = false,
)
@TypeConverters(
  PartnerConverters::class,
  ItemConverters::class,
  RoundConverters::class,
  StoreConverters::class,
  UserConverters::class,
)
abstract class BorealDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao

  abstract fun partnerDao(): PartnerDao

  abstract fun partnerListDao(): PartnerListDao

  abstract fun priceListDao(): PriceListDao

  abstract fun storeDetailsDao(): StoreDetailsDao

  abstract fun taxPayerDao(): TaxPayerDao

  abstract fun itemDao(): ItemDao

  abstract fun otherItemDao(): OtherItemDao

  abstract fun storeDao(): StoreDao

  abstract fun roundDao(): RoundDao

  abstract fun cacheMetadataDao(): CacheMetadataDao
}
