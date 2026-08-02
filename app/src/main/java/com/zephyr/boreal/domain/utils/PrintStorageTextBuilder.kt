package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import java.text.Collator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ports RN's `createPrintStorageChanges.ts` verbatim, including its column widths, `{b}...{/b}`
 * printer bold-markup tokens, and the RN quirk where the totals section sums over every
 * [StorageListItem] passed in, even when [printAll] is false and the itemized list above it only
 * shows the changed rows. This is a behavioral port, not a redesign, so that quirk is intentional.
 */
object PrintStorageTextBuilder {
  private const val ITEMS_SEPARATOR =
    "-------------------------------------------------------------------"
  private const val TOTALS_SEPARATOR =
    "==================================================================="
  private const val HEADER_SEPARATOR = "------------------------"
  private const val FOOTER_LINE_WIDTH = 67

  private const val INDEX_PAD_WIDTH = 3
  private const val MAX_ZERO_PADDED_INDEX = 999

  private const val ARTICLE_NUMBER_WIDTH = 6
  private const val EXPIRES_AT_WIDTH = 7
  private const val BARCODE_WIDTH = 13
  private const val NAME_WIDTH = 37
  private const val UNIT_NAME_WIDTH = 6
  private const val QUANTITY_WIDTH = 5
  private const val QUANTITY_CHANGE_WIDTH = 6

  private const val SUMMARY_LABEL_WIDTH = 44
  private const val SUMMARY_OUT_WIDTH = 57

  private const val FOOTER_DASH_LENGTH = 20
  private const val FOOTER_LEFT_LABEL_WIDTH = 40
  private const val FOOTER_RIGHT_LABEL_WIDTH = 25

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.")

  private fun huCollator(): Collator =
    Collator.getInstance(
      Locale
        .Builder()
        .setLanguage("hu")
        .setRegion("HU")
        .build(),
    )

  private fun mapToNr(index: Int): String {
    val stringIndex = index.toString()
    return if (index > MAX_ZERO_PADDED_INDEX) {
      stringIndex
    } else {
      "0".repeat(INDEX_PAD_WIDTH - stringIndex.length) + stringIndex + "."
    }
  }

  private fun padLeftSide(
    target: Int,
    length: Int,
    signed: Boolean = false,
  ): String {
    val text = if (signed && target > 0) "+$target" else target.toString()
    return TextPadding.padLeftSide(text, length)
  }

  private fun padLeftSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String = TextPadding.padLeftSide(target, length, fillString)

  private fun padRightSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String = TextPadding.padRightSide(target, length, fillString)

  fun build(
    printAll: Boolean,
    storageItems: List<StorageListItem>,
    storeDetails: StoreDetails,
    user: User,
  ): String {
    val today = LocalDate.now().format(dateFormatter)
    val header = header(today, storeDetails, user)
    val itemsList = itemsList(printAll, storageItems)
    val summary = itemsSummarySection(storageItems)
    val footer = footer(today, user)

    return "\n" +
      "$header\n\n" +
      "$itemsList$summary\n\n\n\n" +
      "$footer\n"
  }

  private fun header(
    today: String,
    storeDetails: StoreDetails,
    user: User,
  ): String =
    "{b}RAKJEGYZÉK | $today{/b}\n" +
      "$HEADER_SEPARATOR\n" +
      "Rakodta: ${user.userName} ${user.name}\n" +
      "Raktár : ${storeDetails.code} ${storeDetails.name}"

  private fun itemsList(
    printAll: Boolean,
    storageItems: List<StorageListItem>,
  ): String {
    val collator = huCollator()
    val filteredSorted =
      storageItems
        .filter { printAll || (it.quantityChange ?: 0) != 0 }
        .sortedWith { a, b -> collator.compare(a.name, b.name) }

    val itemRows =
      filteredSorted.mapIndexed { index, item ->
        val quantityChange = item.quantityChange ?: 0
        val originalQuantity = (item.originalQuantity ?: 0.0).toInt()
        val currentQuantity = originalQuantity + quantityChange

        "${mapToNr(index + 1)} ${padRightSide(item.articleNumber, ARTICLE_NUMBER_WIDTH)} " +
          "${padRightSide(item.expiresAt, EXPIRES_AT_WIDTH)} " +
          "${padRightSide((item.itemBarcode + item.expirationBarcode).trim(), BARCODE_WIDTH)}\n" +
          "${padRightSide(item.name, NAME_WIDTH)} ${padRightSide(item.unitName, UNIT_NAME_WIDTH)} " +
          "${padLeftSide(originalQuantity, QUANTITY_WIDTH)} " +
          "${padLeftSide(quantityChange, QUANTITY_CHANGE_WIDTH, signed = true)} " +
          "${padLeftSide(currentQuantity, QUANTITY_WIDTH)} [ ]\n" +
          "$ITEMS_SEPARATOR\n"
      }

    return "SSz. Cikk   Lejárat                   Menny. Mennyiségek\n" +
      "Megnevezés                            egység Kezdő Mozgás Kész  Ok?\n" +
      "$ITEMS_SEPARATOR\n" +
      itemRows.joinToString(separator = "")
  }

  private data class ItemsSummary(
    val originalQuantity: Int = 0,
    val inQuantity: Int = 0,
    val outQuantity: Int = 0,
    val currentQuantity: Int = 0,
  )

  private fun itemsSummarySection(storageItems: List<StorageListItem>): String {
    val summary =
      storageItems.fold(ItemsSummary()) { acc, item ->
        val quantityChange = item.quantityChange ?: 0
        val originalQuantity = (item.originalQuantity ?: 0.0).toInt()
        ItemsSummary(
          originalQuantity = acc.originalQuantity + originalQuantity,
          inQuantity = if (quantityChange > 0) acc.inQuantity + quantityChange else acc.inQuantity,
          outQuantity = if (quantityChange < 0) acc.outQuantity + quantityChange else acc.outQuantity,
          currentQuantity = acc.currentQuantity + originalQuantity + quantityChange,
        )
      }

    return "${padRightSide("Összesen (rakodva ${storageItems.size} termék)", SUMMARY_LABEL_WIDTH)} " +
      "${padLeftSide(summary.originalQuantity, QUANTITY_WIDTH)} " +
      "${padLeftSide(summary.inQuantity, QUANTITY_CHANGE_WIDTH, signed = true)}\n" +
      "${padLeftSide(summary.outQuantity, SUMMARY_OUT_WIDTH, signed = true)} " +
      "${padLeftSide(summary.currentQuantity, QUANTITY_WIDTH)}\n" +
      TOTALS_SEPARATOR
  }

  private fun footer(
    today: String,
    user: User,
  ): String =
    "${padLeftSide("-".repeat(FOOTER_DASH_LENGTH), FOOTER_LEFT_LABEL_WIDTH)} " +
      "${padLeftSide("-".repeat(FOOTER_DASH_LENGTH), FOOTER_RIGHT_LABEL_WIDTH)}\n" +
      "${padLeftSide("rakodta", FOOTER_LEFT_LABEL_WIDTH)} ${padLeftSide("ellenőrizte", FOOTER_RIGHT_LABEL_WIDTH)}\n" +
      padRightSide("|> $today | ${user.userName} | ${user.name} <|", FOOTER_LINE_WIDTH, "-")
}
