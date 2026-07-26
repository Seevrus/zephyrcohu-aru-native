package com.zephyr.boreal.data.local.record

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Dedicated persistence types for the `receipts` table (a durable record, not a cache — see
 * `rn-migration-tasks/03f-persistence-durability-classes.md`). Deliberately not the `api/dto`
 * types: a later domain or wire-format refactor must not be able to invalidate stored receipts.
 * Property names (and the one [SerialName] override) mirror [com.zephyr.boreal.api.dto.response]'s
 * `Receipt*Dto` types exactly, so the JSON already on disk decodes unchanged under these types —
 * the schema split is a type rename, not a data transform.
 */
@Serializable
data class PersistedReceiptVendor(
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val felir: String,
  val iban: String,
  val bankAccount: String,
  val vatNumber: String,
)

@Serializable
data class PersistedReceiptBuyer(
  val id: Int,
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val deliveryName: String,
  val deliveryCountry: String,
  val deliveryPostalCode: String,
  val deliveryCity: String,
  val deliveryAddress: String,
  val iban: String? = null,
  val bankAccount: String? = null,
  val vatNumber: String? = null,
)

@Serializable
data class PersistedReceiptUser(
  val id: Int,
  val userName: String,
  val name: String,
  val phoneNumber: String,
)

@Serializable
data class PersistedReceiptItem(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val quantity: Double,
  val unitName: String,
  val netPrice: Double,
  val netAmount: Double,
  val vatRate: String,
  val vatAmount: Double,
  val grossAmount: Double,
  val discountName: String? = null,
  @SerialName("CNCode")
  val cnCode: String,
  val expiresAt: String,
)

@Serializable
data class PersistedReceiptOtherItem(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val quantity: Double,
  val unitName: String,
  val netPrice: Double,
  val netAmount: Double,
  val vatRate: String,
  val vatAmount: Double,
  val grossAmount: Double,
  val comment: String? = null,
)

@Serializable
data class PersistedReceiptVatAmount(
  val vatRate: String,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
)
