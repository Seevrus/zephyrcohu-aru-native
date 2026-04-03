package com.zephyr.boreal.api.dto.request

import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import kotlinx.serialization.Serializable

@Serializable
data class FinishRoundRequestDataDto(
  val roundId: Int,
  val lastSerialNumber: Int? = null,
  val yearCode: Int? = null,
  val receipts: List<RoundReceiptDto>,
)
