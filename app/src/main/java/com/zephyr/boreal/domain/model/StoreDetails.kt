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
  val address: String,
  val city: String,
  val postalCode: String,
  val country: String,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpiration>,
)
