package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.api.dto.response.RoundPartnerListDto
import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import com.zephyr.boreal.api.dto.response.RoundStoreDto
import com.zephyr.boreal.api.dto.response.RoundUserDto

@Entity(tableName = "rounds")
data class RoundEntity(
  @PrimaryKey val id: Int,
  val user: RoundUserDto,
  val store: RoundStoreDto,
  val partnerList: RoundPartnerListDto,
  val yearCode: Int?,
  val roundStarted: String,
  val roundFinished: String?,
  val lastSerialNumber: Int?,
  val receipts: List<RoundReceiptDto>,
)
