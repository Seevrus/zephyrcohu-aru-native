package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.PriceListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceListDao {
  @Query("SELECT * FROM price_lists")
  fun getAllPriceLists(): Flow<List<PriceListEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPriceLists(priceLists: List<PriceListEntity>)

  @Query("DELETE FROM price_lists")
  suspend fun clearPriceLists()
}
