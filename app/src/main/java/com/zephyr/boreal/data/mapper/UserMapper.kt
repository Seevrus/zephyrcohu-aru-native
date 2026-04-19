package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.CheckTokenResponseDto
import com.zephyr.boreal.api.dto.response.CompanyDto
import com.zephyr.boreal.api.dto.response.LoginResponseDto
import com.zephyr.boreal.api.dto.response.PartialCompanyDto
import com.zephyr.boreal.api.dto.response.SelectStoreResponseTypeDto
import com.zephyr.boreal.api.dto.response.UserDto
import com.zephyr.boreal.data.local.UserEntity
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.User

fun CompanyDto.toDomain(): Company =
  Company(
    id = id,
    code = code,
    name = name,
    country = country,
    postalCode = postalCode,
    city = city,
    address = address,
    felir = felir,
    vatNumber = vatNumber,
    iban = iban,
    bankAccount = bankAccount,
    phoneNumber = phoneNumber,
    email = email,
  )

fun PartialCompanyDto.toDomain(): Company =
  Company(
    id = id,
    name = name,
    code = "",
    country = "",
    postalCode = "",
    city = "",
    address = "",
    felir = "",
    vatNumber = "",
    iban = "",
    bankAccount = "",
  )

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
    company = company.toDomain(),
  )

fun CheckTokenResponseDto.toDomain(): User =
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
    company = company.toDomain(),
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
    company = company.toDomain(),
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
    company = company.toDomain(),
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
    // We convert PartialCompanyDto to CompanyDto for persistence.
    // Fields will be empty strings initially, but will be filled during checkToken.
    company =
      CompanyDto(
        id = company.id,
        name = company.name,
        code = "",
        country = "",
        postalCode = "",
        city = "",
        address = "",
        felir = "",
        vatNumber = "",
        iban = "",
        bankAccount = "",
      ),
    token = token ?: this.token.accessToken,
  )

fun CheckTokenResponseDto.toEntity(token: String? = null): UserEntity =
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
    company = company,
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
    company = company.toDomain(),
  )
