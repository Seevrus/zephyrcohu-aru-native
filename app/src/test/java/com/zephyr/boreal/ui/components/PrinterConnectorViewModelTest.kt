package com.zephyr.boreal.ui.components

import android.net.Uri
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.printer.BorealPrinter
import com.zephyr.boreal.printer.PrintOutcome
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PrinterConnectorViewModelTest {
  private val borealPrinter: BorealPrinter = mock()
  private val bluetoothHelper: BluetoothHelper = mock()
  private val printSettingsStore: PrintSettingsStore = mock()

  private val printSettingsFlow =
    MutableStateFlow(PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55"))

  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(bluetoothHelper.observeBluetoothState()).thenReturn(flowOf(true))
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = PrinterConnectorViewModel(borealPrinter, bluetoothHelper, printSettingsStore)

  @Test
  fun `uiState reflects bluetooth state and the selected printer address`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.isBluetoothEnabled)
      assertEquals("00:11:22:33:44:55", viewModel.uiState.value.selectedPrinterAddress)
    }

  @Test
  fun `connect success moves to CONNECTED with no error`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:11:22:33:44:55")).thenReturn(Result.success(Unit))
      val viewModel = createViewModel()
      advanceUntilIdle()

      viewModel.connect()
      advanceUntilIdle()

      assertEquals(PrinterConnectorPhase.CONNECTED, viewModel.uiState.value.phase)
      assertNull(viewModel.uiState.value.error)
    }

  @Test
  fun `connect failure with a timeout message maps to ProbablyOff and drops back to IDLE`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:11:22:33:44:55"))
        .thenReturn(Result.failure(Exception("Connection timeout")))
      val viewModel = createViewModel()
      advanceUntilIdle()

      viewModel.connect()
      advanceUntilIdle()

      assertEquals(PrinterConnectorPhase.IDLE, viewModel.uiState.value.phase)
      assertEquals(PrinterConnectorError.ProbablyOff, viewModel.uiState.value.error)
    }

  @Test
  fun `connect failure with a generic message is surfaced as-is`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:11:22:33:44:55"))
        .thenReturn(Result.failure(Exception("Nyomtató nincs csatlakoztatva.")))
      val viewModel = createViewModel()
      advanceUntilIdle()

      viewModel.connect()
      advanceUntilIdle()

      assertEquals(
        PrinterConnectorError.Other("Nyomtató nincs csatlakoztatva."),
        viewModel.uiState.value.error,
      )
    }

  @Test
  fun `print while not connected is a no-op`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      advanceUntilIdle()

      viewModel.print("receipt text", "file.txt") {}
      advanceUntilIdle()

      verify(borealPrinter, org.mockito.kotlin.never()).printTaggedText(org.mockito.kotlin.any())
    }

  @Test
  fun `print success stays CONNECTED and invokes onPrintSuccess`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:11:22:33:44:55")).thenReturn(Result.success(Unit))
      whenever(borealPrinter.printTaggedText("receipt text")).thenReturn(Result.success(PrintOutcome.Printed))
      val viewModel = createViewModel()
      advanceUntilIdle()
      viewModel.connect()
      advanceUntilIdle()

      var successCalled = false
      viewModel.print("receipt text", "file.txt") { successCalled = true }
      advanceUntilIdle()

      assertTrue(successCalled)
      assertEquals(PrinterConnectorPhase.CONNECTED, viewModel.uiState.value.phase)
    }

  @Test
  fun `print failure with Broken pipe drops back to IDLE with ProbablyOff`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:11:22:33:44:55")).thenReturn(Result.success(Unit))
      whenever(borealPrinter.printTaggedText("receipt text"))
        .thenReturn(Result.failure(Exception("Broken pipe")))
      val viewModel = createViewModel()
      advanceUntilIdle()
      viewModel.connect()
      advanceUntilIdle()

      viewModel.print("receipt text", "file.txt") {}
      advanceUntilIdle()

      assertEquals(PrinterConnectorPhase.IDLE, viewModel.uiState.value.phase)
      assertEquals(PrinterConnectorError.ProbablyOff, viewModel.uiState.value.error)
    }

  @Test
  fun `print of a SaveToFile outcome surfaces a saveFileRequest without calling onPrintSuccess yet`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:00:00:00:00:00")).thenReturn(Result.success(Unit))
      whenever(borealPrinter.printTaggedText("receipt text"))
        .thenReturn(Result.success(PrintOutcome.SaveToFile("receipt text")))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:00:00:00:00:00")
      val viewModel = createViewModel()
      advanceUntilIdle()
      viewModel.connect()
      advanceUntilIdle()

      var successCalled = false
      viewModel.print("receipt text", "partner_2026-01-01_1.txt") { successCalled = true }
      advanceUntilIdle()

      assertTrue(!successCalled)
      assertEquals(
        SaveFileRequest("partner_2026-01-01_1.txt", "receipt text"),
        viewModel.uiState.value.saveFileRequest,
      )
    }

  @Test
  fun `onSaveFileResult with a uri writes content, invokes onPrintSuccess, and returns to CONNECTED`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:00:00:00:00:00")).thenReturn(Result.success(Unit))
      whenever(borealPrinter.printTaggedText("receipt text"))
        .thenReturn(Result.success(PrintOutcome.SaveToFile("receipt text")))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:00:00:00:00:00")
      val viewModel = createViewModel()
      advanceUntilIdle()
      viewModel.connect()
      advanceUntilIdle()

      val uri: Uri = mock()
      whenever(borealPrinter.writeToUri(uri, "receipt text")).thenReturn(Result.success(Unit))

      var successCalled = false
      viewModel.print("receipt text", "file.txt") { successCalled = true }
      advanceUntilIdle()

      viewModel.onSaveFileResult(uri)
      advanceUntilIdle()

      assertTrue(successCalled)
      verify(borealPrinter).writeToUri(uri, "receipt text")
      assertEquals(PrinterConnectorPhase.CONNECTED, viewModel.uiState.value.phase)
      assertNull(viewModel.uiState.value.saveFileRequest)
    }

  @Test
  fun `onSaveFileResult with a null uri returns to CONNECTED without error or onPrintSuccess`() =
    runTest(testDispatcher) {
      whenever(borealPrinter.connect("00:00:00:00:00:00")).thenReturn(Result.success(Unit))
      whenever(borealPrinter.printTaggedText("receipt text"))
        .thenReturn(Result.success(PrintOutcome.SaveToFile("receipt text")))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:00:00:00:00:00")
      val viewModel = createViewModel()
      advanceUntilIdle()
      viewModel.connect()
      advanceUntilIdle()

      var successCalled = false
      viewModel.print("receipt text", "file.txt") { successCalled = true }
      advanceUntilIdle()

      viewModel.onSaveFileResult(null)
      advanceUntilIdle()

      assertTrue(!successCalled)
      assertNull(viewModel.uiState.value.error)
      assertEquals(PrinterConnectorPhase.CONNECTED, viewModel.uiState.value.phase)
    }

  @Test
  fun `disconnect calls BorealPrinter disconnect`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      advanceUntilIdle()

      viewModel.disconnect()
      advanceUntilIdle()

      verify(borealPrinter).disconnect()
    }
}
