package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
  val id: Int,
  val userName: String,
  val state: UserState,
  val name: String,
  val phoneNumber: String? = null,
  val isDev: Boolean,
  val roles: List<UserRole>,
  val storeInUseId: Int? = null,
  val storeOwnedId: Int? = null,
  val lastActive: String,
  val createdAt: String,
  val updatedAt: String,
  val company: PartialCompanyDto,
)

@Serializable
data class TokenDto(
  val tokenType: String,
  val accessToken: String,
  val abilities: List<UserRole>,
  val expiresAt: String? = null,
)

@Serializable
data class LoginResponseDto(
  val id: Int,
  val userName: String,
  val state: UserState,
  val name: String,
  val phoneNumber: String? = null,
  val isDev: Boolean,
  val roles: List<UserRole>,
  val storeInUseId: Int? = null,
  val storeOwnedId: Int? = null,
  val lastActive: String,
  val createdAt: String,
  val updatedAt: String,
  val company: PartialCompanyDto,
  val lastRound: RoundResponseDto? = null,
  val token: TokenDto,
)

@Serializable
data class CheckTokenResponseDto(
  val id: Int,
  val userName: String,
  val state: UserState,
  val name: String,
  val phoneNumber: String? = null,
  val isDev: Boolean,
  val roles: List<UserRole>,
  val storeInUseId: Int? = null,
  val storeOwnedId: Int? = null,
  val lastActive: String,
  val createdAt: String,
  val updatedAt: String,
  val company: CompanyDto,
  val lastRound: RoundResponseDto? = null,
  val token: TokenDto,
)
