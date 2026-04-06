package com.zephyr.boreal.store.print

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PrintSettingsStoreTest {
  @TempDir
  lateinit var tempDir: File

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher + Job())

  private lateinit var dataStore: DataStore<Preferences>
  private lateinit var printSettingsStore: PrintSettingsStore

  @BeforeEach
  fun setUp() {
    dataStore =
      PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { tempDir.resolve("test_print_settings.preferences_pb") },
      )
    printSettingsStore = PrintSettingsStore(dataStore, testScope)
  }

  @Test
  fun `initially should have default values`() =
    runTest(testDispatcher) {
      val state = printSettingsStore.printSettingsState.value

      assertNull(state.selectedPrinterMacAddress)
      assertFalse(state.printFullStorageList)
    }

  @Test
  fun `updateSettings should persist values`() =
    runTest(testDispatcher) {
      val expectedAddress = "11:22:33:44:55:66"
      val expectedFullList = true

      printSettingsStore.updateSettings(expectedAddress, expectedFullList)

      val state = printSettingsStore.printSettingsState.value
      assertEquals(expectedAddress, state.selectedPrinterMacAddress)
      assertTrue(state.printFullStorageList)
    }

  @Test
  fun `updateSettings with null address should remove it`() =
    runTest(testDispatcher) {
      printSettingsStore.updateSettings("some_address", true)
      assertEquals("some_address", printSettingsStore.printSettingsState.value.selectedPrinterMacAddress)

      printSettingsStore.updateSettings(null, false)

      val state = printSettingsStore.printSettingsState.value
      assertNull(state.selectedPrinterMacAddress)
      assertFalse(state.printFullStorageList)
    }
}
