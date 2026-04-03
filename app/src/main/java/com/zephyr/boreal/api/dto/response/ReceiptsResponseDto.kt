package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.api.dto.request.OrderItemDto
import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptVendorDto(
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val felir: String,
  val iban: String,
  val bankAccount: String,
  val vatNumber: String,
)

@Serializable
data class ReceiptBuyerDto(
  val id: Int,
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val deliveryName: String,
  val deliveryCountry: String,
  val deliveryPostalCode: String,
  val deliveryCity: String,
  val deliveryAddress: String,
  val iban: String? = null,
  val bankAccount: String? = null,
  val vatNumber: String? = null,
)

@Serializable
data class ReceiptUserDto(
  val id: Int,
  val code: String,
  val name: String,
  val phoneNumber: String,
)

@Serializable
data class ReceiptItemDto(
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

@Serializable
data class ReceiptOtherItemDto(
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
  val comment: String? = null,
)

@Serializable
data class ReceiptVatAmountDto(
  val vatRate: String,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
)

@Serializable
data class ReceiptResponseDataDto(
  val id: Int,
  val companyId: Int,
  val companyCode: String,
  val partnerId: Int,
  val partnerCode: String,
  val partnerSiteCode: String,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int? = null,
  val cancelYearCode: Int? = null,
  val originalCopiesPrinted: Int,
  val vendor: ReceiptVendorDto,
  val buyer: ReceiptBuyerDto,
  val invoiceDate: String,
  val fulfillmentDate: String,
  val invoiceType: InvoiceType,
  val paidDate: String,
  val user: ReceiptUserDto? = null,
  val items: List<ReceiptItemDto>,
  val otherItems: List<ReceiptOtherItemDto>? = null,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmountDto>,
  val roundAmount: Double,
  val roundedAmount: Double,
  val lastDownloadedAt: String? = null,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class CreatedOrderResponseDataDto(
  val id: Int,
  val partnerId: Int,
  // UTC
  val orderedAt: String,
  val items: List<OrderItemDto>,
)

@Serializable
data class CreateOrdersResponseDto(
  val data: List<CreatedOrderResponseDataDto>,
)

@Serializable
data class ReceiptsResponseDto(
  val data: List<ReceiptResponseDataDto>,
)
