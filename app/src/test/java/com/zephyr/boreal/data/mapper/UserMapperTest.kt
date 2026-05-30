package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.CheckTokenResponseDto
import com.zephyr.boreal.api.dto.response.CompanyDto
import com.zephyr.boreal.api.dto.response.LoginResponseDto
import com.zephyr.boreal.api.dto.response.PartialCompanyDto
import com.zephyr.boreal.api.dto.response.RoundResponseDataDto
import com.zephyr.boreal.api.dto.response.TokenDto
import com.zephyr.boreal.data.local.UserEntity
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserMapperTest {
  private val roundResponseDto =
    RoundResponseDataDto(
      id = 1,
      user =
        com.zephyr.boreal.api.dto.response
          .RoundUserDto(id = 1, userName = "test", name = "Test User"),
      store =
        com.zephyr.boreal.api.dto.response
          .RoundStoreDto(id = 1, code = "C", name = "Store"),
      partnerList =
        com.zephyr.boreal.api.dto.response
          .RoundPartnerListDto(id = 1, name = "Partner List"),
      yearCode = 2026,
      roundStarted = "2026-05-30T10:00:00Z",
      roundFinished = null,
      lastSerialNumber = null,
      receipts = emptyList(),
    )

  @Test
  fun `LoginResponseDto toDomain maps lastRound correctly`() {
    val dto =
      LoginResponseDto(
        id = 1,
        userName = "test@user",
        state = UserState.IDLE,
        name = "Test User",
        phoneNumber = null,
        isDev = false,
        roles = listOf(UserRole.ADMIN),
        storeInUseId = null,
        storeOwnedId = null,
        lastActive = "2026-04-03T10:00:00Z",
        createdAt = "2026-04-03T09:00:00Z",
        updatedAt = "2026-04-03T09:00:00Z",
        company = PartialCompanyDto(3, "Company 3"),
        lastRound = roundResponseDto,
        token = TokenDto("Bearer", "token", listOf(UserRole.ADMIN)),
      )

    val domain = dto.toDomain()

    assertNotNull(domain.lastRound)
    assertEquals(1, domain.lastRound?.id)
  }

  @Test
  fun `CheckTokenResponseDto toDomain maps lastRound correctly`() {
    val dto =
      CheckTokenResponseDto(
        id = 1,
        userName = "test@user",
        state = UserState.IDLE,
        name = "Test User",
        phoneNumber = null,
        isDev = false,
        roles = listOf(UserRole.ADMIN),
        storeInUseId = null,
        storeOwnedId = null,
        lastActive = "2026-04-03T10:00:00Z",
        createdAt = "2026-04-03T09:00:00Z",
        updatedAt = "2026-04-03T09:00:00Z",
        company =
          com.zephyr.boreal.api.dto.response.CompanyDto(
            id = 3,
            code = "CODE",
            name = "Company 3",
            country = "HU",
            postalCode = "1234",
            city = "City",
            address = "Address",
            felir = "FELIR",
            vatNumber = "12345678-1-11",
            iban = "IBAN",
            bankAccount = "BANK",
          ),
        lastRound = roundResponseDto,
        token = TokenDto("Bearer", "token", listOf(UserRole.ADMIN)),
      )

    val domain = dto.toDomain()

    assertNotNull(domain.lastRound)
    assertEquals(1, domain.lastRound?.id)
  }

  @Test
  fun `LoginResponseDto toEntity maps lastRound correctly`() {
    val dto =
      LoginResponseDto(
        id = 1,
        userName = "test@user",
        state = UserState.IDLE,
        name = "Test User",
        phoneNumber = null,
        isDev = false,
        roles = listOf(UserRole.ADMIN),
        storeInUseId = null,
        storeOwnedId = null,
        lastActive = "2026-04-03T10:00:00Z",
        createdAt = "2026-04-03T09:00:00Z",
        updatedAt = "2026-04-03T09:00:00Z",
        company = PartialCompanyDto(3, "Company 3"),
        lastRound = roundResponseDto,
        token = TokenDto("Bearer", "token", listOf(UserRole.ADMIN)),
      )

    val entity = dto.toEntity()

    assertNotNull(entity.lastRound)
    assertEquals(1, entity.lastRound?.id)
  }

  @Test
  fun `UserEntity toDomain maps lastRound correctly`() {
    val entity =
      UserEntity(
        id = 1,
        userName = "test@user",
        state = UserState.IDLE,
        name = "Test User",
        phoneNumber = null,
        isDev = false,
        roles = listOf(UserRole.ADMIN),
        storeInUseId = null,
        storeOwnedId = null,
        lastActive = "2026-04-03T10:00:00Z",
        createdAt = "2026-04-03T09:00:00Z",
        updatedAt = "2026-04-03T09:00:00Z",
        company =
          com.zephyr.boreal.api.dto.response.CompanyDto(
            id = 3,
            code = "CODE",
            name = "Company 3",
            country = "HU",
            postalCode = "1234",
            city = "City",
            address = "Address",
            felir = "FELIR",
            vatNumber = "12345678-1-11",
            iban = "IBAN",
            bankAccount = "BANK",
          ),
        lastRound = roundResponseDto,
        token = "token",
      )

    val domain = entity.toDomain()

    assertNotNull(domain.lastRound)
    assertEquals(1, domain.lastRound?.id)
  }

  @Test
  fun `Null lastRound maps to null in domain and entity`() {
    val entity =
      UserEntity(
        id = 1,
        userName = "test@user",
        state = UserState.IDLE,
        name = "Test User",
        phoneNumber = null,
        isDev = false,
        roles = listOf(UserRole.ADMIN),
        storeInUseId = null,
        storeOwnedId = null,
        lastActive = "2026-04-03T10:00:00Z",
        createdAt = "2026-04-03T09:00:00Z",
        updatedAt = "2026-04-03T09:00:00Z",
        company =
          com.zephyr.boreal.api.dto.response.CompanyDto(
            id = 3,
            code = "CODE",
            name = "Company 3",
            country = "HU",
            postalCode = "1234",
            city = "City",
            address = "Address",
            felir = "FELIR",
            vatNumber = "12345678-1-11",
            iban = "IBAN",
            bankAccount = "BANK",
          ),
        lastRound = null,
        token = "token",
      )

    val domain = entity.toDomain()

    assertNull(domain.lastRound)
  }
}
