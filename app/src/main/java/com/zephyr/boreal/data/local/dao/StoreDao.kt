package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.entities.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
  @Query("SELECT * FROM stores")
  fun getAllStores(): Flow<List<StoreEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStores(stores: List<StoreEntity>)

  @Query("DELETE FROM stores")
  suspend fun clearStores()
}
