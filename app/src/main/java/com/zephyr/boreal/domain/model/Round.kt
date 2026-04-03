package com.zephyr.boreal.domain.model

data class Round(
  val id: Int,
  val user: RoundUser,
  val store: RoundStore,
  val partnerList: RoundPartnerList,
  val yearCode: Int?,
  val roundStarted: String, // UTC
  val roundFinished: String?,
  val lastSerialNumber: Int?,
  val receipts: List<RoundReceipt> = emptyList(),
)

data class RoundUser(
  val id: Int?,
  val userName: String,
  val name: String,
)

data class RoundStore(
  val id: Int?,
  val code: String,
  val name: String,
)

data class RoundPartnerList(
  val id: Int?,
  val name: String,
)

data class RoundReceipt(
  val id: Int?,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int?,
  val cancelYearCode: Int?,
  val invoiceType: InvoiceType,
  val paymentDays: Int,
  val buyer: RoundReceiptBuyer,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val roundAmount: Double,
  val roundedAmount: Double,
)

data class RoundReceiptBuyer(
  val name: String,
  val vatNumber: String?,
)
