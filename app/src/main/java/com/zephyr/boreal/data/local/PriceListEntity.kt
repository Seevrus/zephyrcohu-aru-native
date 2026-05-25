package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.domain.model.PriceListItem

@Entity(tableName = "price_lists")
data class PriceListEntity(
  @PrimaryKey val id: Int,
  val code: String?,
  val name: String,
  val items: List<PriceListItem>,
)
