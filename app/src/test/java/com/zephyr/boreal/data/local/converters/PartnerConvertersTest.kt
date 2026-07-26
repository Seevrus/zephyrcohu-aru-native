package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PartnerConvertersTest {
  private val converters = PartnerConverters()

  @Test
  fun `toPartnerLocationList degrades to an empty list when a required field is missing`() {
    // Missing the required "locationType" field.
    val malformedJson =
      """[{"name":"Site","country":"HU","postalCode":"1011","city":"Budapest",""" +
        """"address":"Fo","createdAt":"a","updatedAt":"a"}]"""

    val result = converters.toPartnerLocationList(malformedJson)

    assertEquals(emptyList<Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.PARTNERS))
  }

  @Test
  fun `toIntList degrades to an empty list when malformed, and marks the partner lists guard`() {
    val result = converters.toIntList("not valid json")

    assertEquals(emptyList<Int>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.PARTNER_LISTS))
  }

  @Test
  fun `toPriceList degrades to null when a required field is missing`() {
    val malformedJson = """{"code":"C"}""" // missing required "id"/"name"

    val result = converters.toPriceList(malformedJson)

    assertNull(result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.PARTNERS))
  }

  @Test
  fun `toLocationsMap degrades to an empty map when malformed, and marks the tax payer guard`() {
    val result = converters.toLocationsMap("not valid json")

    assertEquals(emptyMap<String, Any>(), result)
    assertTrue(CacheDecodeGuard.consumeDegraded(CacheGroups.TAX_NUMBER_SEARCH))
  }
}
