package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.User
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Ports RN's `createPrintEndErrand.ts` verbatim (the live-round branch only — the historical-round
 * branch used by errand-details is Task 09's concern), including its column widths and literal
 * "átvettem"/"átadtam" handover footer. One deliberate deviation: RN sorts invoice rows
 * lexicographically on the stringified serial number (an artifact of building `{[serial]: line}`
 * objects and sorting on `keys(...)[0]`), so serial 10 prints before serial 9; this port sorts
 * numerically instead, since the column exists to be a numerically ordered invoice list.
 */
object PrintEndErrandTextBuilder {
  private const val HEADER_ID_WIDTH = 7
  private const val HEADER_LABEL_WIDTH = 48
  private const val HEADER_SEPARATOR = "---------------------------"

  private const val ITEMS_SEPARATOR =
    "-------------------------------------------------------------------"
  private const val TOTALS_SEPARATOR =
    "==================================================================="

  private const val SERIAL_WIDTH = 11
  private const val VAT_NUMBER_WIDTH = 13
  private const val BUYER_NAME_WIDTH = 29
  private const val AMOUNT_WIDTH = 9

  private const val SUMMARY_LABEL_WIDTH = 57

  private const val FOOTER_DASHES = "------------------"
  private const val FOOTER_LEFT_LABEL_WIDTH = 47
  private const val FOOTER_TAKEOVER_WIDTH = 37
  private const val FOOTER_HANDOVER_WIDTH = 18
  private const val FOOTER_QUANTITY_WIDTH = 15
  private const val FOOTER_TOTAL_AMOUNT_WIDTH = 11

  private val headerDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.")
  private val rowDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

  fun build(
    receipts: List<Receipt>,
    round: Round,
    user: User,
  ): String {
    val totals = computeTotals(receipts)
    val header = header(round, user)
    val receiptsList = receiptsList(receipts)
    val totalsText = totalsText(totals)
    val footer = footer(totals)

    return "\n$header\n\n$receiptsList$totalsText\n$footer\n"
  }

