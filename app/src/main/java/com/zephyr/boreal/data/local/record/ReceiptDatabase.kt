package com.zephyr.boreal.data.local.record

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * `receipts` is the one durable record in this app — there's no `GET /receipts`, so a wiped table
 * is unrecoverable. Its own physical database, separate from the cache tables in
 * [com.zephyr.boreal.data.local.BorealDatabase], means the two migration policies (destructive for
 * a cache, real migrations for a record) are enforced by construction: this database is never
 * built with `.fallbackToDestructiveMigration(...)` (see `di/DatabaseModule.kt`), so a version
 * mismatch with no matching [androidx.room.migration.Migration] throws instead of wiping data.
 */
@Database(
  entities = [ReceiptEntity::class],
  version = 1,
  exportSchema = true,
)
@TypeConverters(ReceiptConverters::class)
abstract class ReceiptDatabase : RoomDatabase() {
  abstract fun receiptDao(): ReceiptDao
}
