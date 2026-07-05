package com.zephyr.boreal.domain.model

data class OrderItem(
  val articleNumber: String,
  val name: String,
  val quantity: Double,
)

/**
 * An order while still under construction, built when items are confirmed on the select-items
 * screen and submitted later during receipt finalization (see ReviewItemsViewModel).
 */
data class DraftOrder(
  val partnerId: Int,
  val orderedAt: String,
  val items: List<OrderItem>,
)

data class CreatedOrder(
  val id: Int,
  val partnerId: Int,
  val orderedAt: String,
  val items: List<OrderItem>,
)
