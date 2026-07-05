package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptVatAmount

data class ReceiptTotals(
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmount>,
)

private data class LineAmounts(
  val vatRate: String,
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
)

fun calculateReceiptTotals(
  items: List<DraftReceiptItem>,
  otherItems: List<ReceiptOtherItem>,
): ReceiptTotals {
  val lines =
    items.map {
      LineAmounts(it.vatRate, it.quantity, it.netAmount, it.vatAmount, it.grossAmount)
    } +
      otherItems.map {
        LineAmounts(it.vatRate, it.quantity, it.netAmount, it.vatAmount, it.grossAmount)
      }

  val vatAmounts =
    lines
      .groupBy { it.vatRate }
      .map { (vatRate, linesForRate) ->
        ReceiptVatAmount(
          vatRate = vatRate,
          netAmount = linesForRate.sumOf { it.netAmount },
          vatAmount = linesForRate.sumOf { it.vatAmount },
          grossAmount = linesForRate.sumOf { it.grossAmount },
        )
      }

  return ReceiptTotals(
    quantity = lines.sumOf { it.quantity },
    netAmount = lines.sumOf { it.netAmount },
    vatAmount = lines.sumOf { it.vatAmount },
    grossAmount = lines.sumOf { it.grossAmount },
    vatAmounts = vatAmounts,
  )
}
