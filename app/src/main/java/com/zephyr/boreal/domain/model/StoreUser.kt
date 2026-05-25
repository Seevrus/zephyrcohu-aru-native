package com.zephyr.boreal.domain.model

data class StoreUser(
  val id: Int,
  val userName: String,
  val state: UserState,
  val name: String,
  val phoneNumber: String?,
  val isDev: Boolean,
  val roles: List<UserRole>,
  val storeInUseId: Int?,
  val storeOwnedId: Int?,
  val lastActive: String,
  val createdAt: String,
  val updatedAt: String,
)
