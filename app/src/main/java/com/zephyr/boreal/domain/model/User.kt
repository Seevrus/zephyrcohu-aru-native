package com.zephyr.boreal.domain.model

data class User(
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
  val company: Company,
)

val User.canAddPartner: Boolean
  get() = roles.contains(UserRole.NEW_PARTNER)

val User.canLoadAnyStore: Boolean
  get() = roles.contains(UserRole.LOAD_ANY)

val User.canLoadStore: Boolean
  get() = roles.contains(UserRole.LOAD_OWNED)

val User.canUseApp: Boolean
  get() = roles.contains(UserRole.APP)
