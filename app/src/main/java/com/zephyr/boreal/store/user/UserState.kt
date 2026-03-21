package com.zephyr.boreal.store.user

data class UserState(
  val deviceId: String? = null,
  val storedToken: StoredToken? = null,
)
