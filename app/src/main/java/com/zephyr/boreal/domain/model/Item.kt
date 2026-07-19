package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

data class Item(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val shortName: String,
  val unitName: String,
  val vatRate: String,
  val netPrice: Double,
  val cnCode: String,
  val barcode: String? = null,
  val productCatalogCode: String,
  val expirations: List<Expiration> = emptyList(),
  val discounts: List<Discount> = emptyList(),
  val createdAt: String,
  val updatedAt: String,
)

data class Expiration(
  val id: Int,
  val barcode: String? = null,
  val expiresAt: String, // XXXXXX
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class Discount(
  val id: Int,
  val name: String,
  val type: DiscountType,
  val amount: Double? = null,
  val createdAt: String,
  val updatedAt: String,
)
