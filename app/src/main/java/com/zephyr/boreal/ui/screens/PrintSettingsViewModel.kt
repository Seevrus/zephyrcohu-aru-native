package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.printer.BluetoothDeviceData
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.store.print.PrintSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrintSettingsUiState(
  val selectedPrinterAddress: String? = null,
  val printFullStorageList: Boolean = false,
  val pairedDevices: List<BluetoothDeviceData> = emptyList(),
  val isBluetoothEnabled: Boolean = false,
  val hasPermissions: Boolean = false,
  val canAskPermissions: Boolean = true,
  val isInitialCheck: Boolean = true,
)

@HiltViewModel
class PrintSettingsViewModel
  @Inject
  constructor(
    private val printSettingsStore: PrintSettingsStore,
    private val bluetoothHelper: BluetoothHelper,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(PrintSettingsUiState())
    val uiState: StateFlow<PrintSettingsUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        printSettingsStore.printSettingsState.collect { settings ->
          _uiState.update { current ->
            current.copy(
              selectedPrinterAddress = settings.selectedPrinterMacAddress,
              printFullStorageList = settings.printFullStorageList,
            )
          }
        }
      }
      viewModelScope.launch {
        _uiState
          .map { it.hasPermissions }
          .distinctUntilChanged()
          .combine(bluetoothHelper.observeBluetoothState()) { hasPerms, isEnabled ->
            val pairedDevices =
              if (isEnabled || hasPerms) {
                bluetoothHelper.getPairedDevices()
              } else {
                emptyList()
              }
            isEnabled to pairedDevices
          }.collect { (isEnabled, pairedDevices) ->
            _uiState.update {
              it.copy(
                isBluetoothEnabled = isEnabled,
                pairedDevices = pairedDevices,
              )
            }
          }
      }
    }

    fun updatePermissionsState(
      hasPermissions: Boolean,
      canAskPermissions: Boolean,
    ) {
      _uiState.update {
        it.copy(
          hasPermissions = hasPermissions,
          canAskPermissions = canAskPermissions,
          isInitialCheck = if (hasPermissions) false else it.isInitialCheck,
        )
      }
    }

    fun finishInitialCheck() {
      _uiState.update { it.copy(isInitialCheck = false) }
    }

    fun onPrinterSelected(address: String) {
      _uiState.update { it.copy(selectedPrinterAddress = address) }
    }

    fun onPrintFullStorageListChanged(printFullStorageList: Boolean) {
      _uiState.update { it.copy(printFullStorageList = printFullStorageList) }
    }

    fun saveSettings() {
      viewModelScope.launch {
        printSettingsStore.updateSettings(
          selectedPrinterMacAddress = _uiState.value.selectedPrinterAddress,
          printFullStorageList = _uiState.value.printFullStorageList,
        )
      }
    }
  }
