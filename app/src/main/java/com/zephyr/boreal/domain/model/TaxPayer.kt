package com.zephyr.boreal.domain.model

data class TaxPayer(
  val id: Int,
  val vatNumber: String,
  val locations: Map<String, PartnerLocation>,
)
