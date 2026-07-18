package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptUserDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReceiptConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromReceiptVendorDto(value: ReceiptVendorDto): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptVendorDto(value: String): ReceiptVendorDto = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptBuyerDto(value: ReceiptBuyerDto): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptBuyerDto(value: String): ReceiptBuyerDto = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptUserDto(value: ReceiptUserDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toReceiptUserDto(value: String?): ReceiptUserDto? = value?.let { json.decodeFromString(it) }

  @TypeConverter
  fun fromReceiptItemList(value: List<ReceiptItemDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptItemList(value: String): List<ReceiptItemDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptOtherItemList(value: List<ReceiptOtherItemDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptOtherItemList(value: String): List<ReceiptOtherItemDto> = json.decodeFromString(value)

  @TypeConverter
  fun fromReceiptVatAmountList(value: List<ReceiptVatAmountDto>): String = json.encodeToString(value)

  @TypeConverter
  fun toReceiptVatAmountList(value: String): List<ReceiptVatAmountDto> = json.decodeFromString(value)
}
