package com.zephyr.boreal.store.user

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class StoredTokenTest {
  @Test
  fun isTokenExpired_returnsFalse_whenExpiresAtIsInTheFuture() {
    val futureDate = Instant.now().plus(1, ChronoUnit.HOURS).toString()
    val token = StoredToken("token", false, futureDate)

    assertFalse(token.isTokenExpired)
  }

  @Test
  fun isTokenExpired_returnsTrue_whenExpiresAtIsInThePast() {
    val pastDate = Instant.now().minus(1, ChronoUnit.HOURS).toString()
    val token = StoredToken("token", false, pastDate)

    assertTrue(token.isTokenExpired)
  }

  @Test
  fun isTokenExpired_returnsFalse_whenExpiresAtIsNull() {
    val token = StoredToken("token", false, null)

    assertFalse(token.isTokenExpired)
  }

  @Test
  fun isTokenExpired_returnsTrue_whenExpiresAtIsMalformed() {
    val malformedDate = "not-a-date"
    val token = StoredToken("token", false, malformedDate)

    assertTrue(token.isTokenExpired)
  }
}
