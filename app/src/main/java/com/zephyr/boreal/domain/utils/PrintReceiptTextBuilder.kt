package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptVatAmount
import com.zephyr.boreal.domain.model.User
import java.text.Collator
import java.util.Locale

/**
 * Ports RN's `createPrintReceipt.ts` + `usePrintSection.ts` copy-count logic verbatim, including
 * its row-width contract (67 characters) and literal Hungarian body text. This is receipt-body
 * text sent directly to the Datecs printer, not app UI copy, so it lives here rather than
 * `strings.xml` — matching how the RN app kept it out of its i18n layer too.
 */
object PrintReceiptTextBuilder {
  private const val ROW_WIDTH = 67
  private const val DEFAULT_COPIES = 3
  private const val AMOUNT_FIELD_WIDTH = 10
  private const val VAT_RATE_FIELD_WIDTH = 3
  private const val TRUNCATION_SUFFIX_LENGTH = 3

  // Lengths of the literal "STORNO SZÁMLA " prefix and " (Stornozott számla száma: )" suffix,
  // used to compute how much filler space remains on the cancel-title row within [ROW_WIDTH].
  private const val CANCEL_TITLE_LENGTH = 13
  private const val CANCEL_SUFFIX_LENGTH = 27

  // Fixed field widths for the vendor/buyer/delivery/metadata blocks (all sum to ROW_WIDTH once
  // their fixed "Neve: "/"Bank: "/label prefixes are included).
  private const val ADDRESS_LINE_WIDTH = 61
  private const val VENDOR_NAME_WIDTH = 45
  private const val FELIR_WIDTH = 9
  private const val BANK_ACCOUNT_WIDTH = 31
  private const val TAX_NUMBER_LABEL_WIDTH = 29
  private const val METADATA_DATE_WIDTH = 35
  private const val SALES_AGENT_NAME_WIDTH = 40
  private const val PHONE_NUMBER_WIDTH = 15

  private val newHuCollator: () -> Collator = {
    Collator.getInstance(
      Locale
        .Builder()
        .setLanguage("hu")
        .setRegion("HU")
        .build(),
    )
  }

  private const val PACKING_INFO =
    "A számlán  szereplő  csomagolóanyagok  mentesek  a termékdíjfizetés\n" +
      "alól, a termékdíj megfizetésre került. A NÉTA az eladót terheli."

  private const val FOOTER =
    "                      --------------------      -------------------\n" +
      "                              eladó                     vevő\n" +
      "KÖSZÖNJÜK A VÁSÁRLÁST! --------------------------------------------"

  private data class PrintLineItem(
    val name: String,
    val cnCode: String,
    val articleNumber: String,
    val expiresAt: String,
    val quantity: Double,
    val unitName: String,
    val netPrice: Double,
    val netAmount: Double,
    val vatRate: String,
    val vatAmount: Double,
    val grossAmount: Double,
    val comment: String?,
  )

  fun copyCount(
    partner: Partner?,
    receipt: Receipt,
  ): Int =
    when {
      partner == null -> DEFAULT_COPIES
      receipt.isPrinted || partner.invoiceType == InvoiceType.ELECTRONIC -> 2
      else -> partner.invoiceCopies
    }

  fun receiptFormatLabel(partner: Partner?): String =
    if (partner?.invoiceType == InvoiceType.ELECTRONIC) "elektronikus" else "papír alapú"

  fun build(
    isCancel: Boolean,
    user: User,
    receipt: Receipt,
    partner: Partner?,
  ): String {
    val copies = copyCount(partner, receipt)
    return (1..copies)
      .joinToString(separator = "\n\n\n${"-".repeat(ROW_WIDTH)}\n\n\n") { copy ->
        oneReceiptBody(isCancel, user, receipt, partner, copy)
      }
  }

  private fun oneReceiptBody(
    isCancel: Boolean,
    user: User,
    receipt: Receipt,
    partner: Partner?,
    copy: Int,
  ): String {
    val header = header(isCancel, receipt, partner, copy)
    val vendor = vendorBlock(receipt)
    val buyer = buyerBlock(receipt)
    val hasDeliveryData = receipt.buyer.deliveryName.isNotEmpty()
    val deliveryData = if (hasDeliveryData) deliveryBlock(receipt) else ""
    val metadata = metadataBlock(user, receipt, partner)
    val totals = totalsBlock(isCancel, receipt, partner)

    return "\n" +
      "$header\n\n" +
      "$vendor\n\n" +
      "$buyer\n" +
      deliveryData +
      "\n\n" +
      "=".repeat(ROW_WIDTH) + "\n" +
      "$metadata\n" +
      itemsList(isCancel, receipt.items, receipt.otherItems) +
      totals +
      "$PACKING_INFO\n\n\n\n" +
      FOOTER
  }

