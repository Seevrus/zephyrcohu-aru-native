package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ReviewItemsScreenContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun buildItem(
    id: Int = 1,
    expirationId: Int = 1,
    grossAmount: Double = 1270.0,
  ) = ReceiptItem(
    id = id,
    articleNumber = "ART-00$id",
    name = "Teszt termék $id",
    quantity = 2.0,
    unitName = "db",
    netPrice = 500.0,
    netAmount = 1000.0,
    vatRate = "27%",
    vatAmount = 270.0,
    grossAmount = grossAmount,
    discountName = null,
    expirationId = expirationId,
    cnCode = "CN001",
    expiresAt = "2026-12-31",
  )

  private fun setContent(
    uiState: ReviewItemsUiState,
    onToggleExpanded: (String) -> Unit = {},
    onRemoveItem: (Int, Int) -> Unit = { _, _ -> },
    onCancelClick: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        ReviewItemsScreenContent(
          uiState = uiState,
          onToggleExpanded = onToggleExpanded,
          onRemoveItem = onRemoveItem,
          onCancelClick = onCancelClick,
        )
      }
    }
  }

  @Test
  fun showsScreenTitle() {
    setContent(uiState = ReviewItemsUiState())

    composeTestRule.onNodeWithText("Áttekintés").assertIsDisplayed()
  }

  @Test
  fun showsItemNameAndExpirationInAccordionHeader() {
    val item = buildItem()
    setContent(uiState = ReviewItemsUiState(items = listOf(item)))

    composeTestRule.onNodeWithText("Teszt termék 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2026-12-31").assertIsDisplayed()
  }

  @Test
  fun articleNumberAndDeleteButtonHiddenWhenCollapsed() {
    val item = buildItem()
    setContent(
      uiState =
        ReviewItemsUiState(
          items = listOf(item),
          expandedItemKeys = emptySet(),
        ),
    )

    composeTestRule.onNodeWithText("Cikkszám:").assertDoesNotExist()
    composeTestRule.onNodeWithText("Törlés").assertDoesNotExist()
  }

  @Test
  fun articleNumberAndDeleteButtonVisibleWhenExpanded() {
    val item = buildItem()
    setContent(
      uiState =
        ReviewItemsUiState(
          items = listOf(item),
          expandedItemKeys = setOf("1_1"),
        ),
    )

    composeTestRule.onNodeWithText("Cikkszám:").assertIsDisplayed()
    composeTestRule.onNodeWithText("ART-001").assertIsDisplayed()
    composeTestRule.onNodeWithText("Törlés").assertIsDisplayed()
  }

  @Test
  fun showsTotalInHungarianCurrencyFormat() {
    val item = buildItem(grossAmount = 1270.0)
    setContent(uiState = ReviewItemsUiState(items = listOf(item), grossTotal = 1270.0))

    composeTestRule.onNodeWithText("Mindösszesen:", substring = true).assertIsDisplayed()
  }

  @Test
  fun cancelButtonTriggersOnCancelClick() {
    val onCancelClick: () -> Unit = mock()
    setContent(uiState = ReviewItemsUiState(), onCancelClick = onCancelClick)

    composeTestRule.onNodeWithText("Elvetés").performClick()

    verify(onCancelClick).invoke()
  }

  @Test
  fun cancelDialogIsShownWhenShowCancelConfirmationIsTrue() {
    val message =
      "Ez a lépés törli a jelenlegi árulevételi munkamenetet és visszairányít " +
        "a kezdőoldalra. Biztosan folytatni szeretné?"
    composeTestRule.setContent {
      BorealTheme {
        BorealAlert(
          title = "Folyamat törlése",
          message = message,
          confirmButtonText = "Folytatás",
          confirmButtonVariant = ButtonVariant.WARNING,
          cancelButtonText = "Mégsem",
          onConfirmClick = {},
          onCancelClick = {},
          onDismissRequest = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Folyamat törlése").assertIsDisplayed()
    composeTestRule.onNodeWithText("Biztosan folytatni szeretné?", substring = true).assertIsDisplayed()
  }

  @Test
  fun confirmCancelDialogCallsOnConfirmClick() {
    val onConfirmClick: () -> Unit = mock()
    composeTestRule.setContent {
      BorealTheme {
        BorealAlert(
          title = "Folyamat törlése",
          message = null,
          confirmButtonText = "Folytatás",
          confirmButtonVariant = ButtonVariant.WARNING,
          cancelButtonText = "Mégsem",
          onConfirmClick = onConfirmClick,
          onCancelClick = {},
          onDismissRequest = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Folytatás").performClick()

    verify(onConfirmClick).invoke()
  }

  @Test
  fun dismissCancelDialogCallsOnCancelClick() {
    val onCancelClick: () -> Unit = mock()
    composeTestRule.setContent {
      BorealTheme {
        BorealAlert(
          title = "Folyamat törlése",
          message = null,
          confirmButtonText = "Folytatás",
          confirmButtonVariant = ButtonVariant.WARNING,
          cancelButtonText = "Mégsem",
          onConfirmClick = {},
          onCancelClick = onCancelClick,
          onDismissRequest = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Mégsem").performClick()

    verify(onCancelClick).invoke()
  }
}
