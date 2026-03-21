package com.zephyr.boreal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class BorealDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
}
