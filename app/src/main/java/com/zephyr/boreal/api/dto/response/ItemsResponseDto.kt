package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.DiscountType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpirationDto(
  val id: Int,
  val barcode: String? = null,
  val expiresAt: String,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class DiscountDto(
  val id: Int,
  val name: String,
  val type: DiscountType,
  val amount: Double? = null,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class ItemResponseDataDto(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val shortName: String,
  val unitName: String,
  val vatRate: String,
  val netPrice: Double,
  @SerialName("CNCode")
  val cnCode: String,
  val barcode: String? = null,
  val productCatalogCode: String,
  val expirations: List<ExpirationDto>,
  val discounts: List<DiscountDto>? = null,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class ItemsResponseDto(
  val data: List<ItemResponseDataDto>,
)
