package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.LoginResponseDto
import com.zephyr.boreal.api.dto.response.SelectStoreResponseTypeDto
import com.zephyr.boreal.api.dto.response.UserDto
import com.zephyr.boreal.data.local.UserEntity
import com.zephyr.boreal.domain.model.User

fun LoginResponseDto.toDomain(): User =
  User(
    id = id,
    userName = userName,
    state = state,
    name = name,
    phoneNumber = phoneNumber,
    isDev = isDev,
    roles = roles,
    storeInUseId = storeInUseId,
    storeOwnedId = storeOwnedId,
    lastActive = lastActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun SelectStoreResponseTypeDto.toDomain(): User =
  User(
    id = id,
    userName = userName,
    state = state,
    name = name,
    phoneNumber = phoneNumber,
    isDev = isDev,
    roles = roles,
    storeInUseId = storeInUseId,
    storeOwnedId = storeOwnedId,
    lastActive = lastActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun UserDto.toDomain(): User =
  User(
    id = id,
    userName = userName,
    state = state,
    name = name,
    phoneNumber = phoneNumber,
    isDev = isDev,
    roles = roles,
    storeInUseId = storeInUseId,
    storeOwnedId = storeOwnedId,
    lastActive = lastActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun LoginResponseDto.toEntity(token: String? = null): UserEntity =
  UserEntity(
    id = id,
    userName = userName,
    state = state,
    name = name,
    phoneNumber = phoneNumber,
    isDev = isDev,
    roles = roles,
    storeInUseId = storeInUseId,
    storeOwnedId = storeOwnedId,
    lastActive = lastActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    token = token ?: this.token.accessToken,
  )

fun UserEntity.toDomain(): User =
  User(
    id = id,
    userName = userName,
    state = state,
    name = name,
    phoneNumber = phoneNumber,
    isDev = isDev,
    roles = roles,
    storeInUseId = storeInUseId,
    storeOwnedId = storeOwnedId,
    lastActive = lastActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
