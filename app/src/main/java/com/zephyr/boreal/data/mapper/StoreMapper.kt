package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.StoreResponseDataDto
import com.zephyr.boreal.data.local.StoreEntity
import com.zephyr.boreal.domain.model.Store

fun StoreResponseDataDto.toEntity(): StoreEntity =
  StoreEntity(
    id = id,
    code = code,
    name = name,
    type = type,
    state = state,
    firstAvailableSerialNumber = firstAvailableSerialNumber,
    lastAvailableSerialNumber = lastAvailableSerialNumber,
    yearCode = yearCode,
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
