package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

data class Partner(
  val id: Int,
  val code: String,
  val siteCode: String,
  val name: String,
  val vatNumber: String? = null,
  val invoiceType: InvoiceType,
  val invoiceCopies: Int,
  val paymentDays: Int,
  val iban: String? = null,
  val bankAccount: String? = null,
  val phoneNumber: String? = null,
  val email: String? = null,
  val locations: List<PartnerLocation> = emptyList(),
  val priceList: PartnerPriceList? = null,
  val createdAt: String,
  val updatedAt: String,
)

data class PartnerPriceList(
  val id: Int,
  val code: String? = null,
  val name: String,
)

@Serializable
data class PartnerLocation(
  val name: String,
  val locationType: LocationType,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val createdAt: String,
  val updatedAt: String,
)
