package com.zephyr.boreal.store.print

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
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

  private lateinit var testDispatcher: TestDispatcher
  private lateinit var testScope: TestScope

  private lateinit var dataStore: DataStore<Preferences>
  private lateinit var printSettingsStore: PrintSettingsStore

  @BeforeEach
  fun setUp() {
    testDispatcher = StandardTestDispatcher()
    testScope = TestScope(testDispatcher + Job())

    val testFile = File(tempDir, "test_print_settings_${System.nanoTime()}.preferences_pb")
    dataStore =
      PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { testFile },
      )
    printSettingsStore = PrintSettingsStore(dataStore, testScope)
  }

  @AfterEach
  fun tearDown() {
    testScope.cancel()
  }

  @Test
  fun `initially should have default values`() =
    runTest(testDispatcher) {
      runCurrent()
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
      runCurrent()

      val state = printSettingsStore.printSettingsState.value
      assertEquals(expectedAddress, state.selectedPrinterMacAddress)
      assertTrue(state.printFullStorageList)
    }

  @Test
  fun `updateSettings with null address should work`() =
    runTest(testDispatcher) {
      printSettingsStore.updateSettings(null, false)
      runCurrent()

      val state = printSettingsStore.printSettingsState.value
      assertNull(state.selectedPrinterMacAddress)
      assertFalse(state.printFullStorageList)
    }
}
