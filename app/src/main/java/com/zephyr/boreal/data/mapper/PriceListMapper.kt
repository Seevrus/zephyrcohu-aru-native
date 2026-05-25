package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.PriceListItemDto
import com.zephyr.boreal.api.dto.response.PriceListResponseDataDto
import com.zephyr.boreal.data.local.PriceListEntity
import com.zephyr.boreal.domain.model.PriceList
import com.zephyr.boreal.domain.model.PriceListItem

fun PriceListResponseDataDto.toDomain(): PriceList =
  PriceList(
    id = id,
    code = code,
    name = name,
    items = items.map { it.toDomain() },
  )

fun PriceListItemDto.toDomain(): PriceListItem =
  PriceListItem(
    itemId = itemId,
    articleNumber = articleNumber,
    netPrice = netPrice,
  )

fun PriceListResponseDataDto.toEntity(): PriceListEntity =
  PriceListEntity(
    id = id,
    code = code,
    name = name,
    items = items.map { it.toDomain() },
  )

fun PriceListEntity.toDomain(): PriceList =
  PriceList(
    id = id,
    code = code,
    name = name,
    items = items,
  )
