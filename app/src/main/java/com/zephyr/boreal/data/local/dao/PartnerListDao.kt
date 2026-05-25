package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.PartnerListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerListDao {
  @Query("SELECT * FROM partner_lists")
  fun getAllPartnerLists(): Flow<List<PartnerListEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPartnerLists(partnerLists: List<PartnerListEntity>)

  @Query("DELETE FROM partner_lists")
  suspend fun clearPartnerLists()
}
