package com.zephyr.boreal.data.local.converters

import androidx.room.TypeConverter
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserConverters {
  private val json = Json { ignoreUnknownKeys = true }

  @TypeConverter
  fun fromUserRoleList(value: List<UserRole>): String = json.encodeToString(value)

  @TypeConverter
  fun toUserRoleList(value: String): List<UserRole> = json.decodeFromString(value)

  @TypeConverter
  fun fromUserState(value: UserState): String = value.name

  @TypeConverter
  fun toUserState(value: String): UserState = UserState.valueOf(value)
}
