package com.zephyr.boreal.printer

import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
      val result = borealPrinter.connect("00:00:00:00:00:00")

      assertTrue(result.isSuccess)
    }

  @Test
  fun `printTaggedText in dev test mode should return success`() =
    runTest {
      // connect to dev test first
      borealPrinter.connect("00:00:00:00:00:00")

      val result = borealPrinter.printTaggedText("Test Text")

      assertTrue(result.isSuccess)
    }

  @Test
  fun `disconnect should always return success`() =
    runTest {
      val result = borealPrinter.disconnect()

      assertTrue(result.isSuccess)
    }
}
