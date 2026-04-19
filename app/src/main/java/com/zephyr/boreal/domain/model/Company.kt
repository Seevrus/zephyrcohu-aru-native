package com.zephyr.boreal.domain.model

data class PartialCompany(
  val id: Int,
  val name: String,
)

data class Company(
  val id: Int,
  val name: String,
  val code: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val felir: String,
  val vatNumber: String,
  val iban: String,
  val bankAccount: String,
  val phoneNumber: String? = null,
  val email: String? = null,
)
