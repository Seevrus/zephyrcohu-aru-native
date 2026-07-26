package com.zephyr.boreal.domain.utils

import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.SelectedDiscount
import kotlin.math.round

object DiscountCalculator {
  data class DiscountedLineAmounts(
    val netAmount: Double,
    val vatAmount: Double,
    val grossAmount: Double,
  )

  /**
   * One charged portion of a discounted line: either a per-discount slice or the undiscounted
   * remainder (`discountName == null`).
   */
  data class DiscountedLinePortion(
    val quantity: Double,
    val netPrice: Double,
    val netAmount: Double,
    val vatAmount: Double,
    val grossAmount: Double,
    val discountName: String?,
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
        round(
          originalNetPrice *
            ((AmountCalculator.PERCENTAGE_DIVISOR - (discount.amount ?: 0.0)) / AmountCalculator.PERCENTAGE_DIVISOR),
        )
      DiscountType.FREE_FORM -> discount.price ?: originalNetPrice
    }

  /**
   * Splits a line's selected discounts into individually charged portions plus any undiscounted
   * remainder of the line's quantity (`discountName == null`), matching
   * createUniqueDiscountedItems.ts. This is the single producer of the decomposition: both the
   * displayed total ([calculateDiscountedLineAmounts]) and the submitted API rows
   * (`DraftReceiptItem.toDtos`) are derived from it, so they cannot disagree.
   *
   * Only called for a non-empty [selectedDiscounts]; callers keep their own empty-list
   * short-circuit.
   */
  fun splitDiscountedLineIntoPortions(
    originalNetPrice: Double,
    quantity: Double,
    vatRate: String,
    selectedDiscounts: List<SelectedDiscount>,
  ): List<DiscountedLinePortion> {
    val discountedPortions =
      selectedDiscounts.map { discount ->
        val unitNetPrice = calculateDiscountedUnitNetPrice(originalNetPrice, discount)
        val amounts = AmountCalculator.calculateAmounts(unitNetPrice, discount.quantity, vatRate)
        DiscountedLinePortion(
          quantity = discount.quantity,
          netPrice = unitNetPrice,
          netAmount = amounts.netAmount,
          vatAmount = amounts.vatAmount,
          grossAmount = amounts.grossAmount,
          discountName = discount.name,
        )
      }

    val remainingQuantity = quantity - selectedDiscounts.sumOf { it.quantity }
    return if (remainingQuantity > 0) {
      val amounts = AmountCalculator.calculateAmounts(originalNetPrice, remainingQuantity, vatRate)
      discountedPortions +
        DiscountedLinePortion(
          quantity = remainingQuantity,
          netPrice = originalNetPrice,
          netAmount = amounts.netAmount,
          vatAmount = amounts.vatAmount,
          grossAmount = amounts.grossAmount,
          discountName = null,
        )
    } else {
      discountedPortions
    }
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

    val portions = splitDiscountedLineIntoPortions(originalNetPrice, quantity, vatRate, selectedDiscounts)
    return DiscountedLineAmounts(
      netAmount = portions.sumOf { it.netAmount },
      vatAmount = portions.sumOf { it.vatAmount },
      grossAmount = portions.sumOf { it.grossAmount },
    )
  }
}
