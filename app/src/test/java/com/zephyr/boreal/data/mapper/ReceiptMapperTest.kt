package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.SelectedDiscount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReceiptMapperTest {
  private fun buildItem(
    quantity: Double = 5.0,
    netPrice: Double = 1000.0,
    selectedDiscounts: List<SelectedDiscount> = emptyList(),
  ) = DraftReceiptItem(
    id = 1,
    articleNumber = "ART-1",
    name = "Item 1",
    quantity = quantity,
    unitName = "db",
    netPrice = netPrice,
    netAmount = netPrice * quantity,
    vatRate = "27",
    vatAmount = 0.0,
    grossAmount = 0.0,
    discountName = null,
    expirationId = 1,
    cnCode = "CN001",
    expiresAt = "202607",
    selectedDiscounts = selectedDiscounts,
  )

  @Test
  fun `toDtos returns a single row identical to toDto when there are no discounts`() {
    val item = buildItem()

    val rows = item.toDtos()

    assertEquals(1, rows.size)
    assertEquals(item.toDto(), rows.first())
  }

  @Test
  fun `toDtos returns a single discounted row with no remainder when the discount covers the full quantity`() {
    val discount =
      SelectedDiscount(id = 1, name = "Fix kedvezmény", type = DiscountType.ABSOLUTE, quantity = 5.0, amount = 200.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(discount))

    val rows = item.toDtos()

    assertEquals(1, rows.size)
    val row = rows.first()
    assertEquals(5.0, row.quantity)
    assertEquals(800.0, row.netPrice)
    assertEquals("Fix kedvezmény", row.discountName)
  }

  @Test
  fun `toDtos returns a discounted row plus a remainder row with no discountName for a partial discount`() {
    val discount =
      SelectedDiscount(id = 1, name = "Fix kedvezmény", type = DiscountType.ABSOLUTE, quantity = 2.0, amount = 200.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(discount))

    val rows = item.toDtos()

    assertEquals(2, rows.size)
    val discountedRow = rows[0]
    assertEquals(2.0, discountedRow.quantity)
    assertEquals(800.0, discountedRow.netPrice)
    assertEquals("Fix kedvezmény", discountedRow.discountName)

    val remainderRow = rows[1]
    assertEquals(3.0, remainderRow.quantity)
    assertEquals(1000.0, remainderRow.netPrice)
    assertNull(remainderRow.discountName)
  }

  @Test
  fun `toDtos returns four rows for all three discount types plus a remainder`() {
    val absolute = SelectedDiscount(id = 1, name = "Fix", type = DiscountType.ABSOLUTE, quantity = 1.0, amount = 100.0)
    val percentage =
      SelectedDiscount(id = 2, name = "Százalékos", type = DiscountType.PERCENTAGE, quantity = 1.0, amount = 10.0)
    val freeForm =
      SelectedDiscount(id = 3, name = "Egyedi", type = DiscountType.FREE_FORM, quantity = 1.0, price = 500.0)
    val item = buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = listOf(absolute, percentage, freeForm))

    val rows = item.toDtos()

    assertEquals(4, rows.size)
    assertEquals(listOf("Fix", "Százalékos", "Egyedi", null), rows.map { it.discountName })
    assertEquals(listOf(1.0, 1.0, 1.0, 2.0), rows.map { it.quantity })
  }
}
