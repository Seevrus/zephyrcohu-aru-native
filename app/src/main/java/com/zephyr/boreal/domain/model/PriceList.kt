package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

data class PriceList(
  val id: Int,
  val code: String? = null,
  val name: String,
  val items: List<PriceListItem>,
)

@Serializable
data class PriceListItem(
  val itemId: Int,
  val articleNumber: String,
  val netPrice: Double,
)
