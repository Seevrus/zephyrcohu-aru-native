package com.zephyr.boreal.data.local.record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
  @Query("SELECT * FROM receipts ORDER BY id DESC")
  fun getAllReceipts(): Flow<List<ReceiptEntity>>

  @Query("SELECT * FROM receipts WHERE serialNumber = :serialNumber LIMIT 1")
  fun findBySerialNumber(serialNumber: Int): Flow<ReceiptEntity?>

  @Query(
    """
    SELECT MAX(maxVal) FROM (
      SELECT MAX(serialNumber) AS maxVal FROM receipts
      UNION ALL
      SELECT MAX(cancelSerialNumber) AS maxVal FROM receipts
    )
    """,
  )
  suspend fun getMaxSerialNumber(): Int?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReceipt(receipt: ReceiptEntity)

  @Query("DELETE FROM receipts")
  suspend fun clearReceipts()

  @Query("UPDATE receipts SET isPrinted = :isPrinted WHERE id = :id")
  suspend fun updatePrintedState(
    id: Int,
    isPrinted: Boolean,
  )
}
