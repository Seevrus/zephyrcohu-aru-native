package com.zephyr.boreal.api.dto.request

import com.zephyr.boreal.domain.model.InvoiceType
import kotlinx.serialization.Serializable

@Serializable
data class FinishRoundRequestDataDto(
  val roundId: Int,
  val lastSerialNumber: Int? = null,
  val yearCode: Int? = null,
  val receipts: List<FinishRoundReceiptDto>,
)

/**
 * A receipt as sent to `/rounds/finish`. Duplicates
 * [com.zephyr.boreal.api.dto.response.RoundReceiptDto]'s shape rather than reusing it directly:
 * that response DTO's nullable fields declare `= null` defaults, and kotlinx.serialization omits
 * a field from encoded JSON entirely when its value matches the declared default (regardless of
 * `explicitNulls`) — so an uncancelled receipt's `cancelSerialNumber`/`cancelYearCode` silently
 * vanished from the request body, and the backend's `present` validation rule rejected it.
 * Declaring these fields without a default here forces kotlinx.serialization to always encode
 * them, even as `null`.
 */
@Serializable
data class FinishRoundReceiptDto(
  val id: Int?,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int?,
  val cancelYearCode: Int?,
  val invoiceType: InvoiceType,
  val paymentDays: Int,
  val buyer: FinishRoundReceiptBuyerDto,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val roundAmount: Double,
  val roundedAmount: Double,
)

@Serializable
data class FinishRoundReceiptBuyerDto(
  val name: String,
  val vatNumber: String?,
)
