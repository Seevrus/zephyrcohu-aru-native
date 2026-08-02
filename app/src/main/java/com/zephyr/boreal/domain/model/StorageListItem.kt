package com.zephyr.boreal.domain.model

data class StorageListItem(
  val itemId: Int,
  val expirationId: Int,
  val articleNumber: String,
  val name: String,
  val expiresAt: String,
  val itemBarcode: String,
  val expirationBarcode: String,
  val unitName: String,
  val primaryStoreQuantity: Double?,
  val originalQuantity: Double?,
  val quantityChange: Int? = null,
)
