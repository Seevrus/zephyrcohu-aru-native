package com.zephyr.boreal.printer

import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BorealPrinterTest {
  private val context: Context = mock()
  private val bluetoothManager: BluetoothManager = mock()
  private val bluetoothAdapter: android.bluetooth.BluetoothAdapter = mock()

  private lateinit var borealPrinter: BorealPrinter

  @BeforeEach
  fun setUp() {
    whenever(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    whenever(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    whenever(context.getString(any())).thenReturn("Mock String")

    borealPrinter = BorealPrinter(context)
  }

  @Test
  fun `connect to dev test address should return success`() =
    runTest {
      val result = borealPrinter.connect(BorealPrinter.DEV_PRINTER_ADDRESS)

      assertTrue(result.isSuccess)
    }

  @Test
  fun `printTaggedText in dev test mode should return a SaveToFile outcome carrying the text`() =
    runTest {
      // connect to dev test first
      borealPrinter.connect(BorealPrinter.DEV_PRINTER_ADDRESS)

      val result = borealPrinter.printTaggedText("Test Text")

      assertTrue(result.isSuccess)
      assertEquals(PrintOutcome.SaveToFile("Test Text"), result.getOrNull())
    }

  @Test
  fun `disconnect should always return success`() =
    runTest {
      val result = borealPrinter.disconnect()

      assertTrue(result.isSuccess)
    }

  @Test
  fun `printTaggedText on a real address with no live connection returns a not-connected failure, not SaveToFile`() =
    runTest {
      // A real (non-dev) address whose connect attempt fails against the mocked adapter, leaving
      // no live `printer` and `isDevPrinter` false — this must report "not connected", never
      // silently fall back to the dev save-to-file outcome.
      borealPrinter.connect("11:22:33:44:55:66")

      val result = borealPrinter.printTaggedText("Test Text")

      assertTrue(result.isFailure)
    }
}
