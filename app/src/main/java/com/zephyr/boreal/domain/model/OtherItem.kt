package com.zephyr.boreal.domain.model

data class OtherItem(
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
