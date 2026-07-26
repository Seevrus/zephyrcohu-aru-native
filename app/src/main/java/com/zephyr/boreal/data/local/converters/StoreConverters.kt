package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.StoreUserDto
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import kotlinx.serialization.encodeToString

class StoreConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromUser(value: StoreUserDto?): String? = value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toUser(value: String?): StoreUserDto? = value?.let { json.decodeFromString(it) }

  @TypeConverter
  fun fromStoreDetailsExpirationList(value: List<StoreDetailsExpiration>): String = json.encodeToString(value)

  @TypeConverter
  fun toStoreDetailsExpirationList(value: String): List<StoreDetailsExpiration> = json.decodeFromString(value)
}
