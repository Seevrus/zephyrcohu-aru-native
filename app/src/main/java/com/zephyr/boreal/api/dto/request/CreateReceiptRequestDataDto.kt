package com.zephyr.boreal.api.dto.request

import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.Serializable

@Serializable
data class CreateReceiptRequestDataDto(
  val partnerId: Int,
  val partnerCode: String,
  val partnerSiteCode: String,
  val serialNumber: Int,
  val yearCode: Int,
  val vendor: ReceiptVendorDto,
  val buyer: ReceiptBuyerDto,
  // yyyy-MM-dd
  val invoiceDate: String,
  // yyyy-MM-dd
  val fulfillmentDate: String,
  val invoiceType: InvoiceType,
  val paymentDays: Int,
  // yyyy-MM-dd
  val paidDate: String,
  val items: List<ReceiptItemDto>,
  val otherItems: List<ReceiptOtherItemDto>? = null,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmountDto>,
  val roundAmount: Double,
  val roundedAmount: Double,
)
