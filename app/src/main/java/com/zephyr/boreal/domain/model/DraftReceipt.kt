package com.zephyr.boreal.domain.model

/**
 * Represents a receipt under construction (equivalent to `Partial<ContextReceipt>` in TypeScript).
 * Fields are nullable or default to empty values so they can be set incrementally during the sell flow.
 */
data class DraftReceipt(
  val partnerId: Int? = null,
  val partnerCode: String? = null,
  val partnerSiteCode: String? = null,
  val buyer: ReceiptBuyer? = null,
  val paymentDays: Int? = null,
  val invoiceType: InvoiceType? = null,
  val items: List<ReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
)

/**
 * Represents a discount that was selected for a specific item quantity.
 */
data class SelectedDiscount(
  val id: Int,
  val name: String,
  val type: DiscountType,
  val quantity: Double,
  val amount: Double? = null,
  val price: Double? = null,
)
