package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.RoundPartnerListDto
import com.zephyr.boreal.api.dto.response.RoundReceiptDto
import com.zephyr.boreal.api.dto.response.RoundResponseDataDto
import com.zephyr.boreal.api.dto.response.RoundStoreDto
import com.zephyr.boreal.api.dto.response.RoundUserDto
import com.zephyr.boreal.data.local.RoundEntity
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundReceipt
import com.zephyr.boreal.domain.model.RoundReceiptBuyer
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser

fun RoundResponseDataDto.toEntity(): RoundEntity =
  RoundEntity(
    id = id,
    user = user,
    store = store,
    partnerList = partnerList,
    yearCode = yearCode,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
    lastSerialNumber = lastSerialNumber,
    receipts = receipts ?: emptyList(),
  )

fun RoundEntity.toDomain(): Round =
  Round(
    id = id,
    user = user.toDomain(),
    store = store.toDomain(),
    partnerList = partnerList.toDomain(),
    yearCode = yearCode,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
    lastSerialNumber = lastSerialNumber,
    receipts = receipts.map { it.toDomain() },
  )

fun RoundResponseDataDto.toDomain(): Round =
  Round(
    id = id,
    user = user.toDomain(),
    store = store.toDomain(),
    partnerList = partnerList.toDomain(),
    yearCode = yearCode,
    roundStarted = roundStarted,
    roundFinished = roundFinished,
    lastSerialNumber = lastSerialNumber,
    receipts = receipts?.map { it.toDomain() } ?: emptyList(),
  )

fun RoundUserDto.toDomain(): RoundUser =
  RoundUser(
    id = id,
    userName = userName,
    name = name,
  )

fun RoundStoreDto.toDomain(): RoundStore =
  RoundStore(
    id = id,
    code = code,
    name = name,
  )

fun RoundPartnerListDto.toDomain(): RoundPartnerList =
  RoundPartnerList(
    id = id,
    name = name,
  )

fun RoundReceiptDto.toDomain(): RoundReceipt =
  RoundReceipt(
    id = id,
    serialNumber = serialNumber,
    yearCode = yearCode,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
    invoiceType = invoiceType,
    paymentDays = paymentDays,
    buyer = RoundReceiptBuyer(buyer.name, buyer.vatNumber),
    quantity = quantity,
    netAmount = netAmount,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    roundAmount = roundAmount,
    roundedAmount = roundedAmount,
  )
