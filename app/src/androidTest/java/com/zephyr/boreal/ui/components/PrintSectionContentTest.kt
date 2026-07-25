package com.zephyr.boreal.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.zephyr.boreal.ui.screens.PrintSectionUiState
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class PrintSectionContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun setContent(uiState: PrintSectionUiState) {
    composeTestRule.setContent {
      BorealTheme {
        PrintSectionContent(
          uiState = uiState,
          onPrintSuccess = {},
          // Avoid the real PrinterConnector here: it resolves a Hilt ViewModel, which needs a
          // Hilt-enabled activity this plain compose rule doesn't provide.
          printerConnector = { _, _, _ -> Text("printer connector stub") },
        )
      }
    }
  }

  @Test
  fun showsLoadingIndicatorWhileLoading() {
    setContent(uiState = PrintSectionUiState(isLoading = true))

    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
  }

  @Test
  fun showsMissingDataMessageWhenReceiptToPrintIsNull() {
    setContent(uiState = PrintSectionUiState(isLoading = false, receiptToPrint = null))

    composeTestRule.onNodeWithText("Nem áll rendelkezésre minden adat a nyomtatáshoz.").assertIsDisplayed()
  }

  @Test
  fun showsFormatLabelAndCopyCountDescriptionWhenPrintable() {
    setContent(
      uiState =
        PrintSectionUiState(
          isLoading = false,
          receiptFormat = "papír alapú",
          copyCount = 3,
          receiptToPrint = "receipt body",
        ),
    )

    composeTestRule.onNodeWithText("Az eredeti számla formátuma: papír alapú.").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("3 példányban történik", substring = true)
      .assertIsDisplayed()
  }
}
