package com.zephyr.boreal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zephyr.boreal.data.local.CacheMetadataEntity

@Dao
interface CacheMetadataDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCacheMetadata(metadata: CacheMetadataEntity)

  @Query("SELECT * FROM cache_metadata WHERE queryKey = :queryKey")
  suspend fun getCacheMetadata(queryKey: String): CacheMetadataEntity?

  @Query("DELETE FROM cache_metadata WHERE queryKey = :queryKey")
  suspend fun clearCacheMetadata(queryKey: String)

  @Query("DELETE FROM cache_metadata WHERE fetchedAt < :threshold")
  suspend fun deleteOldEntries(threshold: Long)

  @Query("DELETE FROM cache_metadata")
  suspend fun clearAll()
}
