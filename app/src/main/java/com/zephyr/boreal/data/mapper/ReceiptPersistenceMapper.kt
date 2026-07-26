package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.data.local.record.PersistedReceiptBuyer
import com.zephyr.boreal.data.local.record.PersistedReceiptItem
import com.zephyr.boreal.data.local.record.PersistedReceiptOtherItem
import com.zephyr.boreal.data.local.record.PersistedReceiptUser
import com.zephyr.boreal.data.local.record.PersistedReceiptVatAmount
import com.zephyr.boreal.data.local.record.PersistedReceiptVendor
import com.zephyr.boreal.data.local.record.ReceiptEntity
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.ReceiptVendor

/**
 * Domain <-> [ReceiptEntity] mapping, kept separate from [ReceiptMapper.kt]'s domain <-> `api/dto`
 * mapping (still needed for the create-receipt API response). `receipts` is a durable record, so
 * these map directly against dedicated persisted types instead of round-tripping through a DTO —
 * see `rn-migration-tasks/03f-persistence-durability-classes.md`.
 */
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
    vendor = vendor.toPersisted(),
    buyer = buyer.toPersisted(),
    invoiceDate = invoiceDate,
    fulfillmentDate = fulfillmentDate,
    invoiceType = invoiceType,
    paidDate = paidDate,
    user = user?.toPersisted(),
    items = items.map { it.toPersisted() },
    otherItems = otherItems.map { it.toPersisted() },
    quantity = quantity,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    vatAmounts = vatAmounts.map { it.toPersisted() },
    roundAmount = roundAmount,
    roundedAmount = roundedAmount,
    lastDownloadedAt = lastDownloadedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPrinted = isPrinted,
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
    isPrinted = isPrinted,
  )

fun ReceiptVendor.toPersisted(): PersistedReceiptVendor =
  PersistedReceiptVendor(
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

fun PersistedReceiptVendor.toDomain(): ReceiptVendor =
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

fun ReceiptBuyer.toPersisted(): PersistedReceiptBuyer =
  PersistedReceiptBuyer(
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

fun PersistedReceiptBuyer.toDomain(): ReceiptBuyer =
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

fun ReceiptUser.toPersisted(): PersistedReceiptUser =
  PersistedReceiptUser(
    id = id,
    userName = userName,
    name = name,
    phoneNumber = phoneNumber,
  )

fun PersistedReceiptUser.toDomain(): ReceiptUser =
  ReceiptUser(
    id = id,
    userName = userName,
    name = name,
    phoneNumber = phoneNumber,
  )

fun ReceiptItem.toPersisted(): PersistedReceiptItem =
  PersistedReceiptItem(
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

fun PersistedReceiptItem.toDomain(): ReceiptItem =
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

fun ReceiptOtherItem.toPersisted(): PersistedReceiptOtherItem =
  PersistedReceiptOtherItem(
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

fun PersistedReceiptOtherItem.toDomain(): ReceiptOtherItem =
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

fun ReceiptVatAmount.toPersisted(): PersistedReceiptVatAmount =
  PersistedReceiptVatAmount(
    vatRate = vatRate,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
  )

fun PersistedReceiptVatAmount.toDomain(): ReceiptVatAmount =
  ReceiptVatAmount(
    vatRate = vatRate,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
  )
