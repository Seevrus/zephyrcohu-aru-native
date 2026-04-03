package com.zephyr.boreal.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PriceListItemDto(
  val itemId: Int,
  val articleNumber: String,
  val netPrice: Double,
)

@Serializable
data class PriceListResponseDataDto(
  val id: Int,
  val code: String? = null,
  val name: String,
  val items: List<PriceListItemDto>,
)

@Serializable
data class PriceListResponseDto(
  val data: List<PriceListResponseDataDto>,
)
