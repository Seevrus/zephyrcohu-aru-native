package com.zephyr.boreal.api.dto.request

import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.SerialName
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
  val items: List<CreateReceiptItemDto>,
  val otherItems: List<ReceiptOtherItemDto>? = null,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmountDto>,
  val roundAmount: Double,
  val roundedAmount: Double,
)

/**
 * A receipt line item as sent to the API. Carries [expirationId] so the app can track which
 * inventory batch the sale came from; the backend's create-receipt request validation doesn't
 * define a rule for it (and the response never returns it — see
 * [com.zephyr.boreal.api.dto.response.ReceiptItemDto]), but sending it is harmless.
 */
@Serializable
data class CreateReceiptItemDto(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val quantity: Double,
  val unitName: String,
  val netPrice: Double,
  val netAmount: Double,
  val vatRate: String,
  val vatAmount: Double,
  val grossAmount: Double,
  val discountName: String? = null,
  val expirationId: Int,
  @SerialName("CNCode")
  val cnCode: String,
  val expiresAt: String,
)
