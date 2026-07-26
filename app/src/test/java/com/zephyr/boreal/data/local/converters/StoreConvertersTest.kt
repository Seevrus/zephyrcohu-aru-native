package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StoreConvertersTest {
  private val converters = StoreConverters()

  @Test
  fun `toUser degrades to null when malformed and marks both stores and store details guards`() {
    val result = converters.toUser("not valid json")

    assertNull(result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.STORES))
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.STORE_DETAILS))
  }

  @Test
  fun `toUser handles a literal null string without decoding`() {
    val result = converters.toUser(null)

    assertNull(result)
  }

  @Test
  fun `toStoreDetailsExpirationList degrades to an empty list when malformed`() {
    val result = converters.toStoreDetailsExpirationList("not valid json")

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.STORE_DETAILS))
  }
}
