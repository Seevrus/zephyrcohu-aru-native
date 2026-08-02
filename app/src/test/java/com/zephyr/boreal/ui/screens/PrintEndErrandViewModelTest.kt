package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PrintEndErrandViewModelTest {
  private val userRepository: UserRepository = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val storeSessionStore: StoreSessionStore = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())

  private val round =
    Round(
      id = 17,
      user = RoundUser(id = 1, userName = "user1", name = "Kovács Béla"),
      store = RoundStore(id = 2, code = "K1", name = "Kamion 1"),
      partnerList = RoundPartnerList(id = 3, name = "Északi kör"),
      yearCode = 2026,
      roundStarted = "2026-01-05",
      roundFinished = "2026-01-06",
      lastSerialNumber = 50,
    )

  private fun user(lastRound: Round? = round) =
    User(
      id = 1,
      userName = "user1",
      state = UserState.ON_ROUND,
      name = "Kovács Béla",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = 2,
      storeOwnedId = null,
      lastActive = "",
      createdAt = "",
      updatedAt = "",
      company = mock(),
      lastRound = lastRound,
    )

  private val vendor =
    ReceiptVendor(
      name = "Boreal Kft.",
      country = "HU",
      postalCode = "1000",
      city = "Budapest",
      address = "Fő utca 1.",
      felir = "FELIR1",
      iban = "HU00",
      bankAccount = "11111111",
      vatNumber = "12345678-1-42",
    )

  private fun buildReceipt(serialNumber: Int) =
    Receipt(
      id = serialNumber,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = serialNumber,
      yearCode = 26,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor = vendor,
      buyer =
        ReceiptBuyer(
          id = 7,
          name = "Partner Ltd.",
          country = "HU",
          postalCode = "2000",
          city = "Szentendre",
          address = "Kossuth utca 2.",
          deliveryName = "",
          deliveryCountry = "",
          deliveryPostalCode = "",
          deliveryCity = "",
          deliveryAddress = "",
          iban = null,
          bankAccount = null,
          vatNumber = null,
        ),
      invoiceDate = "2026-01-05",
      fulfillmentDate = "2026-01-05",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-01-05",
      user = null,
      quantity = 1.0,
      netAmount = 1000.0,
      vatAmount = 0.0,
      grossAmount = 1000.0,
      roundAmount = 0.0,
      roundedAmount = 1000.0,
      lastDownloadedAt = null,
      createdAt = "2026-01-05T00:00:00Z",
      updatedAt = "2026-01-05T00:00:00Z",
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user())))
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    PrintEndErrandViewModel(userRepository, receiptsStore, storeSessionStore, testDispatcher)

  @Test
  fun `isPrintEnabled is true and text is built when user and lastRound are available`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.isPrintEnabled)
      assertTrue(
        viewModel.uiState.value.textToPrint!!
          .contains("KÖRELSZÁMOLÁS"),
      )
      uiStateJob.cancel()
    }

  @Test
  fun `isPrintEnabled is false and no text is built when there is no current user`() =
    runTest {
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(null)))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isPrintEnabled)
      assertNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `isPrintEnabled is false when the user has no lastRound`() =
    runTest {
      whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user(lastRound = null))))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.isPrintEnabled)
      assertNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `canLeaveWithoutPrint is true when there are no receipts`() =
    runTest {
      receiptsFlow.value = emptyList()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.canLeaveWithoutPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `canLeaveWithoutPrint is false when there are receipts`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.canLeaveWithoutPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `onPrintSuccess flips isPrinted`() =
    runTest {
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onPrintSuccess()

      assertTrue(viewModel.uiState.value.isPrinted)
      uiStateJob.cancel()
    }

  @Test
  fun `onReturnClick shows the exit confirmation when unprinted and non-empty`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var exited = false
      viewModel.onReturnClick { exited = true }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showExitConfirm)
      assertFalse(exited)
      uiStateJob.cancel()
    }

  @Test
  fun `onReturnClick exits directly when there is nothing to print`() =
    runTest {
      receiptsFlow.value = emptyList()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var exited = false
      viewModel.onReturnClick { exited = true }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.showExitConfirm)
      assertTrue(exited)
      uiStateJob.cancel()
    }

  @Test
  fun `onReturnClick exits directly once printed`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onPrintSuccess()

      var exited = false
      viewModel.onReturnClick { exited = true }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.showExitConfirm)
      assertTrue(exited)
      uiStateJob.cancel()
    }

  @Test
  fun `onBackPress mirrors onReturnClick's confirmation gating`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onBackPress {}
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showExitConfirm)
      uiStateJob.cancel()
    }

  @Test
  fun `confirmExit dismisses the dialog and runs the reset-and-logout sequence`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onReturnClick {}
      advanceUntilIdle()

      var exited = false
      viewModel.confirmExit { exited = true }
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.showExitConfirm)
      verify(receiptsStore).resetReceipts()
      verify(storeSessionStore).resetSelectedStoreCurrentState()
      verify(userRepository).logout()
      assertTrue(exited)
      uiStateJob.cancel()
    }

  @Test
  fun `exit runs the reset-and-logout sequence exactly once across both the button and back-press paths`() =
    runTest {
      receiptsFlow.value = emptyList()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var exitCount = 0
      viewModel.onReturnClick { exitCount++ }
      viewModel.onBackPress { exitCount++ }
      advanceUntilIdle()

      verify(receiptsStore, times(1)).resetReceipts()
      verify(storeSessionStore, times(1)).resetSelectedStoreCurrentState()
      verify(userRepository, times(1)).logout()
      assertEquals(1, exitCount)
      uiStateJob.cancel()
    }

  @Test
  fun `dismissExitConfirm hides the dialog without exiting`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onReturnClick {}
      advanceUntilIdle()
      viewModel.dismissExitConfirm()

      assertFalse(viewModel.uiState.value.showExitConfirm)
      verify(receiptsStore, times(0)).resetReceipts()
      uiStateJob.cancel()
    }
}
