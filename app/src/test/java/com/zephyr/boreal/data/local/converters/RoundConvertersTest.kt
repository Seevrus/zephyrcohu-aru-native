package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoundConvertersTest {
  private val converters = RoundConverters()

  @Test
  fun `toRoundReceiptList degrades to an empty list when malformed, and marks the rounds guard`() {
    val result = converters.toRoundReceiptList("not valid json")

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ROUNDS))
  }

  @Test
  fun `toRoundUser degrades to a placeholder when a required field is missing`() {
    // Missing the required "userName" field.
    val malformedJson = """{"id":1,"name":"Test User"}"""

    val result = converters.toRoundUser(malformedJson)

    assertEquals("", result.userName)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ROUNDS))
  }

  @Test
  fun `toRoundStore degrades to a placeholder when malformed`() {
    val result = converters.toRoundStore("not valid json")

    assertEquals("", result.code)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ROUNDS))
  }

  @Test
  fun `toRoundPartnerList degrades to a placeholder when malformed`() {
    val result = converters.toRoundPartnerList("not valid json")

    assertEquals("", result.name)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.ROUNDS))
  }
}
