package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a User.
 */
@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val name: String,
  val email: String,
  val token: String? = null,
)
