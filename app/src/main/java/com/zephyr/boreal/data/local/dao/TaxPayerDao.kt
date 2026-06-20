package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.TaxPayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxPayerDao {
  @Query("SELECT * FROM tax_payers WHERE vatNumber LIKE :prefix || '%'")
  fun getTaxPayersByVatNumber(prefix: String): Flow<List<TaxPayerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTaxPayers(taxPayers: List<TaxPayerEntity>)

  @Query("DELETE FROM tax_payers WHERE vatNumber LIKE :prefix || '%'")
  suspend fun deleteTaxPayersByVatNumber(prefix: String)
}
