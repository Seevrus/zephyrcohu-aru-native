package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheDecodeGuardTest {
  @Test
  fun `consumeDegraded is false for a group that was never marked`() {
    assertFalse(CacheDecodeGuard.consumeDegraded("never_marked_group"))
  }

  @Test
  fun `consumeDegraded returns true once after markDegraded, then false again`() {
    CacheDecodeGuard.markDegraded("test_group")

    assertTrue(CacheDecodeGuard.consumeDegraded("test_group"))
    assertFalse(CacheDecodeGuard.consumeDegraded("test_group"))
  }

  @Test
  fun `marking one group does not affect another`() {
    CacheDecodeGuard.markDegraded("group_a")

    assertFalse(CacheDecodeGuard.consumeDegraded("group_b"))
    assertTrue(CacheDecodeGuard.consumeDegraded("group_a"))
  }
}
