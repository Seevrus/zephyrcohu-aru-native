package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.UserDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StoreConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromUser(value: UserDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toUser(value: String?): UserDto? = value?.let { json.decodeFromString(it) }
}
