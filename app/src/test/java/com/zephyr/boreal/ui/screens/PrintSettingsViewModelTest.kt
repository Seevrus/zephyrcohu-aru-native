package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.printer.BluetoothDeviceData
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PrintSettingsViewModelTest {
  private val printSettingsStore: PrintSettingsStore = mock()
  private val bluetoothHelper: BluetoothHelper = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val printSettingsStateFlow = MutableStateFlow(PrintSettingsState())
  private val bluetoothStateFlow = MutableStateFlow(false)

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsStateFlow)
    whenever(bluetoothHelper.observeBluetoothState()).thenReturn(bluetoothStateFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initially should reflect settings from store`() =
    runTest {
      val expectedAddress = "00:11:22:33:44:55"
      val expectedFullStorageList = true
      printSettingsStateFlow.value =
        PrintSettingsState(
          selectedPrinterMacAddress = expectedAddress,
          printFullStorageList = expectedFullStorageList,
        )

      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
      runCurrent()

      assertEquals(expectedAddress, viewModel.uiState.value.selectedPrinterAddress)
      assertEquals(expectedFullStorageList, viewModel.uiState.value.printFullStorageList)
    }

  @Test
  fun `updatePermissionsState should update state and refresh bluetooth if granted`() =
    runTest {
      whenever(bluetoothHelper.isBluetoothEnabled()).thenReturn(true)
      bluetoothStateFlow.value = true
      whenever(bluetoothHelper.getPairedDevices()).thenReturn(listOf(BluetoothDeviceData("Test", "Address")))

      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
      runCurrent()

      viewModel.updatePermissionsState(hasPermissions = true, canAskPermissions = false)
      runCurrent()

      assertTrue(viewModel.uiState.value.hasPermissions)
      assertFalse(viewModel.uiState.value.canAskPermissions)
      assertFalse(viewModel.uiState.value.isInitialCheck)
      assertTrue(viewModel.uiState.value.isBluetoothEnabled)
      assertEquals(1, viewModel.uiState.value.pairedDevices.size)
    }

  @Test
  fun `onPrinterSelected should update uiState`() =
    runTest {
      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
      val address = "AA:BB:CC"

      viewModel.onPrinterSelected(address)

      assertEquals(address, viewModel.uiState.value.selectedPrinterAddress)
    }

  @Test
  fun `onPrintFullStorageListChanged should update uiState`() =
    runTest {
      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)

      viewModel.onPrintFullStorageListChanged(true)

      assertTrue(viewModel.uiState.value.printFullStorageList)
    }

  @Test
  fun `saveSettings should call printSettingsStore updateSettings`() =
    runTest {
      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)
      runCurrent() // Process initial collection in init

      val address = "AA:BB:CC"
      val fullList = true

      viewModel.onPrinterSelected(address)
      viewModel.onPrintFullStorageListChanged(fullList)
      viewModel.saveSettings()
      runCurrent()

      verify(printSettingsStore).updateSettings(address, fullList)
    }

  @Test
  fun `finishInitialCheck should update state`() =
    runTest {
      val viewModel = PrintSettingsViewModel(printSettingsStore, bluetoothHelper)

      viewModel.finishInitialCheck()

      assertFalse(viewModel.uiState.value.isInitialCheck)
    }
}
