package com.zephyr.boreal.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.zephyr.boreal.R
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BluetoothHelperTest {
  private val context: Context = mock()
  private val bluetoothManager: BluetoothManager = mock()
  private val bluetoothAdapter: BluetoothAdapter = mock()

  private lateinit var bluetoothHelper: BluetoothHelper

  @BeforeEach
  fun setUp() {
    whenever(context.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(bluetoothManager)
    whenever(bluetoothManager.adapter).thenReturn(bluetoothAdapter)
    whenever(context.getString(R.string.bluetooth_unknown_device)).thenReturn("Unknown Device")
    whenever(context.getString(R.string.bluetooth_dev_test_device)).thenReturn("Dev Test")

    bluetoothHelper = BluetoothHelper(context)
  }

  @Test
  fun `isBluetoothEnabled should return true when adapter is enabled`() {
    whenever(bluetoothAdapter.isEnabled).thenReturn(true)

    assertTrue(bluetoothHelper.isBluetoothEnabled())
  }

  @Test
  fun `isBluetoothEnabled should return false when adapter is disabled`() {
    whenever(bluetoothAdapter.isEnabled).thenReturn(false)

    assertFalse(bluetoothHelper.isBluetoothEnabled())
  }

  @Test
  fun `isBluetoothEnabled should return false when security exception occurs`() {
    whenever(bluetoothAdapter.isEnabled).thenThrow(SecurityException())

    assertFalse(bluetoothHelper.isBluetoothEnabled())
  }

  @Test
  fun `getPairedDevices should return empty list when bluetooth is disabled`() {
    whenever(bluetoothAdapter.isEnabled).thenReturn(false)

    val devices = bluetoothHelper.getPairedDevices()

    assertTrue(devices.isEmpty())
  }

  @Test
  fun `getPairedDevices should return list of devices when bluetooth is enabled`() {
    whenever(bluetoothAdapter.isEnabled).thenReturn(true)

    val device1: BluetoothDevice = mock()
    whenever(device1.name).thenReturn("Device 1")
    whenever(device1.address).thenReturn("11:22")

    val device2: BluetoothDevice = mock()
    whenever(device2.name).thenReturn("Device 2")
    whenever(device2.address).thenReturn("33:44")

    whenever(bluetoothAdapter.bondedDevices).thenReturn(setOf(device1, device2))

    val devices = bluetoothHelper.getPairedDevices()

    assertTrue(devices.any { it.name == "Device 1" && it.address == "11:22" })
    assertTrue(devices.any { it.name == "Device 2" && it.address == "33:44" })
  }

  @Test
  fun `getPairedDevices should return empty list when security exception occurs`() {
    whenever(bluetoothAdapter.isEnabled).thenReturn(true)
    whenever(bluetoothAdapter.bondedDevices).thenThrow(SecurityException())

    val devices = bluetoothHelper.getPairedDevices()

    assertTrue(devices.isEmpty())
  }
}
