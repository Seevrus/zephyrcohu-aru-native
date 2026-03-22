package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.RoundPartnerListDto
import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import com.zephyr.boreal.api.dto.response.RoundStoreDto
import com.zephyr.boreal.api.dto.response.RoundUserDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoundConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromRoundReceiptList(value: List<RoundReceiptDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundReceiptList(value: String): List<RoundReceiptDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromRoundUser(value: RoundUserDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundUser(value: String): RoundUserDto = json.decodeFromString(value)

  @TypeConverter
  fun fromRoundStore(value: RoundStoreDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundStore(value: String): RoundStoreDto = json.decodeFromString(value)

  @TypeConverter
  fun fromRoundPartnerList(value: RoundPartnerListDto): String = json.encodeToString(value)

  @TypeConverter
  fun toRoundPartnerList(value: String): RoundPartnerListDto = json.decodeFromString(value)
}
