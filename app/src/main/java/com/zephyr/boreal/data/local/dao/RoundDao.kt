package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.entities.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
  @Query("SELECT * FROM rounds")
  fun getAllRounds(): Flow<List<RoundEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRounds(rounds: List<RoundEntity>)

  @Query("DELETE FROM rounds")
  suspend fun clearRounds()
}
