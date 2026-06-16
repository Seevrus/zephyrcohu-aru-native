package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.domain.model.StoreDetailsExpiration

@Entity(tableName = "store_details")
data class StoreDetailsEntity(
  @PrimaryKey val id: Int,
  val name: String,
  val code: String,
  val type: String,
  val state: String,
  val firstAvailableSerialNumber: Int?,
  val lastAvailableSerialNumber: Int?,
  val yearCode: Int?,
  val owner: com.zephyr.boreal.api.dto.response.StoreUserDto?,
  val user: com.zephyr.boreal.api.dto.response.StoreUserDto?,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpiration>,
)
