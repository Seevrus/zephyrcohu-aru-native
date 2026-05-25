package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.api.dto.response.ExpirationDto
import com.zephyr.boreal.domain.model.PriceListItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ItemConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromExpirationList(value: List<ExpirationDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toExpirationList(value: String): List<ExpirationDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromDiscountList(value: List<DiscountDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toDiscountList(value: String): List<DiscountDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromPriceListItemList(value: List<PriceListItem>): String = json.encodeToString(value)

  @TypeConverter
  fun toPriceListItemList(value: String): List<PriceListItem> = json.decodeFromString(value)
}
