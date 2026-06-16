package com.zephyr.boreal.domain.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AmountCalculatorTest {
  @Test
  fun `calculateAmounts computes accurate vat and gross totals`() {
    val netPrice = 100.0
    val quantity = 2.0
    val vatRate = "27"

    val result = AmountCalculator.calculateAmounts(netPrice, quantity, vatRate)

    // Net amount = 100.0 * 2 = 200.0
    // VAT rate = 27% -> 200.0 * 0.27 = 54.0 -> Math.round(54.0) = 54.0
    // Gross amount = 200.0 + 54.0 = 254.0
    assertEquals(200.0, result.netAmount)
    assertEquals(54.0, result.vatAmount)
    assertEquals(254.0, result.grossAmount)
  }

  @Test
  fun `calculateVatAmount rounds to nearest integer accurately`() {
    val netAmount = 100.5 // 100.5 * 0.27 = 27.135 -> 27.0
    val result = AmountCalculator.calculateVatAmount(netAmount, "27")
    assertEquals(27.0, result)

    val netAmount2 = 100.0 // 100.0 * 0.05 = 5.0
    val result2 = AmountCalculator.calculateVatAmount(netAmount2, "5")
    assertEquals(5.0, result2)
  }

  @Test
  fun `calculateGrossAmount includes accurately rounded vat`() {
    val result = AmountCalculator.calculateGrossAmount(200.0, "27")
    assertEquals(254.0, result)
  }

  @Test
  fun `calculateAmounts handles invalid vat gracefully`() {
    val result = AmountCalculator.calculateAmounts(100.0, 1.0, "invalid")
    assertEquals(100.0, result.netAmount)
    assertEquals(0.0, result.vatAmount)
    assertEquals(100.0, result.grossAmount)
  }
}
