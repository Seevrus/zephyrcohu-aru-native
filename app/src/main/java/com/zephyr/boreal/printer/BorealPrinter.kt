package com.zephyr.boreal.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.datecs.printer.Printer
import com.datecs.printer.ProtocolAdapter
import com.zephyr.boreal.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BorealPrinter
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
  ) {
    companion object {
      private const val TAG = "BorealPrinter"
      private const val PAPER_FEED_AMOUNT = 110
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var printer: Printer? = null
    private var printerSocket: BluetoothSocket? = null
    private var printerSocketInputStream: InputStream? = null
    private var printerSocketOutputStream: OutputStream? = null
    private var protocolAdapter: ProtocolAdapter? = null

    private val printerListener = BorealPrinterListener()

    suspend fun connect(address: String): Result<Unit> =
      withContext(Dispatchers.IO) {
        try {
          disconnect()

          if (bluetoothAdapter == null) {
            return@withContext Result.failure(
              Exception(context.getString(R.string.printer_error_bluetooth_not_supported)),
            )
          }

          if (address == "00:00:00:00:00:00") {
            // Dev Test mock
            return@withContext Result.success(Unit)
          }

          val printerDevice = bluetoothAdapter.getRemoteDevice(address)
          val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

          printerSocket = printerDevice.createRfcommSocketToServiceRecord(uuid)
          printerSocket!!.connect()

          printerSocketInputStream = printerSocket!!.inputStream
          printerSocketOutputStream = printerSocket!!.outputStream

          initializePrinter(printerSocketInputStream!!, printerSocketOutputStream!!)
          printer?.beep()
          printer?.flush()

          Result.success(Unit)
        } catch (e: Exception) {
          Log.e(TAG, context.getString(R.string.printer_log_connection_error, e.message), e)
          Result.failure(e)
        }
      }

    suspend fun disconnect(): Result<Unit> =
      withContext(Dispatchers.IO) {
        try {
          printerSocket?.close()
          printer?.close()
          protocolAdapter?.close()

          printerSocket = null
          printer = null
          protocolAdapter = null
          printerSocketInputStream = null
          printerSocketOutputStream = null

          Result.success(Unit)
        } catch (e: Exception) {
          Log.e(TAG, context.getString(R.string.printer_log_disconnection_error, e.message), e)
          Result.failure(e)
        }
      }

    suspend fun printTaggedText(taggedText: String): Result<Unit> =
      withContext(Dispatchers.IO) {
        try {
          if (printer == null) {
            // Check if mock dev test
            if (printerSocket == null) {
              Log.d(TAG, context.getString(R.string.printer_log_mock_print, taggedText))
              return@withContext Result.success(Unit)
            }
            return@withContext Result.failure(
              Exception(context.getString(R.string.printer_error_not_connected)),
            )
          }

          val encoding = Charset.forName("IBM852")
          val encodedBytes = taggedText.toByteArray(encoding)
          val correctValue = String(encodedBytes, encoding)

          printer?.printTaggedText(correctValue, "IBM852")
          printer?.feedPaper(PAPER_FEED_AMOUNT)
          printer?.flush()

          Result.success(Unit)
        } catch (e: Exception) {
          Log.e(TAG, context.getString(R.string.printer_log_printing_error, e.message), e)
          Result.failure(e)
        }
      }

    private fun initializePrinter(
      inputStream: InputStream,
      outputStream: OutputStream,
    ) {
      protocolAdapter = ProtocolAdapter(inputStream, outputStream)

      if (protocolAdapter!!.isProtocolEnabled) {
        protocolAdapter!!.setPrinterListener(printerListener)

        val channel = protocolAdapter!!.getChannel(ProtocolAdapter.CHANNEL_PRINTER)
        printer =
          Printer(
            channel.inputStream,
            channel.outputStream,
          )
      } else {
        printer =
          Printer(
            protocolAdapter!!.rawInputStream,
            protocolAdapter!!.rawOutputStream,
          )
      }
    }

    inner class BorealPrinterListener : ProtocolAdapter.PrinterListener {
      override fun onBatteryStateChanged(lowBattery: Boolean) {
        Log.d(TAG, context.getString(R.string.printer_log_battery_low, lowBattery))
      }

      override fun onPaperStateChanged(noPaper: Boolean) {
        Log.d(TAG, context.getString(R.string.printer_log_paper_out, noPaper))
      }

      override fun onThermalHeadStateChanged(overheated: Boolean) {
        Log.d(TAG, context.getString(R.string.printer_log_overheated, overheated))
      }
    }
  }
