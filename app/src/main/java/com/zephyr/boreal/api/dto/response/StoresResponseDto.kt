package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import kotlinx.serialization.Serializable

@Serializable
data class StoreUserDto(
  val id: Int,
  val userName: String,
  val state: UserState,
  val name: String,
  val phoneNumber: String? = null,
  val isDev: Boolean,
  val roles: List<UserRole>,
  val storeInUseId: Int? = null,
  val storeOwnedId: Int? = null,
  val lastActive: String,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class StoreResponseDataDto(
  val id: Int,
  val code: String,
  val name: String,
  val type: StoreType,
  val state: UserState,
  val firstAvailableSerialNumber: Int?,
  val lastAvailableSerialNumber: Int?,
  val yearCode: Int?,
  val owner: StoreUserDto? = null,
  val user: StoreUserDto? = null,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class StoresResponseDto(
  val data: List<StoreResponseDataDto>,
)
