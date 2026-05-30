package com.zephyr.boreal.data.local.converters

import com.zephyr.boreal.api.dto.response.RoundResponseDataDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserConvertersTest {
  private val converters = UserConverters()

  @Test
  fun `fromRoundResponseDataDto serializes to JSON correctly`() {
    val dto =
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

    val jsonString = converters.fromRoundResponseDataDto(dto)

    // Test that the serialized string contains the id correctly (basic verification)
    assertEquals(true, jsonString?.contains("\"id\":1"))
    assertEquals(true, jsonString?.contains("\"store\":{\"id\":1"))
  }

  @Test
  fun `fromRoundResponseDataDto handles null`() {
    val jsonString = converters.fromRoundResponseDataDto(null)
    assertNull(jsonString)
  }

  @Test
  fun `toRoundResponseDataDto deserializes JSON correctly`() {
    val dto =
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
        roundFinished = "2026-05-30T10:00:00Z",
        lastSerialNumber = null,
        receipts = emptyList(),
      )

    val jsonString = converters.fromRoundResponseDataDto(dto)
    val deserializedDto = converters.toRoundResponseDataDto(jsonString)

    assertEquals(dto.id, deserializedDto?.id)
    assertEquals(dto.yearCode, deserializedDto?.yearCode)
    assertEquals(dto.roundFinished, deserializedDto?.roundFinished)
  }

  @Test
  fun `toRoundResponseDataDto handles null`() {
    val dto = converters.toRoundResponseDataDto(null)
    assertNull(dto)
  }
}
