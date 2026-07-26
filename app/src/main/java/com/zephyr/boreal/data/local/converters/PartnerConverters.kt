package com.zephyr.boreal.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.BasePriceListDto
import com.zephyr.boreal.api.dto.response.PartnerLocationDto
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.PartnerLocation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class PartnerConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromPartnerLocationList(value: List<PartnerLocationDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toPartnerLocationList(value: String): List<PartnerLocationDto> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode partner locations, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.PARTNERS)
      emptyList()
    }

  @TypeConverter
  fun fromIntList(value: List<Int>): String = json.encodeToString(value)

  @TypeConverter
  fun toIntList(value: String): List<Int> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode int list, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.PARTNER_LISTS)
      emptyList()
    }

  @TypeConverter
  fun fromPriceList(value: BasePriceListDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toPriceList(value: String?): BasePriceListDto? =
    value?.let {
      try {
        json.decodeFromString(it)
      } catch (e: SerializationException) {
        Log.w(TAG, "Failed to decode partner price list, degrading to null", e)
        CacheDecodeGuard.markDegraded(CacheGroups.PARTNERS)
        null
      }
    }

  @TypeConverter
  fun fromInvoiceType(value: InvoiceType): String = value.name

  @TypeConverter
  fun toInvoiceType(value: String): InvoiceType = InvoiceType.valueOf(value)

  @TypeConverter
  fun fromLocationsMap(value: Map<String, PartnerLocation>): String = json.encodeToString(value)

  @TypeConverter
  fun toLocationsMap(value: String): Map<String, PartnerLocation> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode tax payer locations, degrading to empty map", e)
      CacheDecodeGuard.markDegraded(CacheGroups.TAX_NUMBER_SEARCH)
      emptyMap()
    }

  companion object {
    private const val TAG = "PartnerConverters"
  }
}
