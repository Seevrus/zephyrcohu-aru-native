package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a receipt under construction (equivalent to `Partial<ContextReceipt>` in TypeScript).
 * Fields are nullable or default to empty values so they can be set incrementally during the sell flow.
 */
@Serializable
data class DraftReceipt(
  val partnerId: Int? = null,
  val partnerCode: String? = null,
  val partnerSiteCode: String? = null,
  val buyer: ReceiptBuyer? = null,
  val paymentDays: Int? = null,
  val invoiceType: InvoiceType? = null,
  val items: List<DraftReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
)

/**
 * A receipt line item while still under construction. Unlike the finalized [ReceiptItem]
 * returned by the API, this carries [expirationId] so the sell flow can track which specific
 * inventory batch the quantity was taken from (for review-list keys, removal, and local
 * storage decrements). The backend never accepts or returns expirationId for receipts, so it
 * has no place on the finalized [ReceiptItem].
 */
@Serializable
data class DraftReceiptItem(
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
  val discountName: String?,
  val expirationId: Int,
  val cnCode: String,
  val expiresAt: String,
  val availableDiscounts: List<Discount> = emptyList(),
  val selectedDiscounts: List<SelectedDiscount> = emptyList(),
)

/**
 * Represents a discount that was selected for a specific item quantity.
 */
@Serializable
data class SelectedDiscount(
  val id: Int,
  val name: String,
  val type: DiscountType,
  val quantity: Double,
  val amount: Double? = null,
  val price: Double? = null,
)