  private fun header(
    isCancel: Boolean,
    receipt: Receipt,
    partner: Partner?,
    copy: Int,
  ): String {
    val displayedCancelSerialNumber =
      if (isCancel) "${receipt.cancelSerialNumber}/${receipt.yearCode}" else null
    val displayedSerialNumber = "${receipt.serialNumber}/${receipt.yearCode}"

    val remainingSpaceBetweenCancelTitleAndDescription =
      ROW_WIDTH - CANCEL_TITLE_LENGTH - CANCEL_SUFFIX_LENGTH - displayedSerialNumber.length - 2

    val title =
      if (isCancel) {
        "STORNO SZÁMLA ${padRightSide("", remainingSpaceBetweenCancelTitleAndDescription)} " +
          "(Stornozott számla száma: $displayedSerialNumber)\n-------------"
      } else {
        "SZÁMLA\n------"
      }

    val copiesLabel =
      if (partner?.invoiceType == InvoiceType.ELECTRONIC || receipt.isPrinted) {
        "Másolat"
      } else {
        "Eredeti: $copy./${partner?.invoiceCopies ?: DEFAULT_COPIES} példány"
      }

    val headerSn = if (isCancel) requireNotNull(displayedCancelSerialNumber) else displayedSerialNumber
    val remainingSpaceBetweenSnAndCopies = ROW_WIDTH - headerSn.length - copiesLabel.length - 2

    return "$title\n$headerSn ${padRightSide("", remainingSpaceBetweenSnAndCopies, "-")} $copiesLabel"
  }

