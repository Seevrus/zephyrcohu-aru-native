package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.SelectedDiscount
import kotlin.math.round

object DiscountCalculator {
  private const val PERCENTAGE_DIVISOR = 100.0

  data class DiscountedLineAmounts(
    val netAmount: Double,
    val vatAmount: Double,
    val grossAmount: Double,
  )

  /**
   * Calculates the discounted unit net price for a single selected discount.
   * Matches the React Native implementation in calculateDiscountedItemAmounts.ts.
   */
  fun calculateDiscountedUnitNetPrice(
    originalNetPrice: Double,
    discount: SelectedDiscount,
  ): Double =
    when (discount.type) {
      DiscountType.ABSOLUTE -> originalNetPrice - (discount.amount ?: 0.0)
      DiscountType.PERCENTAGE ->
        round(originalNetPrice * ((PERCENTAGE_DIVISOR - (discount.amount ?: 0.0)) / PERCENTAGE_DIVISOR))
      DiscountType.FREE_FORM -> discount.price ?: originalNetPrice
    }

  /**
   * Calculates a line's net/VAT/gross amounts across all selected discounts plus any
   * undiscounted remainder of the line's quantity, matching calculateDiscountedItemAmounts.ts.
   */
  fun calculateDiscountedLineAmounts(
    originalNetPrice: Double,
    quantity: Double,
    vatRate: String,
    selectedDiscounts: List<SelectedDiscount>,
  ): DiscountedLineAmounts {
    if (selectedDiscounts.isEmpty()) {
      val amounts = AmountCalculator.calculateAmounts(originalNetPrice, quantity, vatRate)
      return DiscountedLineAmounts(amounts.netAmount, amounts.vatAmount, amounts.grossAmount)
    }

    var netAmount = 0.0
    var vatAmount = 0.0
    var grossAmount = 0.0
    var discountedQuantity = 0.0

    selectedDiscounts.forEach { discount ->
      val unitNetPrice = calculateDiscountedUnitNetPrice(originalNetPrice, discount)
      val amounts = AmountCalculator.calculateAmounts(unitNetPrice, discount.quantity, vatRate)
      netAmount += amounts.netAmount
      vatAmount += amounts.vatAmount
      grossAmount += amounts.grossAmount
      discountedQuantity += discount.quantity
    }

    val remainingQuantity = quantity - discountedQuantity
    if (remainingQuantity > 0) {
      val amounts = AmountCalculator.calculateAmounts(originalNetPrice, remainingQuantity, vatRate)
      netAmount += amounts.netAmount
      vatAmount += amounts.vatAmount
      grossAmount += amounts.grossAmount
    }

    return DiscountedLineAmounts(netAmount, vatAmount, grossAmount)
  }
}
