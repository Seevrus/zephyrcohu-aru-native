package com.zephyr.boreal.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.api.dto.response.ExpirationDto
import com.zephyr.boreal.domain.model.PriceListItem
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class ItemConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromExpirationList(value: List<ExpirationDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toExpirationList(value: String): List<ExpirationDto> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode expirations, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ITEMS)
      emptyList()
    }

  @TypeConverter
  fun fromDiscountList(value: List<DiscountDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toDiscountList(value: String): List<DiscountDto> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode discounts, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.ITEMS)
      emptyList()
    }

  @TypeConverter
  fun fromPriceListItemList(value: List<PriceListItem>): String = json.encodeToString(value)

  @TypeConverter
  fun toPriceListItemList(value: String): List<PriceListItem> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode price list items, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.PRICE_LISTS)
      emptyList()
    }

  companion object {
    private const val TAG = "ItemConverters"
  }
}
