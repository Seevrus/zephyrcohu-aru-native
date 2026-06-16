package com.zephyr.boreal.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class StoreDetailsExpirationDto(
  val itemId: Int,
  val articleNumber: String,
  val expirationId: Int,
  val expiresAt: String,
  val quantity: Double,
)

@Serializable
data class StoreDetailsResponseDto(
  val data: StoreDetailsDataDto,
)

@Serializable
data class StoreDetailsDataDto(
  val id: Int,
  val name: String,
  val code: String,
  val type: String,
  val state: String,
  val firstAvailableSerialNumber: Int? = null,
  val lastAvailableSerialNumber: Int? = null,
  val yearCode: Int? = null,
  val owner: StoreUserDto? = null,
  val user: StoreUserDto? = null,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpirationDto> = emptyList(),
)
