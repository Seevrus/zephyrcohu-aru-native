package com.zephyr.boreal.store.user

import java.time.Instant

data class StoredToken(
  val token: String,
  val isPasswordExpired: Boolean,
  val expiresAt: String,
) {
  val isTokenExpired: Boolean
    get() =
      runCatching {
        Instant.parse(expiresAt).isBefore(Instant.now())
      }.getOrDefault(true)
}
