package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.UserState
import kotlinx.serialization.Serializable

@Serializable
data class StoreResponseDataDto(
  val id: Int,
  val code: String,
  val name: String,
  val type: StoreType,
  val state: UserState,
  val firstAvailableSerialNumber: Int,
  val lastAvailableSerialNumber: Int,
  val yearCode: Int,
  val owner: UserDto? = null,
  val user: UserDto? = null,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class StoresResponseDto(
  val data: List<StoreResponseDataDto>,
)
