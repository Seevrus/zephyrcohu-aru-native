package com.zephyr.boreal.domain.model

data class OrderItem(
  val articleNumber: String,
  val name: String,
  val quantity: Double,
)

data class CreatedOrder(
  val id: Int,
  val partnerId: Int,
  val orderedAt: String,
  val items: List<OrderItem>,
)
