package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreDetailsExpiration(
  val itemId: Int,
  val articleNumber: String,
  val expirationId: Int,
  val expiresAt: String,
  val quantity: Double,
)

data class StoreDetails(
  val id: Int,
  val name: String,
  val code: String,
  val type: String,
  val state: String,
  val firstAvailableSerialNumber: Int? = null,
  val lastAvailableSerialNumber: Int? = null,
  val yearCode: Int? = null,
  val owner: StoreUser? = null,
  val user: StoreUser? = null,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpiration>,
)
