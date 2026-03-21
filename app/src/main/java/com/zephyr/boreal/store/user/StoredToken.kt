package com.zephyr.boreal.store.user

data class StoredToken(
  val token: String,
  val isPasswordExpired: Boolean,
  val expiresAt: String,
)
