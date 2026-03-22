package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
  @Query("SELECT * FROM items")
  fun getAllItems(): Flow<List<ItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItems(items: List<ItemEntity>)

  @Query("DELETE FROM items")
  suspend fun clearItems()
}
