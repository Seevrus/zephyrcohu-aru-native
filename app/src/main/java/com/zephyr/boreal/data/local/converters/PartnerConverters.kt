package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.BasePriceListDto
import com.zephyr.boreal.api.dto.response.PartnerLocationDto
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.PartnerLocation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PartnerConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromPartnerLocationList(value: List<PartnerLocationDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toPartnerLocationList(value: String): List<PartnerLocationDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromIntList(value: List<Int>): String = json.encodeToString(value)

  @TypeConverter
  fun toIntList(value: String): List<Int> = json.decodeFromString(value)

  @TypeConverter
  fun fromPriceList(value: BasePriceListDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toPriceList(value: String?): BasePriceListDto? = value?.let { json.decodeFromString(it) }

  @TypeConverter
  fun fromInvoiceType(value: InvoiceType): String = value.name

  @TypeConverter
  fun toInvoiceType(value: String): InvoiceType = InvoiceType.valueOf(value)

  @TypeConverter
  fun fromLocationsMap(value: Map<String, PartnerLocation>): String = json.encodeToString(value)

  @TypeConverter
  fun toLocationsMap(value: String): Map<String, PartnerLocation> = json.decodeFromString(value)
}
