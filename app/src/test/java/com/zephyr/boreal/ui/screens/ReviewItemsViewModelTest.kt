package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ReceiptsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Company
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.ReceiptUser
import com.zephyr.boreal.domain.model.ReceiptVendor
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.RoundPartnerList
import com.zephyr.boreal.domain.model.RoundStore
import com.zephyr.boreal.domain.model.RoundUser
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewItemsViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val receiptsRepository: ReceiptsRepository = mock()
  private val userRepository: UserRepository = mock()
  private val storeSessionStore: StoreSessionStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val currentReceiptFlow = MutableStateFlow<DraftReceipt?>(null)
  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())
  private val userFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Success(null))
  private val storeFlow = MutableStateFlow<StoreDetails?>(null)
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

  private fun buildDraft(withItems: Boolean = true) =
    DraftReceipt(
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      buyer = buyer,
      paymentDays = 8,
      invoiceType = InvoiceType.PAPER,
      items = if (withItems) listOf(buildItem()) else emptyList(),
      otherItems = emptyList(),
    )

  private fun buildRound(roundStarted: String = "2026-07-01T08:00:00.000000Z") =
    Round(
      id = 1,
      user = RoundUser(id = 1, userName = "user", name = "User"),
      store = RoundStore(id = 5, code = "S5", name = "Store 5"),
      partnerList = RoundPartnerList(id = 1, name = "List"),
      yearCode = 2026,
      roundStarted = roundStarted,
      roundFinished = null,
      lastSerialNumber = null,
    )

  private fun buildUser(
    lastRound: Round? = buildRound(),
    isDev: Boolean = false,
  ) = User(
    id = 1,
    userName = "user",
    state = UserState.ON_ROUND,
    name = "User",
    phoneNumber = null,
    isDev = isDev,
    roles = emptyList(),
    storeInUseId = 5,
    storeOwnedId = null,
    lastActive = "2026-07-01T08:00:00Z",
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z",
    company =
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
      ),
    lastRound = lastRound,
  )

  private fun buildStore() =
    StoreDetails(
      id = 5,
      name = "Store 5",
      code = "S5",
      type = "VAN",
      state = "ACTIVE",
      firstAvailableSerialNumber = 100,
      lastAvailableSerialNumber = 999,
      yearCode = 2026,
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
      expirations = emptyList(),
    )

  private fun buildReceipt(id: Int = 501) =
    Receipt(
      id = id,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = 100,
      yearCode = 2026,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor =
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
        ),
      buyer = buyer,
      invoiceDate = "2026-07-01",
      fulfillmentDate = "2026-07-09",
      invoiceType = InvoiceType.PAPER,
      paidDate = "2026-07-09",
      user = ReceiptUser(id = 1, userName = "user", name = "User", phoneNumber = ""),
      items = listOf(buildReceiptItem()),
      otherItems = emptyList(),
      quantity = 2.0,
      netAmount = 200.0,
      vatAmount = 54.0,
      grossAmount = 254.0,
      vatAmounts = emptyList(),
      roundAmount = 0.0,
      roundedAmount = 254.0,
      lastDownloadedAt = null,
      createdAt = "2026-07-01T00:00:00Z",
      updatedAt = "2026-07-01T00:00:00Z",
    )

  private fun createViewModel(): ReviewItemsViewModel =
    ReviewItemsViewModel(
      receiptsStore = receiptsStore,
      receiptsRepository = receiptsRepository,
      userRepository = userRepository,
      storeSessionStore = storeSessionStore,
      connectivityObserver = connectivityObserver,
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(userFlow)
    whenever(storeSessionStore.selectedStoreCurrentState).thenReturn(storeFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun buildItem(
    id: Int = 1,
    expirationId: Int = 1,
    grossAmount: Double = 1000.0,
  ) = DraftReceiptItem(
    id = id,
    articleNumber = "ART-$id",
    name = "Item $id",
    quantity = 2.0,
    unitName = "db",
    netPrice = 500.0,
    netAmount = 1000.0,
    vatRate = "27%",
    vatAmount = 270.0,
    grossAmount = grossAmount,
    discountName = null,
    expirationId = expirationId,
    cnCode = "CN001",
    expiresAt = "2026-12-31",
  )

  private fun buildReceiptItem(
    id: Int = 1,
    grossAmount: Double = 1000.0,
  ) = ReceiptItem(
    id = id,
    articleNumber = "ART-$id",
    name = "Item $id",
    quantity = 2.0,
    unitName = "db",
    netPrice = 500.0,
    netAmount = 1000.0,
    vatRate = "27%",
    vatAmount = 270.0,
    grossAmount = grossAmount,
    discountName = null,
    cnCode = "CN001",
    expiresAt = "2026-12-31",
  )

  private fun buildOtherItem(
    id: Int = 10,
    grossAmount: Double = 500.0,
  ) = ReceiptOtherItem(
    id = id,
    articleNumber = "OTH-$id",
    name = "Other Item $id",
    quantity = 1.0,
    unitName = "db",
    netPrice = grossAmount / 1.27,
    netAmount = grossAmount / 1.27,
    vatRate = "27",
    vatAmount = grossAmount - (grossAmount / 1.27),
    grossAmount = grossAmount,
    comment = null,
  )

  @Test
  fun `initially should populate items and grossTotal from store`() =
    runTest {
      val item1 = buildItem(id = 1, grossAmount = 1000.0)
      val item2 = buildItem(id = 2, expirationId = 2, grossAmount = 2000.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(item1, item2))

      val viewModel = createViewModel()
      runCurrent()

      assertEquals(listOf(item1, item2), viewModel.uiState.value.items)
      assertEquals(3000.0, viewModel.uiState.value.grossTotal)
      assertEquals(emptyList<ReceiptOtherItem>(), viewModel.uiState.value.otherItems)
    }

  @Test
  fun `onToggleExpanded adds key when not yet expanded`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent()

      viewModel.onToggleExpanded("1_1")

      assertTrue(
        viewModel.uiState.value.expandedItemKeys
          .contains("1_1"),
      )
    }

  @Test
  fun `onToggleExpanded removes key when already expanded`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent()

      viewModel.onToggleExpanded("1_1")
      viewModel.onToggleExpanded("1_1")

      assertFalse(
        viewModel.uiState.value.expandedItemKeys
          .contains("1_1"),
      )
    }

  @Test
  fun `showCancelDialog should set showCancelConfirmation to true`() =
    runTest {
      val viewModel = createViewModel()

      viewModel.showCancelDialog()

      assertTrue(viewModel.uiState.value.showCancelConfirmation)
    }

  @Test
  fun `dismissCancelDialog should set showCancelConfirmation to false`() =
    runTest {
      val viewModel = createViewModel()
      viewModel.showCancelDialog()

      viewModel.dismissCancelDialog()

      assertFalse(viewModel.uiState.value.showCancelConfirmation)
    }

  @Test
  fun `removeItem calls upsertSelectedItem and updateCurrentReceipt`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(id = 1), buildItem(id = 2, expirationId = 2)))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = {})

      verify(receiptsStore).upsertSelectedItem(1, 1, null)
      verify(receiptsStore).updateCurrentReceipt(any())
    }

  @Test
  fun `removeItem navigates home when receipt becomes empty`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = emptyList())
      val onNavigateHome: () -> Unit = mock()
      val viewModel = createViewModel()
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = onNavigateHome)

      verify(receiptsStore).resetReceipts()
      verify(onNavigateHome).invoke()
    }

  @Test
  fun `removeItem does not navigate when items remain`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(id = 2, expirationId = 2)))
      val onNavigateHome: () -> Unit = mock()
      val viewModel = createViewModel()
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = onNavigateHome)

      verifyNoInteractions(onNavigateHome)
    }

  @Test
  fun `cancelReceipt calls resetReceipts and navigates home`() =
    runTest {
      val onNavigateHome: () -> Unit = mock()
      val viewModel = createViewModel()

      viewModel.cancelReceipt(onNavigateHome)

      verify(receiptsStore).resetReceipts()
      verify(onNavigateHome).invoke()
    }

  @Test
  fun `grossTotal includes other items gross amounts`() =
    runTest {
      val item = buildItem(id = 1, grossAmount = 1000.0)
      val otherItem = buildOtherItem(id = 10, grossAmount = 500.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(item), otherItems = listOf(otherItem))

      val viewModel = createViewModel()
      runCurrent()

      assertEquals(1500.0, viewModel.uiState.value.grossTotal)
    }

  @Test
  fun `otherItems are populated from store`() =
    runTest {
      val otherItem = buildOtherItem(id = 10, grossAmount = 500.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(otherItem))

      val viewModel = createViewModel()
      runCurrent()

      assertEquals(listOf(otherItem), viewModel.uiState.value.otherItems)
    }

  @Test
  fun `onToggleOtherItemExpanded adds id when not expanded`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent()

      viewModel.onToggleOtherItemExpanded(10)

      assertTrue(
        viewModel.uiState.value.expandedOtherItemIds
          .contains(10),
      )
    }

  @Test
  fun `onToggleOtherItemExpanded removes id when already expanded`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent()

      viewModel.onToggleOtherItemExpanded(10)
      viewModel.onToggleOtherItemExpanded(10)

      assertFalse(
        viewModel.uiState.value.expandedOtherItemIds
          .contains(10),
      )
    }

  @Test
  fun `removeOtherItem calls updateCurrentReceipt`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(buildOtherItem(id = 10)))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.removeOtherItem(10)

      verify(receiptsStore).updateCurrentReceipt(any())
    }

  @Test
  fun `removeOtherItem does not reset flow even when other items become empty`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(buildOtherItem(id = 10)))
      val onNavigateHome: () -> Unit = mock()
      val viewModel = createViewModel()
      runCurrent()

      viewModel.removeOtherItem(10)

      verifyNoInteractions(onNavigateHome)
      verify(receiptsStore, never()).resetReceipts()
    }

  @Test
  fun `canFinalize is true when online with items round and store present`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      runCurrent()

      assertTrue(viewModel.uiState.value.canFinalize)
    }

  @Test
  fun `canFinalize is false when offline`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      runCurrent()

      assertFalse(viewModel.uiState.value.canFinalize)
      assertFalse(viewModel.uiState.value.isInternetReachable)
    }

  @Test
  fun `canFinalize is false when draft has no items`() =
    runTest {
      currentReceiptFlow.value = buildDraft(withItems = false)
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()

      val viewModel = createViewModel()
      runCurrent()

      assertFalse(viewModel.uiState.value.canFinalize)
    }

  @Test
  fun `canFinalize is false when user has no active round`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(lastRound = null))
      storeFlow.value = buildStore()

      val viewModel = createViewModel()
      runCurrent()

      assertFalse(viewModel.uiState.value.canFinalize)
    }

  @Test
  fun `canFinalize is false when store is missing`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = null

      val viewModel = createViewModel()
      runCurrent()

      assertFalse(viewModel.uiState.value.canFinalize)
    }

  @Test
  fun `showFinalizeDialog and dismissFinalizeDialog toggle showFinalizeConfirmation`() =
    runTest {
      val viewModel = createViewModel()
      runCurrent()

      viewModel.showFinalizeDialog()
      assertTrue(viewModel.uiState.value.showFinalizeConfirmation)

      viewModel.dismissFinalizeDialog()
      assertFalse(viewModel.uiState.value.showFinalizeConfirmation)
    }

  @Test
  fun `confirmFinalize sets missing-data error when round is missing`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(lastRound = null))
      storeFlow.value = buildStore()
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      assertEquals("Hiányzó adatok: aktuális kör", viewModel.uiState.value.errorMessage)
      assertTrue(viewModel.uiState.value.isSentWithErrors)
      assertFalse(viewModel.uiState.value.isSentSuccessfully)
      verifyNoInteractions(receiptsRepository)
    }

  @Test
  fun `confirmFinalize sets missing-data error when store is missing`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = null
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      assertEquals("Hiányzó adatok: kör raktár", viewModel.uiState.value.errorMessage)
      assertTrue(viewModel.uiState.value.isSentWithErrors)
    }

  @Test
  fun `confirmFinalize computes invoiceDate when roundStarted is a date-only string`() =
    runTest {
      // The real backend (RoundResource) formats roundStarted as a plain "yyyy-MM-dd" date,
      // not a full ISO-8601 instant.
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(lastRound = buildRound(roundStarted = "2026-07-05")))
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Success(buildReceipt()))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      val captor = argumentCaptor<com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto>()
      verify(receiptsRepository).createReceipt(captor.capture())
      assertEquals("2026-07-05", captor.firstValue.invoiceDate)
      assertFalse(viewModel.uiState.value.isSentWithErrors)
      assertTrue(viewModel.uiState.value.isSentSuccessfully)
    }

  @Test
  fun `confirmFinalize computes invoiceDate when roundStarted is a full ISO instant`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(lastRound = buildRound(roundStarted = "2026-07-05T08:00:00Z")))
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Success(buildReceipt()))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      val captor = argumentCaptor<com.zephyr.boreal.api.dto.request.CreateReceiptRequestDataDto>()
      verify(receiptsRepository).createReceipt(captor.capture())
      assertEquals("2026-07-05", captor.firstValue.invoiceDate)
      assertTrue(viewModel.uiState.value.isSentSuccessfully)
    }

  @Test
  fun `confirmFinalize submits receipt and appends it to receiptsStore on success`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()
      val createdReceipt = buildReceipt()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Success(createdReceipt))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.showFinalizeDialog()
      viewModel.confirmFinalize()
      runCurrent()

      verify(receiptsStore).addReceipt(createdReceipt)
      verify(receiptsStore, never()).setCurrentReceipt(any())
      verify(receiptsStore, never()).updateCurrentReceipt(any())
      assertEquals("Számla sikeresen beküldve.", viewModel.uiState.value.successMessage)
      assertNull(viewModel.uiState.value.errorMessage)
      assertFalse(viewModel.uiState.value.isSentWithErrors)
      assertTrue(viewModel.uiState.value.isSentSuccessfully)
      assertFalse(viewModel.uiState.value.showFinalizeConfirmation)
      assertFalse(viewModel.uiState.value.isLoading)
    }

  @Test
  fun `confirmFinalize shows raw error message for dev users`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(isDev = true))
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Error("boom"))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      assertEquals("boom", viewModel.uiState.value.errorMessage)
      assertTrue(viewModel.uiState.value.isSentWithErrors)
    }

  @Test
  fun `confirmFinalize shows generic error message for non-dev users`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser(isDev = false))
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Error("boom"))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()

      assertEquals("Számla beküldése sikertelen.", viewModel.uiState.value.errorMessage)
      assertTrue(viewModel.uiState.value.isSentWithErrors)
    }

  @Test
  fun `retryReceipt resubmits after a previous error`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any()))
        .thenReturn(ApiResource.Error("boom"))
        .thenReturn(ApiResource.Success(buildReceipt()))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()
      assertTrue(viewModel.uiState.value.isSentWithErrors)

      viewModel.retryReceipt()
      runCurrent()

      verify(receiptsRepository, times(2)).createReceipt(any())
      assertTrue(viewModel.uiState.value.isSentSuccessfully)
      assertFalse(viewModel.uiState.value.isSentWithErrors)
    }

  @Test
  fun `confirmFinalize is a no-op after a successful submit`() =
    runTest {
      currentReceiptFlow.value = buildDraft()
      userFlow.value = ApiResource.Success(buildUser())
      storeFlow.value = buildStore()
      whenever(receiptsRepository.createReceipt(any())).thenReturn(ApiResource.Success(buildReceipt()))
      val viewModel = createViewModel()
      runCurrent()

      viewModel.confirmFinalize()
      runCurrent()
      viewModel.confirmFinalize()
      runCurrent()

      verify(receiptsRepository, times(1)).createReceipt(any())
    }
}
