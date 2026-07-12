package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.data.mapper.toDto
import com.zephyr.boreal.data.mapper.toDtos
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.StoreDetails

data class ReceiptDates(
  val invoiceDate: String,
  val fulfillmentDate: String,
  val paidDate: String,
)

data class AmountRounding(
  val roundAmount: Double,
  val roundedAmount: Double,
)

/**
 * Builds the create-receipt request from a finalized draft. Callers are responsible for
 * validating that [draft] has all required fields (partnerId, partnerCode, partnerSiteCode,
 * buyer, invoiceType, paymentDays) before calling this.
 */
fun buildCreateReceiptRequest(
  draft: DraftReceipt,
  company: Company,
  store: StoreDetails,
  serialNumber: Int,
  totals: ReceiptTotals,
  dates: ReceiptDates,
  rounding: AmountRounding,
): CreateReceiptRequestDataDto =
  CreateReceiptRequestDataDto(
    partnerId = requireNotNull(draft.partnerId),
    partnerCode = requireNotNull(draft.partnerCode),
    partnerSiteCode = requireNotNull(draft.partnerSiteCode),
    serialNumber = serialNumber,
    yearCode = requireNotNull(store.yearCode),
    vendor = company.toVendorDto(),
    buyer = requireNotNull(draft.buyer).toDto(),
    invoiceDate = dates.invoiceDate,
    fulfillmentDate = dates.fulfillmentDate,
    invoiceType = requireNotNull(draft.invoiceType),
    paymentDays = requireNotNull(draft.paymentDays),
    paidDate = dates.paidDate,
    items = draft.items.flatMap { it.toDtos() },
    otherItems = draft.otherItems.map { it.toDto() },
    quantity = totals.quantity,
    netAmount = totals.netAmount,
    vatAmount = totals.vatAmount,
    grossAmount = totals.grossAmount,
    vatAmounts = totals.vatAmounts.map { it.toDto() },
    roundAmount = rounding.roundAmount,
    roundedAmount = rounding.roundedAmount,
  )

private fun Company.toVendorDto(): ReceiptVendorDto =
  ReceiptVendorDto(
    name = name,
    country = country,
    postalCode = postalCode,
    city = city,
    address = address,
    felir = felir,
    iban = iban,
    bankAccount = bankAccount,
    vatNumber = vatNumber,
  )

private fun ReceiptBuyer.toDto(): ReceiptBuyerDto =
  ReceiptBuyerDto(
    id = id,
    name = name,
    country = country,
    postalCode = postalCode,
    city = city,
    address = address,
    deliveryName = deliveryName,
    deliveryCountry = deliveryCountry,
    deliveryPostalCode = deliveryPostalCode,
    deliveryCity = deliveryCity,
    deliveryAddress = deliveryAddress,
    iban = iban,
    bankAccount = bankAccount,
    vatNumber = vatNumber,
  )
