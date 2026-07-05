package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReceiptTotalsCalculatorTest {
  private fun buildItem(
    id: Int = 1,
    expirationId: Int = 1,
    vatRate: String = "27",
    netAmount: Double = 100.0,
    vatAmount: Double = 27.0,
    grossAmount: Double = 127.0,
    quantity: Double = 1.0,
  ) = DraftReceiptItem(
    id = id,
    articleNumber = "ART-$id",
    name = "Item $id",
    quantity = quantity,
    unitName = "db",
    netPrice = netAmount / quantity,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    discountName = null,
    expirationId = expirationId,
    cnCode = "CN001",
    expiresAt = "2026-12-31",
  )

  private fun buildOtherItem(
    id: Int = 10,
    vatRate: String = "27",
    netAmount: Double = 50.0,
    vatAmount: Double = 13.5,
    grossAmount: Double = 63.5,
    quantity: Double = 1.0,
  ) = ReceiptOtherItem(
    id = id,
    articleNumber = "OTH-$id",
    name = "Other Item $id",
    quantity = quantity,
    unitName = "db",
    netPrice = netAmount / quantity,
    netAmount = netAmount,
    vatRate = vatRate,
    vatAmount = vatAmount,
    grossAmount = grossAmount,
    comment = null,
  )

  @Test
  fun `calculateReceiptTotals sums quantity net vat and gross across items and other items`() {
    val item = buildItem(netAmount = 100.0, vatAmount = 27.0, grossAmount = 127.0, quantity = 2.0)
    val otherItem = buildOtherItem(netAmount = 50.0, vatAmount = 13.5, grossAmount = 63.5, quantity = 1.0)

    val totals = calculateReceiptTotals(items = listOf(item), otherItems = listOf(otherItem))

    assertEquals(3.0, totals.quantity)
    assertEquals(150.0, totals.netAmount)
    assertEquals(40.5, totals.vatAmount)
    assertEquals(190.5, totals.grossAmount)
  }

  @Test
  fun `calculateReceiptTotals groups vatAmounts by vatRate`() {
    val item27 = buildItem(id = 1, vatRate = "27", netAmount = 100.0, vatAmount = 27.0, grossAmount = 127.0)
    val item5 = buildItem(id = 2, vatRate = "5", netAmount = 200.0, vatAmount = 10.0, grossAmount = 210.0)
    val otherItem27 = buildOtherItem(id = 10, vatRate = "27", netAmount = 50.0, vatAmount = 13.5, grossAmount = 63.5)

    val totals = calculateReceiptTotals(items = listOf(item27, item5), otherItems = listOf(otherItem27))

    assertEquals(2, totals.vatAmounts.size)
    val rate27 = totals.vatAmounts.first { it.vatRate == "27" }
    assertEquals(150.0, rate27.netAmount)
    assertEquals(40.5, rate27.vatAmount)
    assertEquals(190.5, rate27.grossAmount)
    val rate5 = totals.vatAmounts.first { it.vatRate == "5" }
    assertEquals(200.0, rate5.netAmount)
    assertEquals(10.0, rate5.vatAmount)
    assertEquals(210.0, rate5.grossAmount)
  }

  @Test
  fun `calculateReceiptTotals handles empty lists`() {
    val totals = calculateReceiptTotals(items = emptyList(), otherItems = emptyList())

    assertEquals(0.0, totals.quantity)
    assertEquals(0.0, totals.netAmount)
    assertEquals(0.0, totals.vatAmount)
    assertEquals(0.0, totals.grossAmount)
    assertEquals(emptyList<Any>(), totals.vatAmounts)
  }
}
