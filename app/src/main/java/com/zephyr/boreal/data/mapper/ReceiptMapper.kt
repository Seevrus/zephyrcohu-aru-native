package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.request.CreateReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptResponseDataDto
import com.zephyr.boreal.api.dto.response.ReceiptUserDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.data.local.ReceiptEntity
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.SelectedDiscount
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.domain.utils.DiscountCalculator

fun ReceiptResponseDataDto.toDomain(): Receipt =
  Receipt(
    id = id,
    companyId = companyId,
    companyCode = companyCode,
    partnerId = partnerId,
    partnerCode = partnerCode,
    partnerSiteCode = partnerSiteCode,
    serialNumber = serialNumber,
    yearCode = yearCode,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
    vendor = vendor.toDomain(),
    buyer = buyer.toDomain(),
    invoiceDate = invoiceDate,
    fulfillmentDate = fulfillmentDate,
    invoiceType = invoiceType,
    paidDate = paidDate,
    user = user?.toDomain(),
    items = items.map { it.toDomain() },
    otherItems = otherItems?.map { it.toDomain() } ?: emptyList(),
    quantity = quantity,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    vatAmounts = vatAmounts.map { it.toDomain() },
    roundAmount = roundAmount,
    roundedAmount = roundedAmount,
    lastDownloadedAt = lastDownloadedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun ReceiptUserDto.toDomain(): ReceiptUser =
  ReceiptUser(
    id = id,
    userName = userName,
    name = name,
    phoneNumber = phoneNumber,
  )

fun ReceiptVendorDto.toDomain(): ReceiptVendor =
  ReceiptVendor(
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

fun ReceiptBuyerDto.toDomain(): ReceiptBuyer =
  ReceiptBuyer(
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

fun ReceiptItemDto.toDomain(): ReceiptItem =
  ReceiptItem(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    discountName = discountName,
    cnCode = cnCode,
    expiresAt = expiresAt,
  )

fun ReceiptOtherItemDto.toDomain(): ReceiptOtherItem =
  ReceiptOtherItem(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    comment = comment,
  )

fun ReceiptVatAmountDto.toDomain(): ReceiptVatAmount =
  ReceiptVatAmount(
    vatRate = vatRate,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
  )

fun DraftReceiptItem.toDto(): CreateReceiptItemDto =
  CreateReceiptItemDto(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    discountName = discountName,
    expirationId = expirationId,
    cnCode = cnCode,
    expiresAt = expiresAt,
  )

/**
 * Splits a line's selected discounts into individual API rows (one per discount, plus one for
 * any undiscounted remainder), matching the RN app's createUniqueDiscountedItems.ts submission
 * behavior. Lines without discounts map to a single row via [toDto].
 */
fun DraftReceiptItem.toDtos(): List<CreateReceiptItemDto> {
  if (selectedDiscounts.isEmpty()) {
    return listOf(toDto())
  }

  var discountedQuantity = 0.0
  val discountedRows =
    selectedDiscounts.map { discount ->
      discountedQuantity += discount.quantity
      toDiscountedDto(discount)
    }

  val remainingQuantity = quantity - discountedQuantity
  return if (remainingQuantity <= 0) discountedRows else discountedRows + toRemainderDto(remainingQuantity)
}

private fun DraftReceiptItem.toDiscountedDto(discount: SelectedDiscount): CreateReceiptItemDto {
  val unitNetPrice = DiscountCalculator.calculateDiscountedUnitNetPrice(netPrice, discount)
  val amounts = AmountCalculator.calculateAmounts(unitNetPrice, discount.quantity, vatRate)
  return CreateReceiptItemDto(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = discount.quantity,
    unitName = unitName,
    netPrice = unitNetPrice,
    netAmount = amounts.netAmount,
    vatRate = vatRate,
    vatAmount = amounts.vatAmount,
    grossAmount = amounts.grossAmount,
    discountName = discount.name,
    expirationId = expirationId,
    cnCode = cnCode,
    expiresAt = expiresAt,
  )
}

private fun DraftReceiptItem.toRemainderDto(remainingQuantity: Double): CreateReceiptItemDto {
  val amounts = AmountCalculator.calculateAmounts(netPrice, remainingQuantity, vatRate)
  return CreateReceiptItemDto(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = remainingQuantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = amounts.netAmount,
    vatRate = vatRate,
    vatAmount = amounts.vatAmount,
    grossAmount = amounts.grossAmount,
    discountName = null,
    expirationId = expirationId,
    cnCode = cnCode,
    expiresAt = expiresAt,
  )
}

fun ReceiptOtherItem.toDto(): ReceiptOtherItemDto =
  ReceiptOtherItemDto(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    comment = comment,
  )

fun ReceiptVatAmount.toDto(): ReceiptVatAmountDto =
  ReceiptVatAmountDto(
    vatRate = vatRate,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
  )

fun ReceiptUser.toDto(): ReceiptUserDto =
  ReceiptUserDto(
    id = id,
    userName = userName,
    name = name,
    phoneNumber = phoneNumber,
  )

fun ReceiptVendor.toDto(): ReceiptVendorDto =
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

fun ReceiptBuyer.toDto(): ReceiptBuyerDto =
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

fun ReceiptItem.toDto(): ReceiptItemDto =
  ReceiptItemDto(
    id = id,
    articleNumber = articleNumber,
    name = name,
    quantity = quantity,
    unitName = unitName,
    netPrice = netPrice,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    discountName = discountName,
    cnCode = cnCode,
    expiresAt = expiresAt,
  )

fun Receipt.toEntity(): ReceiptEntity =
  ReceiptEntity(
    id = id,
    companyId = companyId,
    companyCode = companyCode,
    partnerId = partnerId,
    partnerCode = partnerCode,
    partnerSiteCode = partnerSiteCode,
    serialNumber = serialNumber,
    yearCode = yearCode,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
    vendor = vendor.toDto(),
    buyer = buyer.toDto(),
    invoiceDate = invoiceDate,
    fulfillmentDate = fulfillmentDate,
    invoiceType = invoiceType,
    paidDate = paidDate,
    user = user?.toDto(),
    items = items.map { it.toDto() },
    otherItems = otherItems.map { it.toDto() },
    quantity = quantity,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    vatAmounts = vatAmounts.map { it.toDto() },
    roundAmount = roundAmount,
    roundedAmount = roundedAmount,
    lastDownloadedAt = lastDownloadedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun ReceiptEntity.toDomain(): Receipt =
  Receipt(
    id = id,
    companyId = companyId,
    companyCode = companyCode,
    partnerId = partnerId,
    partnerCode = partnerCode,
    partnerSiteCode = partnerSiteCode,
    serialNumber = serialNumber,
    yearCode = yearCode,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
    vendor = vendor.toDomain(),
    buyer = buyer.toDomain(),
    invoiceDate = invoiceDate,
    fulfillmentDate = fulfillmentDate,
    invoiceType = invoiceType,
    paidDate = paidDate,
    user = user?.toDomain(),
    items = items.map { it.toDomain() },
    otherItems = otherItems.map { it.toDomain() },
    quantity = quantity,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    vatAmounts = vatAmounts.map { it.toDomain() },
    roundAmount = roundAmount,
    roundedAmount = roundedAmount,
    lastDownloadedAt = lastDownloadedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
