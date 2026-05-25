package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_lists")
data class PartnerListEntity(
  @PrimaryKey val id: Int,
  val name: String,
  val partners: List<Int>,
  val createdAt: String,
  val updatedAt: String,
)
