package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptResponseDataDto
import com.zephyr.boreal.api.dto.response.ReceiptUserDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.ReceiptVendor

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
    originalCopiesPrinted = originalCopiesPrinted,
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
    code = code,
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
    expirationId = expirationId,
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
