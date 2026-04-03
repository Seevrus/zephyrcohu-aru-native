package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "other_items")
data class OtherItemEntity(
  @PrimaryKey val id: Int,
  val articleNumber: String,
  val name: String,
  val shortName: String,
  val unitName: String,
  val vatRate: String,
  val netPrice: Double,
  val createdAt: String,
  val updatedAt: String,
)
