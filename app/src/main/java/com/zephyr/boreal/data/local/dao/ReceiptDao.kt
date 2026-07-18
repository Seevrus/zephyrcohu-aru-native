package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
  @Query("SELECT * FROM receipts")
  fun getAllReceipts(): Flow<List<ReceiptEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReceipt(receipt: ReceiptEntity)

  @Query("DELETE FROM receipts")
  suspend fun clearReceipts()
}
