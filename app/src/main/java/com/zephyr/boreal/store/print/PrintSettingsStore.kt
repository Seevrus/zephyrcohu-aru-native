package com.zephyr.boreal.store.print

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

data class PrintSettingsState(
  val selectedPrinterMacAddress: String? = null,
  val printFullStorageList: Boolean = false,
)

@Singleton
open class PrintSettingsStore
  @Inject
  constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) {
    companion object {
      val SELECTED_PRINTER_MAC = stringPreferencesKey("selected_printer_mac")
      val PRINT_FULL_STORAGE_LIST = booleanPreferencesKey("print_full_storage_list")
    }

    open val printSettingsState: StateFlow<PrintSettingsState> =
      dataStore.data
        .map { preferences ->
          val selectedPrinterMacAddress = preferences[SELECTED_PRINTER_MAC]
          val printFullStorageList = preferences[PRINT_FULL_STORAGE_LIST] ?: false

          PrintSettingsState(
            selectedPrinterMacAddress = selectedPrinterMacAddress,
            printFullStorageList = printFullStorageList,
          )
        }.stateIn(
          scope = scope,
          started = SharingStarted.Eagerly,
          initialValue = PrintSettingsState(),
        )

    open suspend fun updateSettings(
      selectedPrinterMacAddress: String?,
      printFullStorageList: Boolean,
    ) {
      dataStore.edit { preferences ->
        if (selectedPrinterMacAddress != null) {
          preferences[SELECTED_PRINTER_MAC] = selectedPrinterMacAddress
        } else {
          preferences.remove(SELECTED_PRINTER_MAC)
        }
        preferences[PRINT_FULL_STORAGE_LIST] = printFullStorageList
      }
    }
  }
