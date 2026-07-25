package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())
  private val isInternetReachableFlow = MutableStateFlow(true)

  private val testDispatcher = StandardTestDispatcher()

  private val buyer =
    ReceiptBuyer(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd.",
      deliveryCountry = "HU",
      deliveryPostalCode = "2000",
      deliveryCity = "Szentendre",
      deliveryAddress = "Kossuth utca 2.",
      iban = null,
      bankAccount = null,
      vatNumber = "87654321-1-13",
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

  private fun buildReceipt(
    id: Int,
    serialNumber: Int,
  ) = Receipt(
    id = id,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = serialNumber,
    yearCode = 2026,
    cancelSerialNumber = null,
    cancelYearCode = null,
    vendor = vendor,
    buyer = buyer,
    invoiceDate = "2026-07-01",
    fulfillmentDate = "2026-07-09",
    invoiceType = InvoiceType.PAPER,
    paidDate = "2026-07-09",
    user = null,
    items = emptyList(),
    otherItems = emptyList(),
    quantity = 2.0,
    netAmount = 1000.0,
    vatAmount = 270.0,
    grossAmount = 1270.0,
    vatAmounts = emptyList(),
    roundAmount = 0.0,
    roundedAmount = 1270.0,
    lastDownloadedAt = null,
    createdAt = "2026-07-01T00:00:00Z",
    updatedAt = "2026-07-01T00:00:00Z",
  )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = SummaryViewModel(receiptsStore, connectivityObserver)

  @Test
  fun `uiState reads the last finalized receipt`() =
    runTest(testDispatcher) {
      receiptsFlow.value = listOf(buildReceipt(id = 1, serialNumber = 101), buildReceipt(id = 2, serialNumber = 102))
      val viewModel = createViewModel()
      runCurrent()

      assertEquals(
        102,
        viewModel.uiState.value.receipt
          ?.serialNumber,
      )
    }

  @Test
  fun `uiState reflects offline connectivity`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      runCurrent()

      isInternetReachableFlow.value = false
      runCurrent()

      assertFalse(viewModel.uiState.value.isInternetReachable)
    }

  @Test
  fun `onLeave resets the sell-flow draft and navigates home`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      runCurrent()
      val onNavigateHome: () -> Unit = mock()

      viewModel.onLeave(onNavigateHome)
      advanceUntilIdle()

      verify(receiptsStore).resetSellFlowDraft()
      verify(onNavigateHome).invoke()
      assertTrue(viewModel.uiState.value.isLoading)
    }

  @Test
  fun `onLeave only resets and navigates once when called twice`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      runCurrent()
      val onNavigateHome: () -> Unit = mock()

      viewModel.onLeave(onNavigateHome)
      advanceUntilIdle()
      viewModel.onLeave(onNavigateHome)
      advanceUntilIdle()

      verify(receiptsStore, times(1)).resetSellFlowDraft()
      verify(onNavigateHome, times(1)).invoke()
    }

  @Test
  fun `receipt is null when no receipts exist yet`() =
    runTest(testDispatcher) {
      val viewModel = createViewModel()
      runCurrent()

      assertNull(viewModel.uiState.value.receipt)
      verify(receiptsStore, never()).resetSellFlowDraft()
    }
}
