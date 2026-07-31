package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsListViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val userRepository: UserRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())
  private val currentUserFlow =
    MutableStateFlow<ApiResource<User?>>(ApiResource.Success(buildUser(UserState.ON_ROUND)))

  private val buyer =
    ReceiptBuyer(
      id = 7,
      name = "Partner Ltd.",
      country = "HU",
      postalCode = "2000",
      city = "Szentendre",
      address = "Kossuth utca 2.",
      deliveryName = "Partner Ltd. Delivery",
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
    serialNumber: Int,
    grossAmount: Double = 1270.0,
    cancelSerialNumber: Int? = null,
    cancelYearCode: Int? = null,
  ) = Receipt(
    id = serialNumber,
    companyId = 1,
    companyCode = "BOR",
    partnerId = 7,
    partnerCode = "P007",
    partnerSiteCode = "S001",
    serialNumber = serialNumber,
    yearCode = 2026,
    cancelSerialNumber = cancelSerialNumber,
    cancelYearCode = cancelYearCode,
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
    grossAmount = grossAmount,
    vatAmounts = emptyList(),
    roundAmount = 0.0,
    roundedAmount = grossAmount,
    lastDownloadedAt = null,
    createdAt = "2026-07-01T00:00:00Z",
    updatedAt = "2026-07-01T00:00:00Z",
  )

  private fun buildUser(state: UserState) =
    User(
      id = 1,
      userName = "test@company",
      state = state,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = null,
      storeOwnedId = 1,
      lastActive = "2026-04-03T10:00:00Z",
      createdAt = "2026-04-03T09:00:00Z",
      updatedAt = "2026-04-03T09:00:00Z",
      company = mock(),
      lastRound = null,
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = ReceiptsListViewModel(receiptsStore, userRepository)

  @Test
  fun `empty store yields empty receipts list`() =
    runTest {
      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertTrue(
        viewModel.uiState.value.receipts
          .isEmpty(),
      )
      job.cancel()
    }

  @Test
  fun `maps store receipts to row fields preserving store order`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(serialNumber = 5), buildReceipt(serialNumber = 3))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      val rows = viewModel.uiState.value.receipts
      assertEquals(listOf(5, 3), rows.map { it.serialNumber })
      assertEquals("Partner Ltd. Delivery", rows[0].deliveryName)
      assertEquals(2026, rows[0].yearCode)
      assertEquals(1270.0, rows[0].grossAmount)
      job.cancel()
    }

  @Test
  fun `receipt with both cancel fields present is canceled`() =
    runTest {
      receiptsFlow.value =
        listOf(buildReceipt(serialNumber = 5, cancelSerialNumber = 1, cancelYearCode = 2026))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertTrue(
        viewModel.uiState.value.receipts
          .single()
          .isCanceled,
      )
      job.cancel()
    }

  @Test
  fun `receipt with only cancelSerialNumber present is not canceled`() =
    runTest {
      receiptsFlow.value =
        listOf(buildReceipt(serialNumber = 5, cancelSerialNumber = 1, cancelYearCode = null))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertFalse(
        viewModel.uiState.value.receipts
          .single()
          .isCanceled,
      )
      job.cancel()
    }

  @Test
  fun `receipts list is empty when round is not in progress even if store has receipts`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(serialNumber = 5))
      currentUserFlow.value = ApiResource.Success(buildUser(UserState.IDLE))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertTrue(
        viewModel.uiState.value.receipts
          .isEmpty(),
      )
      job.cancel()
    }

  @Test
  fun `receipt with no cancel fields is not canceled`() =
    runTest {
      receiptsFlow.value = listOf(buildReceipt(serialNumber = 5))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertFalse(
        viewModel.uiState.value.receipts
          .single()
          .isCanceled,
      )
      job.cancel()
    }
}
