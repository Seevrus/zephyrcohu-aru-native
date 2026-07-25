package com.zephyr.boreal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PrinterConnectorContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private fun setContent(
    uiState: PrinterConnectorUiState,
    onConnectClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      BorealTheme {
        PrinterConnectorContent(
          uiState = uiState,
          onConnectClick = onConnectClick,
          onPrintClick = onPrintClick,
        )
      }
    }
  }

  @Test
  fun showsBluetoothOnWhenEnabled() {
    setContent(uiState = PrinterConnectorUiState(isBluetoothEnabled = true))

    composeTestRule.onNodeWithText("Bluetooth bekapcsolva").assertIsDisplayed()
  }

  @Test
  fun showsBluetoothOffWhenDisabled() {
    setContent(uiState = PrinterConnectorUiState(isBluetoothEnabled = false))

    composeTestRule.onNodeWithText("Bluetooth kikapcsolva").assertIsDisplayed()
  }

  @Test
  fun showsConnectButtonWhenIdleAndClickingInvokesCallback() {
    val onConnectClick: () -> Unit = mock()
    setContent(
      uiState =
        PrinterConnectorUiState(
          isBluetoothEnabled = true,
          selectedPrinterAddress = "00:00:00:00:00:00",
          phase = PrinterConnectorPhase.IDLE,
        ),
      onConnectClick = onConnectClick,
    )

    composeTestRule.onNodeWithText("Kapcsolódás a nyomtatóhoz").performClick()

    verify(onConnectClick).invoke()
  }

  @Test
  fun showsDisabledConnectingButtonWhileConnecting() {
    setContent(
      uiState =
        PrinterConnectorUiState(
          isBluetoothEnabled = true,
          selectedPrinterAddress = "00:00:00:00:00:00",
          phase = PrinterConnectorPhase.CONNECTING,
        ),
    )

    composeTestRule.onNodeWithText("Kapcsolódás…").assertIsNotEnabled()
  }

  @Test
  fun showsPrintButtonWhenConnectedAndClickingInvokesCallback() {
    val onPrintClick: () -> Unit = mock()
    setContent(
      uiState =
        PrinterConnectorUiState(
          isBluetoothEnabled = true,
          selectedPrinterAddress = "00:00:00:00:00:00",
          phase = PrinterConnectorPhase.CONNECTED,
        ),
      onPrintClick = onPrintClick,
    )

    composeTestRule.onNodeWithText("Nyomtatás indítása").performClick()

    verify(onPrintClick).invoke()
  }

  @Test
  fun showsDisabledPrintingButtonWhilePrinting() {
    setContent(
      uiState =
        PrinterConnectorUiState(
          isBluetoothEnabled = true,
          selectedPrinterAddress = "00:00:00:00:00:00",
          phase = PrinterConnectorPhase.PRINTING,
        ),
    )

    composeTestRule.onNodeWithText("Nyomtatás…").assertIsNotEnabled()
  }

  @Test
  fun showsErrorMessageWhenPresent() {
    setContent(
      uiState =
        PrinterConnectorUiState(
          isBluetoothEnabled = true,
          error = PrinterConnectorError.ProbablyOff,
        ),
    )

    composeTestRule
      .onNodeWithText("Nyomtató hiba: A nyomtató valószínűleg nincs bekapcsolva.")
      .assertIsDisplayed()
  }
}
