package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.api.dto.response.StoreUserDto
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.UserState

@Entity(tableName = "stores")
data class StoreEntity(
  @PrimaryKey val id: Int,
  val code: String,
  val name: String,
  val type: StoreType,
  val state: UserState,
  val firstAvailableSerialNumber: Int,
  val lastAvailableSerialNumber: Int,
  val yearCode: Int,
  val owner: StoreUserDto? = null,
  val user: StoreUserDto? = null,
  val createdAt: String,
  val updatedAt: String,
)
