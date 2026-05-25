package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.domain.model.StoreDetailsExpiration

@Entity(tableName = "store_details")
data class StoreDetailsEntity(
  @PrimaryKey val id: Int,
  val name: String,
  val code: String,
  val address: String,
  val city: String,
  val postalCode: String,
  val country: String,
  val createdAt: String,
  val updatedAt: String,
  val expirations: List<StoreDetailsExpiration>,
)
