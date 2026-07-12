package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.SelectedDiscount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiscountCalculatorTest {
  @Test
  fun `calculateDiscountedUnitNetPrice subtracts a fixed amount for absolute discounts`() {
    val discount =
      SelectedDiscount(id = 1, name = "Fix kedvezmény", type = DiscountType.ABSOLUTE, quantity = 1.0, amount = 200.0)

    val result = DiscountCalculator.calculateDiscountedUnitNetPrice(1000.0, discount)

    assertEquals(800.0, result)
  }

  @Test
  fun `calculateDiscountedUnitNetPrice rounds the percentage-discounted price`() {
    // 999 * (100 - 33) / 100 = 669.33 -> round -> 669.0
    val discount =
      SelectedDiscount(
        id = 1,
        name = "Százalékos kedvezmény",
        type = DiscountType.PERCENTAGE,
        quantity = 1.0,
        amount = 33.0,
      )

    val result = DiscountCalculator.calculateDiscountedUnitNetPrice(999.0, discount)

    assertEquals(669.0, result)
  }

  @Test
  fun `calculateDiscountedUnitNetPrice rounds a half-forint percentage result to the nearest even integer`() {
    // 101 * (100 - 50) / 100 = 50.5 -> kotlin.math.round ties towards even -> 50.0 (not 51.0)
    val discount =
      SelectedDiscount(id = 1, name = "Félárú", type = DiscountType.PERCENTAGE, quantity = 1.0, amount = 50.0)

    val result = DiscountCalculator.calculateDiscountedUnitNetPrice(101.0, discount)

    assertEquals(50.0, result)
  }

  @Test
  fun `calculateDiscountedUnitNetPrice uses the entered price for freeForm discounts`() {
    val discount =
      SelectedDiscount(
        id = 1,
        name = "Egyedi kedvezmény",
        type = DiscountType.FREE_FORM,
        quantity = 1.0,
        price = 500.0,
      )

    val result = DiscountCalculator.calculateDiscountedUnitNetPrice(1000.0, discount)

    assertEquals(500.0, result)
  }

  @Test
  fun `calculateDiscountedUnitNetPrice falls back to the original price when freeForm price is null`() {
    val discount = SelectedDiscount(id = 1, name = "Egyedi kedvezmény", type = DiscountType.FREE_FORM, quantity = 1.0)

    val result = DiscountCalculator.calculateDiscountedUnitNetPrice(1000.0, discount)

    assertEquals(1000.0, result)
  }

  @Test
  fun `calculateDiscountedLineAmounts returns plain amounts when no discounts are selected`() {
    val result = DiscountCalculator.calculateDiscountedLineAmounts(100.0, 3.0, "27", emptyList())

    val expected = AmountCalculator.calculateAmounts(100.0, 3.0, "27")
    assertEquals(expected.netAmount, result.netAmount)
    assertEquals(expected.vatAmount, result.vatAmount)
    assertEquals(expected.grossAmount, result.grossAmount)
  }

  @Test
  fun `calculateDiscountedLineAmounts applies an absolute discount to only the discounted quantity`() {
    // 2 units discounted at (100-20)=80, 1 unit undiscounted at 100, vatRate 27%
    val discount = SelectedDiscount(id = 1, name = "Fix", type = DiscountType.ABSOLUTE, quantity = 2.0, amount = 20.0)

    val result = DiscountCalculator.calculateDiscountedLineAmounts(100.0, 3.0, "27", listOf(discount))

    // discounted: net = 80*2=160, vat = round(160*0.27)=43, gross=203
    // remainder: net = 100*1=100, vat = 27, gross = 127
    assertEquals(260.0, result.netAmount)
    assertEquals(70.0, result.vatAmount)
    assertEquals(330.0, result.grossAmount)
  }

  @Test
  fun `calculateDiscountedLineAmounts applies a percentage discount for the full quantity with no remainder`() {
    val discount =
      SelectedDiscount(id = 1, name = "Százalékos", type = DiscountType.PERCENTAGE, quantity = 2.0, amount = 10.0)

    val result = DiscountCalculator.calculateDiscountedLineAmounts(100.0, 2.0, "27", listOf(discount))

    // unit price = round(100*0.9) = 90, net = 180, vat = round(180*0.27)=49, gross=229
    assertEquals(180.0, result.netAmount)
    assertEquals(49.0, result.vatAmount)
    assertEquals(229.0, result.grossAmount)
  }

  @Test
  fun `calculateDiscountedLineAmounts applies a freeForm discount using the entered price`() {
    val discount =
      SelectedDiscount(id = 1, name = "Egyedi", type = DiscountType.FREE_FORM, quantity = 1.0, price = 50.0)

    val result = DiscountCalculator.calculateDiscountedLineAmounts(100.0, 1.0, "27", listOf(discount))

    // net = 50, vat = round(50*0.27) = 14 (13.5 -> round-half-up -> 14), gross = 64
    assertEquals(50.0, result.netAmount)
    assertEquals(14.0, result.vatAmount)
    assertEquals(64.0, result.grossAmount)
  }

  @Test
  fun `calculateDiscountedLineAmounts sums all three discount types plus the undiscounted remainder`() {
    val absolute = SelectedDiscount(id = 1, name = "Fix", type = DiscountType.ABSOLUTE, quantity = 1.0, amount = 10.0)
    val percentage =
      SelectedDiscount(id = 2, name = "Százalékos", type = DiscountType.PERCENTAGE, quantity = 1.0, amount = 20.0)
    val freeForm =
      SelectedDiscount(id = 3, name = "Egyedi", type = DiscountType.FREE_FORM, quantity = 1.0, price = 30.0)

    val result =
      DiscountCalculator.calculateDiscountedLineAmounts(
        100.0,
        5.0,
        "27",
        listOf(absolute, percentage, freeForm),
      )

    // absolute: unit=90, net=90, vat=round(90*.27)=24, gross=114
    // percentage: unit=round(100*.8)=80, net=80, vat=round(80*.27)=22, gross=102
    // freeForm: unit=30, net=30, vat=round(30*.27)=8, gross=38
    // remainder: 2 units at 100, net=200, vat=round(200*.27)=54, gross=254
    assertEquals(90.0 + 80.0 + 30.0 + 200.0, result.netAmount)
    assertEquals(24.0 + 22.0 + 8.0 + 54.0, result.vatAmount)
    assertEquals(114.0 + 102.0 + 38.0 + 254.0, result.grossAmount)
  }
}
