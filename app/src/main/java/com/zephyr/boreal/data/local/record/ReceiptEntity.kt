package com.zephyr.boreal.data.local.record

import androidx.room.Entity
import androidx.room.PrimaryKey
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
  val vendor: PersistedReceiptVendor,
  val buyer: PersistedReceiptBuyer,
  val invoiceDate: String,
  val fulfillmentDate: String,
  val invoiceType: InvoiceType,
  val paidDate: String,
  val user: PersistedReceiptUser?,
  val items: List<PersistedReceiptItem>,
  val otherItems: List<PersistedReceiptOtherItem>,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<PersistedReceiptVatAmount>,
  val roundAmount: Double,
  val roundedAmount: Double,
  val lastDownloadedAt: String?,
  val createdAt: String,
  val updatedAt: String,
  val isPrinted: Boolean = false,
)
