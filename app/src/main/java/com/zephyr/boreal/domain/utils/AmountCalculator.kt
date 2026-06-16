package com.zephyr.boreal.domain.utils

import kotlin.math.round

object AmountCalculator {
  private const val PERCENTAGE_DIVISOR = 100.0

  /**
   * Calculates the exact VAT amount based on the net amount and VAT rate.
   * Matches the React Native implementation using Math.round().
   */
  fun calculateVatAmount(
    netAmount: Double,
    vatRate: String,
  ): Double {
    val vatRateNumeric = vatRate.toDoubleOrNull() ?: 0.0
    return round(netAmount * (vatRateNumeric / PERCENTAGE_DIVISOR))
  }

  /**
   * Calculates the gross amount from the net amount and VAT rate.
   */
  fun calculateGrossAmount(
    netAmount: Double,
    vatRate: String,
  ): Double = netAmount + calculateVatAmount(netAmount, vatRate)

  data class AmountCalculationResult(
    val netAmount: Double,
    val vatAmount: Double,
    val grossAmount: Double,
  )

  /**
   * Calculates net, VAT, and gross amounts for a given quantity of an item.
   */
  fun calculateAmounts(
    netPrice: Double,
    quantity: Double,
    vatRate: String,
  ): AmountCalculationResult {
    val netAmount = netPrice * quantity
    val vatAmount = calculateVatAmount(netAmount, vatRate)
    return AmountCalculationResult(
      netAmount = netAmount,
      vatAmount = vatAmount,
      grossAmount = netAmount + vatAmount,
    )
  }
}
