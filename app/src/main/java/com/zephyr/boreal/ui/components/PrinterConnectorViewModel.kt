package com.zephyr.boreal.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.printer.BorealPrinter
import com.zephyr.boreal.store.print.PrintSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrinterConnectorPhase {
  IDLE,
  CONNECTING,
  CONNECTED,
  PRINTING,
}

sealed class PrinterConnectorError {
  data object ProbablyOff : PrinterConnectorError()

  data class Other(
    val message: String,
  ) : PrinterConnectorError()
}

data class PrinterConnectorUiState(
  val phase: PrinterConnectorPhase = PrinterConnectorPhase.IDLE,
  val isBluetoothEnabled: Boolean = false,
  val selectedPrinterAddress: String? = null,
  val error: PrinterConnectorError? = null,
)

private const val TIMEOUT_MARKER = "timeout"
private const val BROKEN_PIPE_MARKER = "Broken pipe"

/**
 * Reusable connect-then-print state machine, ported from RN's `PrinterConnector.tsx` reducer.
 * Reads the selected printer address from [PrintSettingsStore] and drives [BorealPrinter]
 * directly; assumes Bluetooth permissions are already granted, since a printer must already be
 * selected in Print Settings (which itself gates on permissions) before this is reachable.
 */
@HiltViewModel
class PrinterConnectorViewModel
  @Inject
  constructor(
    private val borealPrinter: BorealPrinter,
    private val bluetoothHelper: BluetoothHelper,
    printSettingsStore: PrintSettingsStore,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(PrinterConnectorUiState())
    val uiState: StateFlow<PrinterConnectorUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        combine(
          bluetoothHelper.observeBluetoothState(),
          printSettingsStore.printSettingsState,
        ) { isBluetoothEnabled, settings ->
          isBluetoothEnabled to settings.selectedPrinterMacAddress
        }.collect { (isBluetoothEnabled, selectedPrinterAddress) ->
          _uiState.update {
            it.copy(isBluetoothEnabled = isBluetoothEnabled, selectedPrinterAddress = selectedPrinterAddress)
          }
        }
      }
    }

    fun connect() {
      val address = _uiState.value.selectedPrinterAddress ?: return
      if (_uiState.value.phase == PrinterConnectorPhase.CONNECTING) {
        return
      }

      _uiState.update { it.copy(phase = PrinterConnectorPhase.CONNECTING, error = null) }
      viewModelScope.launch {
        borealPrinter.connect(address).fold(
          onSuccess = {
            _uiState.update { it.copy(phase = PrinterConnectorPhase.CONNECTED, error = null) }
          },
          onFailure = { exception ->
            _uiState.update {
              it.copy(phase = PrinterConnectorPhase.IDLE, error = classifyConnectError(exception.message))
            }
          },
        )
      }
    }

    fun print(
      textToPrint: String,
      onPrintSuccess: () -> Unit,
    ) {
      if (_uiState.value.phase != PrinterConnectorPhase.CONNECTED) {
        return
      }

      _uiState.update { it.copy(phase = PrinterConnectorPhase.PRINTING, error = null) }
      viewModelScope.launch {
        borealPrinter.printTaggedText(textToPrint).fold(
          onSuccess = {
            _uiState.update { it.copy(phase = PrinterConnectorPhase.CONNECTED, error = null) }
            onPrintSuccess()
          },
          onFailure = { exception ->
            val message = exception.message.orEmpty()
            if (message.contains(BROKEN_PIPE_MARKER, ignoreCase = true)) {
              _uiState.update {
                it.copy(phase = PrinterConnectorPhase.IDLE, error = PrinterConnectorError.ProbablyOff)
              }
            } else {
              _uiState.update {
                it.copy(phase = PrinterConnectorPhase.CONNECTED, error = PrinterConnectorError.Other(message))
              }
            }
          },
        )
      }
    }

    fun disconnect() {
      viewModelScope.launch {
        borealPrinter.disconnect()
      }
    }

    private fun classifyConnectError(message: String?): PrinterConnectorError =
      if (message?.contains(TIMEOUT_MARKER, ignoreCase = true) == true) {
        PrinterConnectorError.ProbablyOff
      } else {
        PrinterConnectorError.Other(message.orEmpty())
      }
  }
