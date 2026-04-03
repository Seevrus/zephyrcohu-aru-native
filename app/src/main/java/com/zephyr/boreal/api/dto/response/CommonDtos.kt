package com.zephyr.boreal.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserRoleDto(
  val role: String,
) // Using string for enum-like values from TS

@Serializable
data class CompanyDto(
  val id: Int,
  val code: String,
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val felir: String,
  val vatNumber: String,
  val iban: String,
  val bankAccount: String,
  val phoneNumber: String? = null,
  val email: String? = null,
)

@Serializable
data class TimeStampsDto(
  val createdAt: String,
  val updatedAt: String,
)
