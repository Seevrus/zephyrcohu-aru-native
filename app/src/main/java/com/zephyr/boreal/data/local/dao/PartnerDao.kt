package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.entities.PartnerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
  @Query("SELECT * FROM partners")
  fun getAllPartners(): Flow<List<PartnerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPartners(partners: List<PartnerEntity>)

  @Query("DELETE FROM partners")
  suspend fun clearPartners()
}
