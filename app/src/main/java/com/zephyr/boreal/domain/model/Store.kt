package com.zephyr.boreal.domain.model

data class Store(
  val id: Int,
  val code: String,
  val name: String,
  val type: StoreType,
  val state: UserState,
  val firstAvailableSerialNumber: Int,
  val lastAvailableSerialNumber: Int,
  val yearCode: Int,
  val owner: User? = null,
  val user: User? = null,
  val createdAt: String,
  val updatedAt: String,
)
