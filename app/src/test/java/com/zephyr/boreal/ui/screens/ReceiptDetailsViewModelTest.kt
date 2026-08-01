package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.zephyr.boreal.api.dto.request.CreateCancelReceiptDataDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ReceiptsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptDetailsViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val receiptsRepository: ReceiptsRepository = mock()
  private val storeSessionStore: StoreSessionStore = mock()
  private val userRepository: UserRepository = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val receiptFlow = MutableStateFlow<Receipt?>(null)
  private val isInternetReachableFlow = MutableStateFlow(true)
  private val selectedStoreCurrentStateFlow = MutableStateFlow<StoreDetails?>(null)
  private val currentUserFlow =
    MutableStateFlow<ApiResource<User?>>(ApiResource.Success(buildUser()))

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

  private fun buildReceiptItem(
    articleNumber: String,
    expiresAt: String,
    quantity: Double,
  ) = ReceiptItem(
    id = 1,
    articleNumber = articleNumber,
    name = "Item",
    quantity = quantity,
    unitName = "db",
    netPrice = 100.0,
    netAmount = 100.0 * quantity,
    vatRate = "27",
    vatAmount = 27.0 * quantity,
    grossAmount = 127.0 * quantity,
    discountName = null,
    cnCode = "1234",
    expiresAt = expiresAt,
  )

  private fun buildReceipt(
    id: Int = 42,
    serialNumber: Int = 5,
    cancelSerialNumber: Int? = null,
    cancelYearCode: Int? = null,
    items: List<ReceiptItem> = emptyList(),
  ) = Receipt(
    id = id,
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
    items = items,
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

  private fun buildStore(
    firstAvailableSerialNumber: Int = 100,
    yearCode: Int = 2026,
    expirations: List<StoreDetailsExpiration> = emptyList(),
  ) = StoreDetails(
    id = 1,
    name = "Main store",
    code = "S001",
    type = "MAIN",
    state = "ACTIVE",
    firstAvailableSerialNumber = firstAvailableSerialNumber,
    lastAvailableSerialNumber = 999,
    yearCode = yearCode,
    owner = null,
    user = null,
    createdAt = "2026-07-01T00:00:00Z",
    updatedAt = "2026-07-01T00:00:00Z",
    expirations = expirations,
  )

  private fun buildUser(isDev: Boolean = false) =
    User(
      id = 1,
      userName = "test@company",
      state = UserState.ON_ROUND,
      name = "Test User",
      phoneNumber = null,
      isDev = isDev,
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
    whenever(receiptsStore.receiptBySerialNumber(any())).thenReturn(receiptFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
    whenever(storeSessionStore.selectedStoreCurrentState).thenReturn(selectedStoreCurrentStateFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(serialNumber: Int = 5) =
    ReceiptDetailsViewModel(
      savedStateHandle = SavedStateHandle(mapOf("serialNumber" to serialNumber)),
      receiptsStore = receiptsStore,
      receiptsRepository = receiptsRepository,
      storeSessionStore = storeSessionStore,
      userRepository = userRepository,
      connectivityObserver = connectivityObserver,
    )

  @Test
  fun `canCancel is true when not canceled, online, synced and store present`() =
    runTest {
      receiptFlow.value = buildReceipt(id = 42)
      selectedStoreCurrentStateFlow.value = buildStore()

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertTrue(viewModel.uiState.value.canCancel)
      job.cancel()
    }

  @Test
  fun `canCancel is false when receipt already canceled`() =
    runTest {
      receiptFlow.value = buildReceipt(cancelSerialNumber = 1, cancelYearCode = 2026)
      selectedStoreCurrentStateFlow.value = buildStore()

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertFalse(viewModel.uiState.value.canCancel)
      job.cancel()
    }

  @Test
  fun `canCancel is false when offline`() =
    runTest {
      receiptFlow.value = buildReceipt()
      selectedStoreCurrentStateFlow.value = buildStore()
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertFalse(viewModel.uiState.value.canCancel)
      job.cancel()
    }

  @Test
  fun `canCancel is false when store missing`() =
    runTest {
      receiptFlow.value = buildReceipt()
      selectedStoreCurrentStateFlow.value = null

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      assertFalse(viewModel.uiState.value.canCancel)
      job.cancel()
    }

  @Test
  fun `requestCancel with no receipt sets not-found error`() =
    runTest {
      receiptFlow.value = null
      selectedStoreCurrentStateFlow.value = buildStore()

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      viewModel.requestCancel()
      runCurrent()

      assertEquals("Számla nem található.", viewModel.uiState.value.errorMessage)
      job.cancel()
    }

  @Test
  fun `requestCancel with no store sets store-missing error`() =
    runTest {
      receiptFlow.value = buildReceipt()
      selectedStoreCurrentStateFlow.value = null

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      viewModel.requestCancel()
      runCurrent()

      assertEquals("Kör raktár nem talákható.", viewModel.uiState.value.errorMessage)
      job.cancel()
    }

  @Test
  fun `successful cancel upserts receipt, restores summed quantities and marks canceled now`() =
    runTest {
      val items =
        listOf(
          buildReceiptItem(articleNumber = "A1", expiresAt = "2026-08-01", quantity = 2.0),
          buildReceiptItem(articleNumber = "A1", expiresAt = "2026-08-01", quantity = 3.0),
          buildReceiptItem(articleNumber = "A2", expiresAt = "2026-09-01", quantity = 1.0),
        )
      val expirationA1 =
        StoreDetailsExpiration(
          itemId = 10,
          articleNumber = "A1",
          expirationId = 100,
          expiresAt = "2026-08-01",
          quantity = 5.0,
        )
      val expirationA2 =
        StoreDetailsExpiration(
          itemId = 20,
          articleNumber = "A2",
          expirationId = 200,
          expiresAt = "2026-09-01",
          quantity = 8.0,
        )
      val store =
        buildStore(firstAvailableSerialNumber = 100, yearCode = 2026, expirations = listOf(expirationA1, expirationA2))
      val originalReceipt = buildReceipt(id = 42, items = items)
      val cancelledReceipt = originalReceipt.copy(cancelSerialNumber = 101, cancelYearCode = 2026)

      receiptFlow.value = originalReceipt
      selectedStoreCurrentStateFlow.value = store
      whenever(receiptsStore.nextAvailableSerialNumber(100)).thenReturn(101)
      whenever(receiptsRepository.cancelReceipt(any())).thenReturn(ApiResource.Success(cancelledReceipt))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      viewModel.requestCancel()
      runCurrent()

      val requestCaptor = argumentCaptor<CreateCancelReceiptDataDto>()
      verify(receiptsRepository).cancelReceipt(requestCaptor.capture())
      assertEquals(42, requestCaptor.firstValue.id)
      assertEquals(101, requestCaptor.firstValue.cancelSerialNumber)
      assertEquals(2026, requestCaptor.firstValue.cancelYearCode)

      verify(receiptsStore).addReceipt(cancelledReceipt)
      verify(storeSessionStore).updateExpirationQuantity(eq(10), eq(100), eq(5.0))
      verify(storeSessionStore).updateExpirationQuantity(eq(20), eq(200), eq(1.0))
      verify(storeSessionStore, times(2)).updateExpirationQuantity(any(), any(), any())

      assertTrue(viewModel.uiState.value.hasCanceledNow)
      assertNull(viewModel.uiState.value.errorMessage)
      assertFalse(viewModel.uiState.value.isCancelInProgress)
      job.cancel()
    }

  @Test
  fun `cancel error shows raw message for dev users`() =
    runTest {
      receiptFlow.value = buildReceipt(id = 42)
      selectedStoreCurrentStateFlow.value = buildStore()
      currentUserFlow.value = ApiResource.Success(buildUser(isDev = true))
      whenever(receiptsStore.nextAvailableSerialNumber(any())).thenReturn(101)
      whenever(receiptsRepository.cancelReceipt(any())).thenReturn(ApiResource.Error("raw backend detail"))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      viewModel.requestCancel()
      runCurrent()

      assertEquals("raw backend detail", viewModel.uiState.value.errorMessage)
      job.cancel()
    }

  @Test
  fun `cancel error shows generic message for non-dev users`() =
    runTest {
      receiptFlow.value = buildReceipt(id = 42)
      selectedStoreCurrentStateFlow.value = buildStore()
      currentUserFlow.value = ApiResource.Success(buildUser(isDev = false))
      whenever(receiptsStore.nextAvailableSerialNumber(any())).thenReturn(101)
      whenever(receiptsRepository.cancelReceipt(any())).thenReturn(ApiResource.Error("raw backend detail"))

      val viewModel = createViewModel()
      val job = launch { viewModel.uiState.collect() }
      runCurrent()

      viewModel.requestCancel()
      runCurrent()

      assertEquals(
        "Váratlan hiba lépett fel a számla sztornózása során.",
        viewModel.uiState.value.errorMessage,
      )
      job.cancel()
    }
}
