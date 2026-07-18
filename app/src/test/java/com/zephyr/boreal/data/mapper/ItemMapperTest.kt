package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.DiscountDto
import com.zephyr.boreal.domain.model.DiscountType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ItemMapperTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun `DiscountDto decodes a freeForm discount whose amount is null`() {
    val body =
      """{"id":72,"name":"Egyedi kedvezmény","type":"freeForm","amount":null,
        |"createdAt":"2026-07-12T15:21:23.000000Z","updatedAt":"2026-07-12T15:21:23.000000Z"}
      """.trimMargin()

    val dto = json.decodeFromString<DiscountDto>(body)

    assertEquals(DiscountType.FREE_FORM, dto.type)
    assertNull(dto.amount)
  }

  @Test
  fun `DiscountDto toDomain preserves a null amount for freeForm discounts`() {
    val dto =
      DiscountDto(
        id = 72,
        name = "Egyedi kedvezmény",
        type = DiscountType.FREE_FORM,
        amount = null,
        createdAt = "2026-07-12T15:21:23.000000Z",
        updatedAt = "2026-07-12T15:21:23.000000Z",
      )

    val discount = dto.toDomain()

    assertNull(discount.amount)
  }

  @Test
  fun `DiscountDto toDomain preserves a non-null amount for absolute discounts`() {
    val dto =
      DiscountDto(
        id = 71,
        name = "Fix kedvezmény",
        type = DiscountType.ABSOLUTE,
        amount = 821.0,
        createdAt = "2026-07-12T15:21:23.000000Z",
        updatedAt = "2026-07-12T15:21:23.000000Z",
      )

    val discount = dto.toDomain()

    assertEquals(821.0, discount.amount)
  }
}
