package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.ui.theme.BorealColors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectItemsHeaderColorTest {
  @Test
  fun `item in receipt is Ok regardless of stock`() {
    assertEquals(BorealColors.Ok, selectItemsHeaderColor(inReceipt = true, hasStock = true))
    assertEquals(BorealColors.Ok, selectItemsHeaderColor(inReceipt = true, hasStock = false))
  }

  @Test
  fun `item not in receipt with stock is Neutral`() {
    assertEquals(BorealColors.Neutral, selectItemsHeaderColor(inReceipt = false, hasStock = true))
  }

  @Test
  fun `item not in receipt without stock is Warning`() {
    assertEquals(BorealColors.Warning, selectItemsHeaderColor(inReceipt = false, hasStock = false))
  }
}
