package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.api.dto.response.BasePriceListDto
import com.zephyr.boreal.api.dto.response.PartnerLocationDto
import com.zephyr.boreal.domain.model.InvoiceType

@Entity(tableName = "partners")
data class PartnerEntity(
  @PrimaryKey val id: Int,
  val code: String,
  val siteCode: String,
  val vatNumber: String?,
  val invoiceType: InvoiceType,
  val invoiceCopies: Int,
  val paymentDays: Int,
  val iban: String?,
  val bankAccount: String?,
  val phoneNumber: String?,
  val email: String?,
  val locations: List<PartnerLocationDto>,
  val priceList: BasePriceListDto?,
  val createdAt: String,
  val updatedAt: String,
)
