package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.api.dto.response.ExpirationDto
import com.zephyr.boreal.api.dto.response.ItemResponseDataDto
import com.zephyr.boreal.api.dto.response.OtherItemResponseDataDto
import com.zephyr.boreal.data.local.ItemEntity
import com.zephyr.boreal.data.local.OtherItemEntity
import com.zephyr.boreal.domain.model.Discount
import com.zephyr.boreal.domain.model.Expiration
import com.zephyr.boreal.domain.model.Item
import com.zephyr.boreal.domain.model.OtherItem

fun ItemResponseDataDto.toEntity(): ItemEntity =
  ItemEntity(
    id = id,
    articleNumber = articleNumber,
    name = name,
    shortName = shortName,
    unitName = unitName,
    vatRate = vatRate,
    netPrice = netPrice,
    cnCode = cnCode,
    barcode = barcode,
    productCatalogCode = productCatalogCode,
    expirations = expirations,
    discounts = discounts ?: emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun OtherItemResponseDataDto.toEntity(): OtherItemEntity =
  OtherItemEntity(
    id = id,
    articleNumber = articleNumber,
    name = name,
    shortName = shortName,
    unitName = unitName,
    vatRate = vatRate,
    netPrice = netPrice,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun ItemEntity.toDomain(): Item =
  Item(
    id = id,
    articleNumber = articleNumber,
    name = name,
    shortName = shortName,
    unitName = unitName,
    vatRate = vatRate,
    netPrice = netPrice,
    cnCode = cnCode,
    barcode = barcode,
    productCatalogCode = productCatalogCode,
    expirations = expirations.map { it.toDomain() },
    discounts = discounts.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun OtherItemEntity.toDomain(): OtherItem =
  OtherItem(
    id = id,
    articleNumber = articleNumber,
    name = name,
    shortName = shortName,
    unitName = unitName,
    vatRate = vatRate,
    netPrice = netPrice,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun ExpirationDto.toDomain(): Expiration =
  Expiration(
    id = id,
    barcode = barcode,
    expiresAt = expiresAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun DiscountDto.toDomain(): Discount =
  Discount(
    id = id,
    name = name,
    type = type,
    amount = amount,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
