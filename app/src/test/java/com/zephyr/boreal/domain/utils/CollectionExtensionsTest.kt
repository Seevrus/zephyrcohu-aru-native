package com.zephyr.boreal.domain.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CollectionExtensionsTest {
  @Test
  fun `toggle adds an absent element`() {
    val result = setOf(1, 2).toggle(3)

    assertEquals(setOf(1, 2, 3), result)
  }

  @Test
  fun `toggle removes a present element`() {
    val result = setOf(1, 2, 3).toggle(2)

    assertEquals(setOf(1, 3), result)
  }
}
