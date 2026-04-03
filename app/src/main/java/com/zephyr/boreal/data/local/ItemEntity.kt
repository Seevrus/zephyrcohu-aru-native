package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.api.dto.response.ExpirationDto

@Entity(tableName = "items")
data class ItemEntity(
  @PrimaryKey val id: Int,
  val articleNumber: String,
  val name: String,
  val shortName: String,
  val unitName: String,
  val vatRate: String,
  val netPrice: Double,
  val cnCode: String,
  val barcode: String?,
  val productCatalogCode: String,
  val expirations: List<ExpirationDto>,
  val discounts: List<DiscountDto>,
  val createdAt: String,
  val updatedAt: String,
)
