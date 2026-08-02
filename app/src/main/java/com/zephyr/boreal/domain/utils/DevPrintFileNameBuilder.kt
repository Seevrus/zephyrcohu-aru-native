package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.StoreDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds the suggested filename for the dev-mode "save print string" flow (Task 03b):
 * `{partnerName}_{invoiceDate}_{receiptSerialNumber}.txt`. Uses [Receipt.buyer]'s `name` directly
 * (the same field [PrintReceiptTextBuilder] renders in the buyer block), so ad-hoc partners (no
 * `Partner` record) resolve consistently without needing a separate fallback.
 */
object DevPrintFileNameBuilder {
  private val ILLEGAL_FILENAME_CHARS = Regex("[\\\\/:*?\"<>|]")
  private val FILE_NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  fun build(receipt: Receipt): String {
    val safePartnerName =
      receipt.buyer.name
        .replace(ILLEGAL_FILENAME_CHARS, "_")
        .trim()
    return "${safePartnerName}_${receipt.invoiceDate}_${receipt.serialNumber}.txt"
  }

  /**
   * Same identifier+date shape as [build], but for the storage-changeset print (no
   * receipt/serial-number to key off of, so the store's code+name plus today's date fill in).
   */
  fun buildForStorage(storeDetails: StoreDetails): String {
    val safeStoreName =
      storeDetails.name
        .replace(ILLEGAL_FILENAME_CHARS, "_")
        .trim()
    val date = LocalDate.now().format(FILE_NAME_DATE_FORMAT)
    return "${safeStoreName}_${storeDetails.code}_$date.txt"
  }

  /**
   * Same identifier+date shape as [build]/[buildForStorage], keyed on the round's store and id
   * since a round summary has no single receipt to key off of.
   */
  fun buildForRound(round: Round): String {
    val safeStoreName =
      round.store.name
        .replace(ILLEGAL_FILENAME_CHARS, "_")
        .trim()
    val date = LocalDate.now().format(FILE_NAME_DATE_FORMAT)
    return "korelszamolas_${safeStoreName}_${round.id}_$date.txt"
  }
}
