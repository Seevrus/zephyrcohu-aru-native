package com.zephyr.boreal.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SelectItemsFromStoreScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun storageListItem(
    itemId: Int,
    expirationId: Int,
    name: String,
    quantityChange: Int? = null,
  ): StorageListItem =
    StorageListItem(
      itemId = itemId,
      expirationId = expirationId,
      articleNumber = "ART$itemId",
      name = name,
      expiresAt = "260101",
      itemBarcode = "ITEMBC$itemId",
      expirationBarcode = "EXPBC$expirationId",
      unitName = "db",
      primaryStoreQuantity = 10.0,
      originalQuantity = 5.0,
      quantityChange = quantityChange,
    )

  @Test
  fun selectItemsFromStoreScreen_forwardIconAbsentUntilAnyItemChanged() {
    val items = listOf(storageListItem(1, 11, "Alpha"), storageListItem(2, 12, "Beta"))

    composeTestRule.setContent {
      BorealTheme {
        SelectItemsFromStoreScreenContent(
          uiState = SelectItemsFromStoreUiState(items = items, isLoading = false),
          onSearchQueryChanged = {},
          onBarcodeIconClick = {},
          onToggleExpanded = {},
          onQuantityChanged = { _, _, _ -> },
          onForward = {},
          onAbandonClick = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Tovább").assertDoesNotExist()
  }

  @Test
  fun selectItemsFromStoreScreen_forwardIconShownWhenAnyItemChanged() {
    val onForward: () -> Unit = mock()
    val items = listOf(storageListItem(1, 11, "Alpha", quantityChange = 3))

    composeTestRule.setContent {
      BorealTheme {
        SelectItemsFromStoreScreenContent(
          uiState = SelectItemsFromStoreUiState(items = items, isLoading = false),
          onSearchQueryChanged = {},
          onBarcodeIconClick = {},
          onToggleExpanded = {},
          onQuantityChanged = { _, _, _ -> },
          onForward = onForward,
          onAbandonClick = {},
        )
      }
    }

    composeTestRule.onNodeWithContentDescription("Tovább").performClick()
    verify(onForward).invoke()
  }

  @Test
  fun selectItemsFromStoreScreen_searchNarrowsVisibleRows() {
    val items = listOf(storageListItem(1, 11, "Alpha"), storageListItem(2, 12, "Beta"))

    composeTestRule.setContent {
      BorealTheme {
        var searchQuery by remember { mutableStateOf("") }
        SelectItemsFromStoreScreenContent(
          uiState = SelectItemsFromStoreUiState(items = items, isLoading = false, searchQuery = searchQuery),
          onSearchQueryChanged = { searchQuery = it },
          onBarcodeIconClick = {},
          onToggleExpanded = {},
          onQuantityChanged = { _, _, _ -> },
          onForward = {},
          onAbandonClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
    composeTestRule.onNodeWithText("Beta").assertIsDisplayed()

    composeTestRule.onNodeWithText("Keresés…").performTextInput("Alpha")

    composeTestRule.onNodeWithText("Beta").assertDoesNotExist()
  }

  @Test
  fun selectItemsFromStoreScreen_collapsedRowHidesPrimaryStockAndClickTogglesExpansion() {
    val onToggleExpanded: (Int) -> Unit = mock()
    val items = listOf(storageListItem(1, 11, "Alpha"))

    composeTestRule.setContent {
      BorealTheme {
        SelectItemsFromStoreScreenContent(
          uiState = SelectItemsFromStoreUiState(items = items, isLoading = false),
          onSearchQueryChanged = {},
          onBarcodeIconClick = {},
          onToggleExpanded = onToggleExpanded,
          onQuantityChanged = { _, _, _ -> },
          onForward = {},
          onAbandonClick = {},
        )
      }
    }

    composeTestRule.onNodeWithText("Rakodás:").assertDoesNotExist()

    composeTestRule.onNodeWithText("Alpha").performClick()
    verify(onToggleExpanded).invoke(11)
  }

  @Test
  fun selectItemsFromStoreScreen_abandonWithoutChangesNavigatesWithoutDialog() {
    val onAbandonClick: () -> Unit = mock()

    composeTestRule.setContent {
      BorealTheme {
        SelectItemsFromStoreScreenContent(
          uiState = SelectItemsFromStoreUiState(items = emptyList(), isLoading = false),
          onSearchQueryChanged = {},
          onBarcodeIconClick = {},
          onToggleExpanded = {},
          onQuantityChanged = { _, _, _ -> },
          onForward = {},
          onAbandonClick = onAbandonClick,
        )
      }
    }

    composeTestRule.onNodeWithText("Rakodás elvetése").performClick()
    verify(onAbandonClick).invoke()
  }
}
