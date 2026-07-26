package com.zephyr.boreal.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.RoundPartnerListDto
import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import com.zephyr.boreal.api.dto.response.RoundStoreDto
import com.zephyr.boreal.api.dto.response.RoundUserDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class RoundConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromRoundReceiptList(value: List<RoundReceiptDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundReceiptList(value: String): List<RoundReceiptDto> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode round receipts, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ROUNDS)
      emptyList()
    }

  @TypeConverter
  fun fromRoundUser(value: RoundUserDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundUser(value: String): RoundUserDto =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode round user, degrading to placeholder", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ROUNDS)
      RoundUserDto(id = 0, userName = "", name = "")
    }

  @TypeConverter
  fun fromRoundStore(value: RoundStoreDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundStore(value: String): RoundStoreDto =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode round store, degrading to placeholder", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ROUNDS)
      RoundStoreDto(id = 0, code = "", name = "")
    }

  @TypeConverter
  fun fromRoundPartnerList(value: RoundPartnerListDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundPartnerList(value: String): RoundPartnerListDto =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode round partner list, degrading to placeholder", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ROUNDS)
      RoundPartnerListDto(id = 0, name = "")
    }

  companion object {
    private const val TAG = "RoundConverters"
  }
}
