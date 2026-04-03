package com.zephyr.boreal.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class OtherItemResponseDataDto(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val shortName: String,
  val unitName: String,
  val vatRate: String,
  val netPrice: Double,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class OtherItemsResponseDto(
  val data: List<OtherItemResponseDataDto>,
)
