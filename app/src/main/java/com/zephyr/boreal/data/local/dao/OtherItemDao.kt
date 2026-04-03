package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.OtherItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OtherItemDao {
  @Query("SELECT * FROM other_items")
  fun getAllOtherItems(): Flow<List<OtherItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOtherItems(items: List<OtherItemEntity>)

  @Query("DELETE FROM other_items")
  suspend fun clearOtherItems()
}
