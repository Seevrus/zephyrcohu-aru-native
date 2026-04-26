package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.printer.BluetoothDeviceData
import com.zephyr.boreal.ui.theme.BorealTheme
import org.junit.Rule
import org.junit.Test

class PrintSettingsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun printSettingsScreen_displaysPermissionError_whenNoPermissions() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val errorMessage = context.getString(R.string.print_settings_permission_error)

    val uiState =
      PrintSettingsUiState(
        hasPermissions = false,
        isInitialCheck = false,
      )

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreenContent(
          uiState = uiState,
          onOpenSettings = {},
          onEnableBluetooth = {},
          onPrinterSelected = {},
          onPrintFullStorageListChanged = {},
          onSave = {},
        )
      }
    }

    composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
  }

  @Test
  fun printSettingsScreen_displaysBluetoothError_whenBluetoothDisabled() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val errorMessage = context.getString(R.string.print_settings_bluetooth_off_error)

    val uiState =
      PrintSettingsUiState(
        hasPermissions = true,
        isBluetoothEnabled = false,
        isInitialCheck = false,
      )

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreenContent(
          uiState = uiState,
          onOpenSettings = {},
          onEnableBluetooth = {},
          onPrinterSelected = {},
          onPrintFullStorageListChanged = {},
          onSave = {},
        )
      }
    }

    composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
  }

  @Test
  fun printSettingsScreen_displaysDropdowns_whenReady() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val printerLabel = context.getString(R.string.print_settings_printer_label)
    val modeLabel = context.getString(R.string.print_settings_mode_label)

    val uiState =
      PrintSettingsUiState(
        hasPermissions = true,
        isBluetoothEnabled = true,
        isInitialCheck = false,
        pairedDevices = listOf(BluetoothDeviceData("My Printer", "11:22")),
        selectedPrinterAddress = "11:22",
      )

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreenContent(
          uiState = uiState,
          onOpenSettings = {},
          onEnableBluetooth = {},
          onPrinterSelected = {},
          onPrintFullStorageListChanged = {},
          onSave = {},
        )
      }
    }

    composeTestRule.onNodeWithText(printerLabel).assertIsDisplayed()
    composeTestRule.onNodeWithText(modeLabel).assertIsDisplayed()
    composeTestRule.onNodeWithText("My Printer").assertIsDisplayed()
  }
}
