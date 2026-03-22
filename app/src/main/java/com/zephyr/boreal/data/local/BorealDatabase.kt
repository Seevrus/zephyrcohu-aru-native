package com.zephyr.boreal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zephyr.boreal.data.local.converters.ItemConverters
import com.zephyr.boreal.data.local.converters.PartnerConverters
import com.zephyr.boreal.data.local.converters.RoundConverters
import com.zephyr.boreal.data.local.converters.StoreConverters
import com.zephyr.boreal.data.local.converters.UserConverters
import com.zephyr.boreal.data.local.dao.ItemDao
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.local.dao.RoundDao
import com.zephyr.boreal.data.local.dao.StoreDao
import com.zephyr.boreal.data.local.dao.UserDao

@Database(
  entities = [
    UserEntity::class,
    PartnerEntity::class,
    ItemEntity::class,
    StoreEntity::class,
    RoundEntity::class,
  ],
  version = 8,
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

  abstract fun itemDao(): ItemDao

  abstract fun storeDao(): StoreDao

  abstract fun roundDao(): RoundDao
}
