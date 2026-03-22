package com.zephyr.boreal.domain.model

data class PriceList(
  val id: Int,
  val code: String? = null,
  val name: String,
  val items: List<PriceListItem>,
)

data class PriceListItem(
  val itemId: Int,
  val articleNumber: String,
  val netPrice: Double,
)
