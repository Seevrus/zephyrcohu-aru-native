package com.zephyr.boreal.api.dto.response

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.LocationType
import kotlinx.serialization.Serializable

@Serializable
data class PartnerLocationDto(
  val name: String,
  val locationType: LocationType,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val createdAt: String,
  val updatedAt: String,
)

@Serializable
data class PartnerResponseDataDto(
  val id: Int,
  val code: String,
  val siteCode: String,
  val vatNumber: String? = null,
  val invoiceType: InvoiceType,
  val invoiceCopies: Int,
  val paymentDays: Int,
  val iban: String? = null,
  val bankAccount: String? = null,
  val phoneNumber: String? = null,
  val email: String? = null,
  val locations: List<PartnerLocationDto>,
  val createdAt: String,
  val updatedAt: String,
  val priceList: BasePriceListDto? = null,
)

@Serializable
data class BasePriceListDto(
  val id: Int,
  val code: String? = null,
  val name: String,
)

@Serializable
data class PartnersResponseDto(
  val data: List<PartnerResponseDataDto>,
)