  private fun header(
    round: Round,
    user: User,
  ): String {
    val today = LocalDate.now().format(headerDateFormatter)
    val roundStarted = parseRoundDate(round.roundStarted).format(rowDateFormatter)
    val roundFinished = (round.roundFinished?.let { parseRoundDate(it) } ?: LocalDate.now()).format(rowDateFormatter)

    return "{b}KÖRELSZÁMOLÁS | $today{/b}\n" +
      "$HEADER_SEPARATOR\n" +
      "Üzletkötő: ${padRightSide(user.userName, HEADER_ID_WIDTH)} ${padRightSide(user.name, HEADER_LABEL_WIDTH)}\n" +
      "Raktár   : ${padRightSide(
        round.store.code,
        HEADER_ID_WIDTH,
      )} ${padRightSide(round.store.name, HEADER_LABEL_WIDTH)}\n" +
      "Kör      : ${padRightSide(round.id.toString(), HEADER_ID_WIDTH)} " +
      "${padRightSide(round.partnerList.name, HEADER_LABEL_WIDTH)}\n" +
      "Kördátum : $roundStarted .. $roundFinished"
  }

  /**
   * The backend sends round dates as plain `yyyy-MM-dd`, but some responses/tests use a full
   * ISO-8601 instant — same dual-format handling as [com.zephyr.boreal.ui.screens.ReviewItemsViewModel].
   */
  private fun parseRoundDate(value: String): LocalDate =
    try {
      Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate()
    } catch (e: DateTimeParseException) {
      LocalDate.parse(value)
    }

  private fun receiptsList(receipts: List<Receipt>): String {
    val rows = mutableListOf<Pair<Int, String>>()

    receipts.forEach { receipt ->
      val receiptType = if (receipt.invoiceType == InvoiceType.ELECTRONIC) "Á" else "K"
      val vatDisplay = receipt.buyer.vatNumber ?: "-"

      rows +=
        receipt.serialNumber to
        "${padLeftSide(receipt.serialNumber, SERIAL_WIDTH)} $receiptType " +
        "${padRightSide(vatDisplay, VAT_NUMBER_WIDTH)} ${padRightSide(receipt.buyer.name, BUYER_NAME_WIDTH)} " +
        "${padLeftSide(receipt.roundedAmount, AMOUNT_WIDTH)}\n"

      if (receipt.isCanceled) {
        val cancelSerialNumber = requireNotNull(receipt.cancelSerialNumber)
        rows +=
          cancelSerialNumber to
          "${padLeftSide(cancelSerialNumber, SERIAL_WIDTH)} $receiptType " +
          "${padRightSide(vatDisplay, VAT_NUMBER_WIDTH)} ${padRightSide(receipt.buyer.name, BUYER_NAME_WIDTH)} " +
          "${padLeftSide(receipt.roundedAmount * -1, AMOUNT_WIDTH, signed = true)}\n"
      }
    }

    val sortedRows = rows.sortedBy { it.first }.joinToString(separator = "") { it.second }

    return "Számlaszám  F Adószám       Bolt                          Végösszeg\n" +
      "$ITEMS_SEPARATOR\n" +
      sortedRows +
      "$ITEMS_SEPARATOR\n"
  }

  private data class Totals(
    val quantity: Double,
    val amount: Double,
    val cashAmount: Double,
    val numberOfReceipts: Int,
  )

  private fun computeTotals(receipts: List<Receipt>): Totals =
    receipts.fold(Totals(0.0, 0.0, 0.0, 0)) { acc, receipt ->
      val numberOfReceipts = acc.numberOfReceipts + if (receipt.cancelSerialNumber == null) 1 else 2

      if (receipt.isCanceled) {
        acc.copy(numberOfReceipts = numberOfReceipts)
      } else {
        acc.copy(
          quantity = acc.quantity + receipt.quantity,
          amount = acc.amount + receipt.roundedAmount,
          cashAmount = acc.cashAmount + if (receipt.paymentDays > 0) 0.0 else receipt.roundedAmount,
          numberOfReceipts = numberOfReceipts,
        )
      }
    }

  private fun totalsText(totals: Totals): String =
    "${padRightSide("Összesen (${totals.numberOfReceipts} db számla)", SUMMARY_LABEL_WIDTH)} " +
      "${padLeftSide(totals.amount, AMOUNT_WIDTH)}\n" +
      "${padRightSide(
        "Készpénzes számlák összesen",
        SUMMARY_LABEL_WIDTH,
      )} ${padLeftSide(totals.cashAmount, AMOUNT_WIDTH)}\n" +
      TOTALS_SEPARATOR

  private fun footer(totals: Totals): String =
    "A számlákat és a készpénzt:\n" +
      "\n" +
      "\n" +
      "\n" +
      "${padLeftSide(FOOTER_DASHES, FOOTER_LEFT_LABEL_WIDTH)}  $FOOTER_DASHES\n" +
      "${padLeftSide("átvettem", FOOTER_TAKEOVER_WIDTH)} ${padLeftSide("átadtam", FOOTER_HANDOVER_WIDTH)}\n" +
      "$ITEMS_SEPARATOR\n" +
      "Számlázott készlet összesen: ${padLeftSide(totals.quantity, FOOTER_QUANTITY_WIDTH)} karton, " +
      "${padLeftSide(totals.amount, FOOTER_TOTAL_AMOUNT_WIDTH)} Ft\n" +
      ITEMS_SEPARATOR

  private fun formatNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) {
      rounded.toLong().toString()
    } else {
      rounded.toString().trimEnd('0').trimEnd('.')
    }
  }

  private fun padLeftSide(
    target: Double,
    length: Int,
    signed: Boolean = false,
  ): String {
    val formatted = formatNumber(target)
    val display = if (signed && target > 0) "+$formatted" else formatted
    return TextPadding.padLeftSide(display, length)
  }

  private fun padLeftSide(
    target: Int,
    length: Int,
  ): String = TextPadding.padLeftSide(target.toString(), length)

  private fun padLeftSide(
    target: String,
    length: Int,
  ): String = TextPadding.padLeftSide(target, length)

  private fun padRightSide(
    target: String,
    length: Int,
  ): String = TextPadding.padRightSide(target, length)
}
