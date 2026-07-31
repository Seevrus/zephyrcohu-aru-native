package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ReceiptsListScreenContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun buildRow(
    serialNumber: Int = 42,
    yearCode: Int = 2026,
    deliveryName: String = "Partner Ltd.",
    grossAmount: Double = 1270.0,
    cancelSerialNumber: Int? = null,
    cancelYearCode: Int? = null,
  ) = ReceiptRowUiModel(
    serialNumber = serialNumber,
    yearCode = yearCode,
    deliveryName = deliveryName,
    grossAmount = grossAmount,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
  )

  private fun setContent(
    uiState: ReceiptsListUiState,
    onReceiptClick: (Int) -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        ReceiptsListScreenContent(
          uiState = uiState,
          onReceiptClick = onReceiptClick,
        )
      }
    }
  }

  @Test
  fun showsScreenTitle() {
    setContent(uiState = ReceiptsListUiState())

    composeTestRule.onNodeWithText("Bizonylatok").assertIsDisplayed()
  }

  @Test
  fun emptyListRendersWithoutCrashing() {
    setContent(uiState = ReceiptsListUiState(receipts = emptyList()))

    composeTestRule.onNodeWithText("Bizonylatok").assertIsDisplayed()
  }

  @Test
  fun rowShowsBuyerNumberAndTotal() {
    setContent(uiState = ReceiptsListUiState(receipts = listOf(buildRow())))

    composeTestRule.onNodeWithText("Partner Ltd.").assertIsDisplayed()
    composeTestRule.onNodeWithText("42/2026").assertIsDisplayed()
  }

  @Test
  fun canceledRowShowsCancelNumberAndLabel() {
    setContent(
      uiState =
        ReceiptsListUiState(
          receipts =
            listOf(
              buildRow(cancelSerialNumber = 10, cancelYearCode = 2025),
            ),
        ),
    )

    composeTestRule.onNodeWithText("10/2025").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sztornózva").assertIsDisplayed()
  }

  @Test
  fun activeRowDoesNotShowCancelLabel() {
    setContent(uiState = ReceiptsListUiState(receipts = listOf(buildRow())))

    composeTestRule.onNodeWithText("Sztornózva").assertDoesNotExist()
  }

  @Test
  fun tappingRowInvokesCallbackWithSerialNumber() {
    val onReceiptClick: (Int) -> Unit = mock()
    setContent(
      uiState = ReceiptsListUiState(receipts = listOf(buildRow(serialNumber = 42))),
      onReceiptClick = onReceiptClick,
    )

    composeTestRule.onNodeWithText("Partner Ltd.").performClick()

    verify(onReceiptClick).invoke(42)
  }

  @Test
  fun rowClickNotInvokedWithoutTap() {
    val onReceiptClick: (Int) -> Unit = mock()
    setContent(uiState = ReceiptsListUiState(receipts = listOf(buildRow())), onReceiptClick = onReceiptClick)

    verify(onReceiptClick, never()).invoke(any())
  }
}
