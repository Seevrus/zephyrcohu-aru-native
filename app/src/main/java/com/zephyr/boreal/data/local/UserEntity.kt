package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: Int,
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
  val token: String? = null,
)
