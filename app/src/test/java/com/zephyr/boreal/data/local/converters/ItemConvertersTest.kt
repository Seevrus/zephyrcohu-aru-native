package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemConvertersTest {
  private val converters = ItemConverters()

  @Test
  fun `toExpirationList round-trips valid JSON`() {
    val json = """[{"id":1,"expiresAt":"202607","createdAt":"2026-01-01","updatedAt":"2026-01-01"}]"""

    val result = converters.toExpirationList(json)

    assertEquals(1, result.size)
    assertEquals(1, result[0].id)
  }

  @Test
  fun `toExpirationList degrades to an empty list when a required field is missing, and marks the guard`() {
    // Realistic stored blob missing the required "expiresAt" field (e.g. after a backend rename).
    val malformedJson = """[{"id":1,"createdAt":"2026-01-01","updatedAt":"2026-01-01"}]"""

    val result = converters.toExpirationList(malformedJson)

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ITEMS))
  }

  @Test
  fun `toDiscountList degrades to an empty list when a required field is missing, and marks the guard`() {
    val malformedJson = """[{"id":1,"type":"ABSOLUTE","createdAt":"2026-01-01","updatedAt":"2026-01-01"}]"""

    val result = converters.toDiscountList(malformedJson)

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ITEMS))
  }

  @Test
  fun `toPriceListItemList degrades to an empty list when malformed, and marks the price lists guard`() {
    val malformedJson = """not valid json"""

    val result = converters.toPriceListItemList(malformedJson)

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.PRICE_LISTS))
  }
}
