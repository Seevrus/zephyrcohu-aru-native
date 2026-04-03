package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.Serializable

@Serializable
data class RoundReceiptDto(
  val id: Int? = null,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int? = null,
  val cancelYearCode: Int? = null,
  val invoiceType: InvoiceType,
  val paymentDays: Int,
  val buyer: RoundReceiptBuyerDto,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val roundAmount: Double,
  val roundedAmount: Double,
)

@Serializable
data class RoundReceiptBuyerDto(
  val name: String,
  val vatNumber: String? = null,
)

@Serializable
data class RoundUserDto(
  val id: Int? = null,
  val userName: String,
  val name: String,
)

@Serializable
data class RoundStoreDto(
  val id: Int? = null,
  val code: String,
  val name: String,
)

@Serializable
data class RoundPartnerListDto(
  val id: Int? = null,
  val name: String,
)

@Serializable
data class RoundResponseDataDto(
  val id: Int,
  val user: RoundUserDto,
  val store: RoundStoreDto,
  val partnerList: RoundPartnerListDto,
  val yearCode: Int? = null,
  val roundStarted: String,
  val roundFinished: String? = null,
  val lastSerialNumber: Int? = null,
  val receipts: List<RoundReceiptDto>? = null,
)

@Serializable
data class RoundResponseDto(
  val data: RoundResponseDataDto,
)

@Serializable
data class RoundsResponseDto(
  val data: List<RoundResponseDataDto>,
)
