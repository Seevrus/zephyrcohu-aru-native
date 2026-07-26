package com.zephyr.boreal.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.StoreUserDto
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class StoreConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromUser(value: StoreUserDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toUser(value: String?): StoreUserDto? =
    value?.let {
      try {
        json.decodeFromString(it)
      } catch (e: SerializationException) {
        Log.w(TAG, "Failed to decode store user, degrading to null", e)
        // Shared by StoreEntity and StoreDetailsEntity — degrade both, an over-invalidation
        // trade-off accepted since neither entity can tell which table it's decoding for.
        CacheDecodeGuard.markDegraded(CacheGroups.STORES)
        CacheDecodeGuard.markDegraded(CacheGroups.STORE_DETAILS)
        null
      }
    }

  @TypeConverter
  fun fromStoreDetailsExpirationList(value: List<StoreDetailsExpiration>): String = json.encodeToString(value)

  @TypeConverter
  fun toStoreDetailsExpirationList(value: String): List<StoreDetailsExpiration> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode store details expirations, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.STORE_DETAILS)
      emptyList()
    }

  companion object {
    private const val TAG = "StoreConverters"
  }
}
