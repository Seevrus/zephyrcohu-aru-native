package com.zephyr.boreal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.zephyr.boreal.R
import com.zephyr.boreal.printer.BluetoothDeviceData
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.ui.theme.BorealTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PrintSettingsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val printSettingsStore: PrintSettingsStore = mock()
  private val bluetoothHelper: BluetoothHelper = mock()
  private val printSettingsStateFlow = MutableStateFlow(PrintSettingsState())

  @Test
  fun printSettingsScreen_displaysPermissionError_whenNoPermissions() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val errorMessage = context.getString(R.string.print_settings_permission_error)

    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsStateFlow)
    val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
    viewModel.updatePermissionsState(hasPermissions = false, canAskPermissions = false)
    viewModel.finishInitialCheck()

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreen(onNavigateBack = {}, viewModel = viewModel)
      }
    }

    composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
  }

  @Test
  fun printSettingsScreen_displaysBluetoothError_whenBluetoothDisabled() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val errorMessage = context.getString(R.string.print_settings_bluetooth_off_error)

    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsStateFlow)
    whenever(bluetoothHelper.isBluetoothEnabled()).thenReturn(false)

    val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
    viewModel.updatePermissionsState(hasPermissions = true, canAskPermissions = false)
    viewModel.refreshBluetoothState()

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreen(onNavigateBack = {}, viewModel = viewModel)
      }
    }

    composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
  }

  @Test
  fun printSettingsScreen_displaysDropdowns_whenReady() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val printerLabel = context.getString(R.string.print_settings_printer_label)
    val modeLabel = context.getString(R.string.print_settings_mode_label)

    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsStateFlow)
    whenever(bluetoothHelper.isBluetoothEnabled()).thenReturn(true)
    whenever(bluetoothHelper.getPairedDevices()).thenReturn(listOf(BluetoothDeviceData("My Printer", "11:22")))

    val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
    viewModel.updatePermissionsState(hasPermissions = true, canAskPermissions = false)
    viewModel.refreshBluetoothState()

    composeTestRule.setContent {
      BorealTheme {
        PrintSettingsScreen(onNavigateBack = {}, viewModel = viewModel)
      }
    }

    composeTestRule.onNodeWithText(printerLabel).assertIsDisplayed()
    composeTestRule.onNodeWithText(modeLabel).assertIsDisplayed()
    composeTestRule.onNodeWithText("My Printer").assertIsDisplayed()
  }
}
