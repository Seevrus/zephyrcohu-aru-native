package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
  @PrimaryKey val queryKey: String,
  val fetchedAt: Long,
)
