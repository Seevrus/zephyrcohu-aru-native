package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.Receipt

/**
 * Builds the suggested filename for the dev-mode "save print string" flow (Task 03b):
 * `{partnerName}_{invoiceDate}_{receiptSerialNumber}.txt`. Uses [Receipt.buyer]'s `name` directly
 * (the same field [PrintReceiptTextBuilder] renders in the buyer block), so ad-hoc partners (no
 * `Partner` record) resolve consistently without needing a separate fallback.
 */
object DevPrintFileNameBuilder {
  private val ILLEGAL_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|]")

  fun build(receipt: Receipt): String {
    val safePartnerName =
      receipt.buyer.name
        .replace(ILLEGAL_FILENAME_CHARS, "_")
        .trim()
    return "${safePartnerName}_${receipt.invoiceDate}_${receipt.serialNumber}.txt"
  }
}
