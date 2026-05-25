package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.StoreDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDetailsDao {
  @Query("SELECT * FROM store_details WHERE id = :storeId")
  fun getStoreDetails(storeId: Int): Flow<StoreDetailsEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStoreDetails(storeDetails: StoreDetailsEntity)

  @Query("DELETE FROM store_details WHERE id = :storeId")
  suspend fun deleteStoreDetails(storeId: Int)
}
