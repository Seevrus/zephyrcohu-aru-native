package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.api.dto.response.ReceiptBuyerDto
import com.zephyr.boreal.api.dto.response.ReceiptItemDto
import com.zephyr.boreal.api.dto.response.ReceiptOtherItemDto
import com.zephyr.boreal.api.dto.response.ReceiptUserDto
import com.zephyr.boreal.api.dto.response.ReceiptVatAmountDto
import com.zephyr.boreal.api.dto.response.ReceiptVendorDto
import com.zephyr.boreal.domain.model.InvoiceType

@Entity(tableName = "receipts")
data class ReceiptEntity(
  @PrimaryKey val id: Int,
  val companyId: Int,
  val companyCode: String,
  val partnerId: Int,
  val partnerCode: String,
  val partnerSiteCode: String,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int?,
  val cancelYearCode: Int?,
  val vendor: ReceiptVendorDto,
  val buyer: ReceiptBuyerDto,
  val invoiceDate: String,
  val fulfillmentDate: String,
  val invoiceType: InvoiceType,
  val paidDate: String,
  val user: ReceiptUserDto?,
  val items: List<ReceiptItemDto>,
  val otherItems: List<ReceiptOtherItemDto>,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmountDto>,
  val roundAmount: Double,
  val roundedAmount: Double,
  val lastDownloadedAt: String?,
  val createdAt: String,
  val updatedAt: String,
)
