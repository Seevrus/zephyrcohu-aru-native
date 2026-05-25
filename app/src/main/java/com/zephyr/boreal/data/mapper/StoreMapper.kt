package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.StoreDetailsDataDto
import com.zephyr.boreal.api.dto.response.StoreDetailsExpirationDto
import com.zephyr.boreal.api.dto.response.StoreResponseDataDto
import com.zephyr.boreal.data.local.StoreDetailsEntity
import com.zephyr.boreal.data.local.StoreEntity
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import java.util.Calendar

fun StoreResponseDataDto.toEntity(): StoreEntity =
  StoreEntity(
    id = id,
    code = code,
    name = name,
    type = type,
    state = state,
    firstAvailableSerialNumber = firstAvailableSerialNumber ?: -1,
    lastAvailableSerialNumber = lastAvailableSerialNumber ?: -1,
    yearCode = yearCode ?: (Calendar.getInstance().get(Calendar.YEAR) % 100),
    owner = owner,
    user = user,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun StoreEntity.toDomain(): Store =
  Store(
    id = id,
    code = code,
    name = name,
    type = type,
    state = state,
    firstAvailableSerialNumber = firstAvailableSerialNumber,
    lastAvailableSerialNumber = lastAvailableSerialNumber,
    yearCode = yearCode,
    owner = owner?.toDomain(),
    user = user?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun StoreDetailsExpirationDto.toDomain(): StoreDetailsExpiration =
  StoreDetailsExpiration(
    itemId = itemId,
    articleNumber = articleNumber,
    expirationId = expirationId,
    expiresAt = expiresAt,
    quantity = quantity,
  )

fun StoreDetailsDataDto.toDomain(): StoreDetails =
  StoreDetails(
    id = id,
    name = name,
    code = code,
    address = address,
    city = city,
    postalCode = postalCode,
    country = country,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expirations = expirations.map { it.toDomain() },
  )

fun StoreDetailsDataDto.toEntity(): StoreDetailsEntity =
  StoreDetailsEntity(
    id = id,
    name = name,
    code = code,
    address = address,
    city = city,
    postalCode = postalCode,
    country = country,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expirations = expirations.map { it.toDomain() },
  )

fun StoreDetailsEntity.toDomain(): StoreDetails =
  StoreDetails(
    id = id,
    name = name,
    code = code,
    address = address,
    city = city,
    postalCode = postalCode,
    country = country,
    createdAt = createdAt,
    updatedAt = updatedAt,
    expirations = expirations,
  )
