package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.printer.BluetoothDeviceData
import com.zephyr.boreal.printer.BluetoothHelper
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PrintSettingsViewModelTest {
  private val printSettingsStore: PrintSettingsStore = mock()
  private val bluetoothHelper: BluetoothHelper = mock()
  private val userRepository: UserRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val printSettingsStateFlow = MutableStateFlow(PrintSettingsState())
  private val bluetoothStateFlow = MutableStateFlow(false)

  private val company =
    Company(
      id = 1,
      name = "Boreal Kft.",
      code = "BOR",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      vatNumber = "12345678-1-42",
      iban = "HU00",
      bankAccount = "11111111",
    )

  private fun buildUser(isDev: Boolean) =
    User(
      id = 1,
      userName = "teszt",
      state = UserState.ON_ROUND,
      name = "Teszt Elek",
      phoneNumber = null,
      isDev = isDev,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = null,
      lastActive = "2026-07-01T00:00:00Z",
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
      company = company,
      lastRound = null,
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsStateFlow)
    whenever(bluetoothHelper.observeBluetoothState()).thenReturn(bluetoothStateFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(null)))
  }

  private fun createViewModel() = PrintSettingsViewModel(printSettingsStore, bluetoothHelper, userRepository)

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initially should reflect settings from store`() =
    runTest {
      val expectedAddress = "00:11:22:33:44:55"
      val expectedFullStorageList = true
      printSettingsStateFlow.value =
        PrintSettingsState(
          selectedPrinterMacAddress = expectedAddress,
          printFullStorageList = expectedFullStorageList,
        )

      val viewModel = createViewModel()
      runCurrent()

      assertEquals(expectedAddress, viewModel.uiState.value.selectedPrinterAddress)
      assertEquals(expectedFullStorageList, viewModel.uiState.value.printFullStorageList)
    }

  @Test
  fun `updatePermissionsState should update state and refresh bluetooth if granted`() =
    runTest {
      whenever(bluetoothHelper.isBluetoothEnabled()).thenReturn(true)
      bluetoothStateFlow.value = true
      whenever(bluetoothHelper.getPairedDevices(false)).thenReturn(listOf(BluetoothDeviceData("Test", "Address")))

      val viewModel = createViewModel()
      runCurrent()

      viewModel.updatePermissionsState(hasPermissions = true, canAskPermissions = false)
      runCurrent()

      assertTrue(viewModel.uiState.value.hasPermissions)
      assertFalse(viewModel.uiState.value.canAskPermissions)
      assertFalse(viewModel.uiState.value.isInitialCheck)
      assertTrue(viewModel.uiState.value.isBluetoothEnabled)
      assertEquals(1, viewModel.uiState.value.pairedDevices.size)
    }

  @Test
  fun `surfaces dev device when current user isDev is true even without bluetooth permissions`() =
    runTest {
      whenever(bluetoothHelper.isBluetoothEnabled()).thenReturn(true)
      bluetoothStateFlow.value = true
      whenever(userRepository.getCurrentUser())
        .thenReturn(flowOf(ApiResource.Success(buildUser(isDev = true))))
      whenever(bluetoothHelper.getPairedDevices(true))
        .thenReturn(listOf(BluetoothDeviceData("Dev Test", "00:00:00:00:00:00")))

      val viewModel = createViewModel()
      runCurrent()

      assertEquals(1, viewModel.uiState.value.pairedDevices.size)
      assertEquals(
        "Dev Test",
        viewModel.uiState.value.pairedDevices
          .first()
          .name,
      )
    }

  @Test
  fun `onPrinterSelected should update uiState`() =
    runTest {
      val viewModel = createViewModel()
      val address = "AA:BB:CC"

      viewModel.onPrinterSelected(address)

      assertEquals(address, viewModel.uiState.value.selectedPrinterAddress)
    }

  @Test
  fun `onPrintFullStorageListChanged should update uiState`() =
    runTest {
      val viewModel = createViewModel()

      viewModel.onPrintFullStorageListChanged(true)

      assertTrue(viewModel.uiState.value.printFullStorageList)
    }

  @Test
  fun `saveSettings should call printSettingsStore updateSettings`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent() // Process initial collection in init

      val address = "AA:BB:CC"
      val fullList = true

      viewModel.onPrinterSelected(address)
      viewModel.onPrintFullStorageListChanged(fullList)
      viewModel.saveSettings()
      runCurrent()

      verify(printSettingsStore).updateSettings(address, fullList)
    }

  @Test
  fun `finishInitialCheck should update state`() =
    runTest {
      val viewModel = createViewModel()

      viewModel.finishInitialCheck()

      assertFalse(viewModel.uiState.value.isInitialCheck)
    }
}
