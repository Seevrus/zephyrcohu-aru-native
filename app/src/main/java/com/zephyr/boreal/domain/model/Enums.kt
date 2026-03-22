package com.zephyr.boreal.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LocationType {
  @SerialName("C")
  CENTRAL,

  @SerialName("D")
  DELIVERY,
}

@Serializable
enum class UserState {
  @SerialName("I")
  IDLE,

  @SerialName("L")
  LOADING,

  @SerialName("R")
  ON_ROUND,
}

@Serializable
enum class StoreType {
  @SerialName("P")
  PRIMARY,

  @SerialName("S")
  SECONDARY,
}

@Serializable
enum class InvoiceType {
  @SerialName("P")
  PAPER,

  @SerialName("E")
  ELECTRONIC,
}

@Serializable
enum class DiscountType {
  @SerialName("absolute")
  ABSOLUTE,

  @SerialName("percentage")
  PERCENTAGE,

  @SerialName("freeForm")
  FREE_FORM,
}

@Serializable
enum class TaxpayerAddressType {
  @SerialName("HQ")
  HEADQUARTERS,

  @SerialName("SITE")
  SITE,
}

@Serializable
enum class UserRole {
  @SerialName("A")
  APP,

  @SerialName("AM")
  ADMIN,

  @SerialName("I")
  INTEGRA,

  @SerialName("L1")
  LOAD_OWNED,

  @SerialName("L2")
  LOAD_ANY,

  @SerialName("NP")
  NEW_PARTNER,
}
