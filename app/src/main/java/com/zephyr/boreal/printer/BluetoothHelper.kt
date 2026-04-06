@file:Suppress("SwallowedException", "ReturnCount")

package com.zephyr.boreal.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import com.zephyr.boreal.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

data class BluetoothDeviceData(
  val name: String,
  val address: String,
)

class BluetoothHelper(
  private val context: Context,
) {
  private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

  fun isBluetoothEnabled(): Boolean =
    try {
      bluetoothAdapter?.isEnabled == true
    } catch (e: SecurityException) {
      false
    }

  fun observeBluetoothState(): Flow<Boolean> =
    callbackFlow {
      val receiver =
        object : android.content.BroadcastReceiver() {
          override fun onReceive(
            context: Context?,
            intent: android.content.Intent?,
          ) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
              val state =
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
              if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                trySend(isBluetoothEnabled())
              }
            }
          }
        }

      context.registerReceiver(
        receiver,
        IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
      )

      awaitClose {
        context.unregisterReceiver(receiver)
      }
    }.onStart { emit(isBluetoothEnabled()) }

  fun getPairedDevices(): List<BluetoothDeviceData> {
    try {
      if (!isBluetoothEnabled()) {
        return emptyList()
      }

      val pairedDevices = bluetoothAdapter?.bondedDevices
      val devices = mutableListOf<BluetoothDeviceData>()

      pairedDevices?.forEach { device ->
        val deviceName = device.name ?: context.getString(R.string.bluetooth_unknown_device)
        val deviceHardwareAddress = device.address
        devices.add(BluetoothDeviceData(name = deviceName, address = deviceHardwareAddress))
      }

      if (com.zephyr.boreal.BuildConfig.DEBUG) {
        devices.add(getDevTestDevice())
      }

      devices.sortBy { it.name }

      return devices
    } catch (e: SecurityException) {
      return emptyList()
    }
  }

  private fun getDevTestDevice(): BluetoothDeviceData =
    BluetoothDeviceData(
      name = context.getString(R.string.bluetooth_dev_test_device),
      address = "00:00:00:00:00:00",
    )
}
