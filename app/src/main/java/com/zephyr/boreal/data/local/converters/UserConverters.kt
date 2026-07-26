package com.zephyr.boreal.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.zephyr.boreal.api.dto.response.CompanyDto
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

class UserConverters {
  private val json = PersistenceJson.instance

  @TypeConverter
  fun fromUserRoleList(value: List<UserRole>): String = json.encodeToString(value)

  @TypeConverter
  fun toUserRoleList(value: String): List<UserRole> =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode user roles, degrading to empty list", e)
      CacheDecodeGuard.markDegraded(CacheGroups.CURRENT_USER)
      emptyList()
    }

  @TypeConverter
  fun fromUserState(value: UserState): String = value.name

  @TypeConverter
  fun toUserState(value: String): UserState = UserState.valueOf(value)

  @TypeConverter
  fun fromCompanyDto(value: CompanyDto): String = json.encodeToString(value)

  @TypeConverter
  fun toCompanyDto(value: String): CompanyDto =
    try {
      json.decodeFromString(value)
    } catch (e: SerializationException) {
      Log.w(TAG, "Failed to decode company, degrading to placeholder", e)
      CacheDecodeGuard.markDegraded(CacheGroups.CURRENT_USER)
      CompanyDto(
        id = 0,
        code = "",
        name = "",
        country = "",
        postalCode = "",
        city = "",
        address = "",
        felir = "",
        vatNumber = "",
        iban = "",
        bankAccount = "",
      )
    }

  @TypeConverter
  fun fromRoundResponseDataDto(value: com.zephyr.boreal.api.dto.response.RoundResponseDataDto?): String? =
    value?.let { json.encodeToString(it) }

  @TypeConverter
  fun toRoundResponseDataDto(value: String?): com.zephyr.boreal.api.dto.response.RoundResponseDataDto? =
    value?.let {
      try {
        json.decodeFromString(it)
      } catch (e: SerializationException) {
        Log.w(TAG, "Failed to decode round response data, degrading to null", e)
        CacheDecodeGuard.markDegraded(CacheGroups.CURRENT_USER)
        null
      }
    }

  companion object {
    private const val TAG = "UserConverters"
  }
}
