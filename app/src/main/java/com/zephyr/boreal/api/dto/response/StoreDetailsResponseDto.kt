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
  val address: String,
  val city: String,
  val postalCode: String,
  val country: String,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpirationDto>,
)