  private fun vendorBlock(receipt: Receipt): String =
    "------------------------------|Eladó|------------------------------\n" +
      "Neve: ${padRightSide(receipt.vendor.name, VENDOR_NAME_WIDTH)}" +
      "|FELIR:${padRightSide(receipt.vendor.felir, FELIR_WIDTH)}\n" +
      "Címe: ${
        padRightSide(
          "${receipt.vendor.postalCode} ${receipt.vendor.city}, ${receipt.vendor.address}",
          ADDRESS_LINE_WIDTH,
        )
      }\n" +
      "Bank: ${padRightSide("${receipt.vendor.iban}-${receipt.vendor.bankAccount}", BANK_ACCOUNT_WIDTH)} " +
      padLeftSide("| Adószám: ${receipt.vendor.vatNumber}", TAX_NUMBER_LABEL_WIDTH)

  private fun buyerBlock(receipt: Receipt): String {
    val buyerBankAccount =
      if (!receipt.buyer.iban.isNullOrEmpty() && !receipt.buyer.bankAccount.isNullOrEmpty()) {
        "${receipt.buyer.iban}-${receipt.buyer.bankAccount}"
      } else {
        "-"
      }
    val buyerVatNumberDisplay =
      if (!receipt.buyer.vatNumber.isNullOrEmpty()) {
        padLeftSide("| Adószám: ${receipt.buyer.vatNumber}", TAX_NUMBER_LABEL_WIDTH)
      } else {
        ""
      }

    return "-------------------------|Vevő (partner)|--------------------------\n" +
      "Neve: ${padRightSide(receipt.buyer.name, ADDRESS_LINE_WIDTH)}\n" +
      "Címe: ${
        padRightSide(
          "${receipt.buyer.postalCode} ${receipt.buyer.city}, ${receipt.buyer.address}",
          ADDRESS_LINE_WIDTH,
        )
      }\n" +
      "Bank: ${padRightSide(buyerBankAccount, BANK_ACCOUNT_WIDTH)} $buyerVatNumberDisplay"
  }

  private fun deliveryBlock(receipt: Receipt): String =
    "-------------------|Vevő (bolt, szállítási cím)|-------------------\n" +
      "Neve: ${padRightSide(receipt.buyer.deliveryName, ADDRESS_LINE_WIDTH)}\n" +
      "Címe: ${
        padRightSide(
          "${receipt.buyer.deliveryPostalCode} ${receipt.buyer.deliveryCity}, ${receipt.buyer.deliveryAddress}",
          ADDRESS_LINE_WIDTH,
        )
      }"

  private fun metadataBlock(
    user: User,
    receipt: Receipt,
    partner: Partner?,
  ): String {
    val paymentMethodDisplay = if ((partner?.paymentDays ?: 0) > 0) "Átutalás" else "Készpénz;"
    val phoneNumber = user.phoneNumber ?: user.company.phoneNumber ?: ""

    return "${padRightSide("Kelt és teljesítés: ${receipt.invoiceDate}", METADATA_DATE_WIDTH)} | " +
      padLeftSide("Fizetés: $paymentMethodDisplay, ${receipt.paidDate}", TAX_NUMBER_LABEL_WIDTH) + "\n" +
      "Üzletkötő: ${padRightSide(user.name, SALES_AGENT_NAME_WIDTH)} " +
      "${padLeftSide(phoneNumber, PHONE_NUMBER_WIDTH)}\n" +
      "Szoftver:            Zephyr Boreal 11.40.0000 | zephyr.bt@gmail.com\n" +
      "-".repeat(ROW_WIDTH)
  }

  private fun totalsBlock(
    isCancel: Boolean,
    receipt: Receipt,
    partner: Partner?,
  ): String {
    val amountMultiplier = if (isCancel) -1 else 1
    val displayedVatAmount = if (receipt.vatAmount == 0.0) "" else formatNumber(receipt.vatAmount * amountMultiplier)
    val total =
      "Összes: ${padRightSide(formatNumber(receipt.quantity * amountMultiplier), 22)} " +
        "${padLeftSide(receipt.netAmount * amountMultiplier, AMOUNT_FIELD_WIDTH, signed = isCancel)}     " +
        "${padLeftSide(displayedVatAmount, AMOUNT_FIELD_WIDTH)} " +
        "${padLeftSide(receipt.grossAmount * amountMultiplier, AMOUNT_FIELD_WIDTH, signed = isCancel)}\n" +
        "=".repeat(ROW_WIDTH)

    val hasRounding = partner == null || partner.paymentDays == 0
    val rounding =
      "Kerekítés (5 forintra) összege                           " +
        padLeftSide(kotlin.math.abs(receipt.roundAmount), AMOUNT_FIELD_WIDTH)

    val totalAmount =
      if (isCancel) {
        "Számla mindösszesen: (visszajáró)                        " +
          padLeftSide(receipt.roundedAmount * -1, AMOUNT_FIELD_WIDTH)
      } else {
        "Számla mindösszesen: (fizetendő)                         " +
          padLeftSide(receipt.roundedAmount, AMOUNT_FIELD_WIDTH) + "\n" +
          "-".repeat(ROW_WIDTH)
      }

    return total + "\n" +
      vatList(isCancel, receipt.vatAmounts) +
      (if (hasRounding) rounding else "") + "\n" +
      "=".repeat(ROW_WIDTH) + "\n" +
      "$totalAmount\n"
  }

  private fun itemsList(
    isCancel: Boolean,
    items: List<ReceiptItem>,
    otherItems: List<ReceiptOtherItem>,
  ): String {
    val amountMultiplier = if (isCancel) -1 else 1
    val collator = newHuCollator()

    val sortedItems = items.map { it.toPrintLineItem() }.sortedWith { a, b -> collator.compare(a.name, b.name) }
    val sortedOtherItems =
      otherItems.map { it.toPrintLineItem() }.sortedWith { a, b -> collator.compare(a.name, b.name) }
    val allItems = sortedItems + sortedOtherItems

    val itemRows =
      allItems.mapIndexed { index, item ->
        val code = padLeftSide(index + 1, 3, fillString = "0")
        val displayedVatRate = if (item.vatAmount == 0.0) item.vatRate else "${item.vatRate}%"
        val displayedVatAmount =
          if (item.vatAmount == 0.0) "" else formatNumber(item.vatAmount * amountMultiplier)

        val cnCodeDisplay = "${item.cnCode.take(4)} ${item.cnCode.takeLast(2)}"
        val expiresAtDisplay =
          item.expiresAt.take(6).let { if (it.length == 6) "${it.take(4)}-${it.takeLast(2)}" else it }

        val commentLine =
          if (!item.comment.isNullOrEmpty()) {
            "\nMegjegyzés: ${padRightSide(item.comment, 55)}"
          } else {
            ""
          }

        "$code|${padRightSide(cnCodeDisplay, 7)} ${padRightSide(item.articleNumber, 6)} " +
          "${padRightSide(expiresAtDisplay, 7)} ${padRightSide(item.name, 40)}\n" +
          "      ${padLeftSide(item.quantity * amountMultiplier, 8, signed = isCancel)} " +
          "${padRightSide(item.unitName, 6)} ${padLeftSide(item.netPrice, 8)} " +
          "${padLeftSide(item.netAmount * amountMultiplier, 10, signed = isCancel)} " +
          "${padLeftSide(displayedVatRate, 3)} ${padLeftSide(displayedVatAmount, 10)} " +
          "${padLeftSide(item.grossAmount * amountMultiplier, 10, signed = isCancel)}$commentLine\n" +
          "-".repeat(ROW_WIDTH) + "\n"
      }

    return "Ssz|VTSZ    Cikk   Lejárat Megnevezés\n" +
      "-".repeat(ROW_WIDTH) + "\n" +
      "      Mennyiség       Nettó               ÁFA kulcsa     Bruttó\n" +
      "      és menny.egység egységár összeg     és összege     összeg\n" +
      "-".repeat(ROW_WIDTH) + "\n" +
      itemRows.joinToString(separator = "")
  }

  private fun vatList(
    isCancel: Boolean,
    vatAmounts: List<ReceiptVatAmount>,
  ): String {
    val amountMultiplier = if (isCancel) -1 else 1
    return vatAmounts.joinToString(separator = "") { amount ->
      val displayedVatRate = if (amount.vatAmount == 0.0) amount.vatRate else "${amount.vatRate}%"
      val displayedVatAmount =
        if (amount.vatAmount == 0.0) "" else formatNumber(amount.vatAmount * amountMultiplier)
      "ÁFA összesen                   " +
        "${padLeftSide(amount.netAmount * amountMultiplier, AMOUNT_FIELD_WIDTH, signed = isCancel)} " +
        "${padLeftSide(displayedVatRate, VAT_RATE_FIELD_WIDTH)} " +
        "${padLeftSide(displayedVatAmount, AMOUNT_FIELD_WIDTH)} " +
        "${padLeftSide(amount.grossAmount * amountMultiplier, AMOUNT_FIELD_WIDTH, signed = isCancel)}\n"
    }
  }

  private fun ReceiptItem.toPrintLineItem() =
    PrintLineItem(
      name = name,
      cnCode = cnCode,
      articleNumber = articleNumber,
      expiresAt = expiresAt,
      quantity = quantity,
      unitName = unitName,
      netPrice = netPrice,
      netAmount = netAmount,
      vatRate = vatRate,
      vatAmount = vatAmount,
      grossAmount = grossAmount,
      comment = discountName,
    )

  private fun ReceiptOtherItem.toPrintLineItem() =
    PrintLineItem(
      name = name,
      cnCode = "",
      articleNumber = articleNumber,
      expiresAt = "",
      quantity = quantity,
      unitName = unitName,
      netPrice = netPrice,
      netAmount = netAmount,
      vatRate = vatRate,
      vatAmount = vatAmount,
      grossAmount = grossAmount,
      comment = comment,
    )

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
    fillString: String = " ",
  ): String {
    val formatted = formatNumber(target)
    val display = if (signed && target > 0) "+$formatted" else formatted
    return padLeftSide(display, length, fillString)
  }

  private fun padLeftSide(
    target: Int,
    length: Int,
    fillString: String = " ",
  ): String = padLeftSide(target.toString(), length, fillString)

  private fun padLeftSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String =
    if (target.length > length) {
      target.take(length - TRUNCATION_SUFFIX_LENGTH) + "..."
    } else {
      target.padStart(length, fillString.first())
    }

  private fun padRightSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String =
    if (target.length > length) {
      target.take(length - TRUNCATION_SUFFIX_LENGTH) + "..."
    } else {
      target.padEnd(length, fillString.first())
    }
}
