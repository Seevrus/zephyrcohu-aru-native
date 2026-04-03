package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.request.OrderItemDto
import com.zephyr.boreal.api.dto.response.CreatedOrderResponseDataDto
import com.zephyr.boreal.domain.model.CreatedOrder
import com.zephyr.boreal.domain.model.OrderItem

fun OrderItemDto.toDomain(): OrderItem =
  OrderItem(
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
  )

fun CreatedOrderResponseDataDto.toDomain(): CreatedOrder =
  CreatedOrder(
    id = id,
    partnerId = partnerId,
    orderedAt = orderedAt,
    items = items.map { it.toDomain() },
  )
