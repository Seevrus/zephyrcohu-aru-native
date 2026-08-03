package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.R
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Item
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.PriceList
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StorageSessionStore
import com.zephyr.boreal.store.store.StoreSessionStore
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.ui.components.TileVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.zephyr.boreal.domain.model.UserState as DomainUserState
import com.zephyr.boreal.store.user.UserState as StoreUserState

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class MainViewModelTest {
  private val userSessionStore: UserSessionStore = mock()
  private val cacheMetadataDao: com.zephyr.boreal.data.local.dao.CacheMetadataDao = mock()
  private val userRepository: UserRepository = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val printSettingsStore: PrintSettingsStore = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val storageSessionStore: StorageSessionStore = mock()
  private val storeSessionStore: StoreSessionStore = mock()
  private val itemsRepository: ItemsRepository = mock()
  private val partnersRepository: PartnersRepository = mock()
  private val storesRepository: StoresRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val isInternetReachableFlow = MutableStateFlow(true)
  private val userStoreStateFlow = MutableStateFlow(StoreUserState())
  private val printSettingsFlow = MutableStateFlow(PrintSettingsState())
  private val currentUserFlow = MutableStateFlow<ApiResource<User>>(ApiResource.Loading())
  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())
  private val isStorageSavedToApiFlow = MutableStateFlow(false)
  private val storageListItemsFlow = MutableStateFlow<List<com.zephyr.boreal.domain.model.StorageListItem>?>(null)
  private val isPendingResetAfterPrintFlow = MutableStateFlow(false)
  private val selectedStoreCurrentStateFlow = MutableStateFlow<StoreDetails?>(null)
  private val itemsFlow = MutableStateFlow<ApiResource<List<Item>>>(ApiResource.Loading())
  private val otherItemsFlow = MutableStateFlow<ApiResource<List<OtherItem>>>(ApiResource.Loading())
  private val priceListsFlow = MutableStateFlow<ApiResource<List<PriceList>>>(ApiResource.Loading())
  private val partnersFlow = MutableStateFlow<ApiResource<List<Partner>>>(ApiResource.Loading())
  private val partnerListsFlow = MutableStateFlow<ApiResource<List<PartnerList>>>(ApiResource.Loading())
  private val storesFlow = MutableStateFlow<ApiResource<List<Store>>>(ApiResource.Loading())

  private val mockUser =
    User(
      id = 1,
      userName = "test@company",
      state = com.zephyr.boreal.domain.model.UserState.IDLE,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = listOf(UserRole.APP, UserRole.LOAD_OWNED),
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
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
    whenever(userSessionStore.userState).thenReturn(userStoreStateFlow)
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsFlow)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
    whenever(storageSessionStore.isStorageSavedToApi).thenReturn(isStorageSavedToApiFlow)
    whenever(storageSessionStore.storageListItems).thenReturn(storageListItemsFlow)
    whenever(userSessionStore.isPendingResetAfterPrint).thenReturn(isPendingResetAfterPrintFlow)
    whenever(storeSessionStore.selectedStoreCurrentState).thenReturn(selectedStoreCurrentStateFlow)
    whenever(itemsRepository.getItems()).thenReturn(itemsFlow)
    whenever(itemsRepository.getOtherItems()).thenReturn(otherItemsFlow)
    whenever(itemsRepository.getPriceLists()).thenReturn(priceListsFlow)
    whenever(partnersRepository.getPartners()).thenReturn(partnersFlow)
    whenever(partnersRepository.getPartnerLists()).thenReturn(partnerListsFlow)
    whenever(storesRepository.getStores()).thenReturn(storesFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    MainViewModel(
      userSessionStore,
      cacheMetadataDao,
      userRepository,
      connectivityObserver,
      printSettingsStore,
      receiptsStore,
      storageSessionStore,
      storeSessionStore,
      itemsRepository,
      partnersRepository,
      storesRepository,
    )

  private fun buildReceipt(id: Int) =
    Receipt(
      id = id,
      companyId = 1,
      companyCode = "BOR",
      partnerId = 7,
      partnerCode = "P007",
      partnerSiteCode = "S001",
      serialNumber = id,
      yearCode = 2026,
      cancelSerialNumber = null,
      cancelYearCode = null,
      vendor = mock(),
      buyer = mock(),
      invoiceDate = "2026-07-01",
      fulfillmentDate = "2026-07-09",
      invoiceType = com.zephyr.boreal.domain.model.InvoiceType.PAPER,
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

  @Test
  fun `initially should be not ready and perform GC`() =
    runTest {
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      assertFalse(viewModel.uiState.value.isReady)
      runCurrent()
      verify(cacheMetadataDao).deleteOldEntries(any())
      uiStateJob.cancel()
    }

  @Test
  fun `should transition to ready after delay`() =
    runTest {
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      assertTrue(viewModel.uiState.value.isReady)
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be OK when idle, online, and has printer`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertNotNull(storageTile)
      assertEquals(TileVariant.OK, storageTile?.variant)
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be DISABLED with offline message when offline`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertEquals(TileVariant.DISABLED, storageTile?.variant)
      assertEquals(R.string.disabled_tile_offline, storageTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be DISABLED with no printer message when printer is missing`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = null)
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertEquals(TileVariant.DISABLED, storageTile?.variant)
      assertEquals(R.string.disabled_tile_no_printer, storageTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Sell tile should be NEUTRAL when user is ON_ROUND`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val sellTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.SELL }
      assertEquals(TileVariant.NEUTRAL, sellTile?.variant)
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking disabled tile should trigger alert`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      isInternetReachableFlow.value = false // Disables storage tile
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }!!
      viewModel.onTileClick(storageTile)
      runCurrent()

      assertNotNull(viewModel.uiState.value.alertState)
      assertEquals(
        R.string.disabled_tile_offline,
        viewModel.uiState.value.alertState
          ?.messageResId,
      )
      uiStateJob.cancel()
    }

  @Test
  fun `Alert dismissal should clear alert state`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      isInternetReachableFlow.value = false
      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }!!
      viewModel.onTileClick(storageTile)
      runCurrent()
      assertNotNull(viewModel.uiState.value.alertState)

      viewModel.dismissAlert()
      runCurrent()
      assertNull(viewModel.uiState.value.alertState)
      uiStateJob.cancel()
    }

  @Test
  fun `Sell tile should be DISABLED with logged out message when logged out`() =
    runTest {
      userStoreStateFlow.value = StoreUserState() // logged out

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val sellTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.SELL }
      assertEquals(TileVariant.DISABLED, sellTile?.variant)
      assertEquals(R.string.disabled_tile_logged_out, sellTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Receipts tile should be DISABLED with logged out message when logged out`() =
    runTest {
      userStoreStateFlow.value = StoreUserState() // logged out

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val receiptsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.RECEIPTS }
      assertEquals(TileVariant.DISABLED, receiptsTile?.variant)
      assertEquals(R.string.disabled_tile_logged_out, receiptsTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Receipts tile should be NEUTRAL when logged in, has printer, round in progress, and receipts exist`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val receiptsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.RECEIPTS }
      assertEquals(TileVariant.NEUTRAL, receiptsTile?.variant)
      assertEquals(1, receiptsTile?.titleArg)
      uiStateJob.cancel()
    }

  @Test
  fun `Receipts tile should be DISABLED with no-receipts message when receipts list is empty`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      receiptsFlow.value = emptyList()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val receiptsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.RECEIPTS }
      assertEquals(TileVariant.DISABLED, receiptsTile?.variant)
      assertEquals(R.string.disabled_tile_no_receipts, receiptsTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Receipts tile should be DISABLED with no-receipts message when round not in progress`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.IDLE))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val receiptsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.RECEIPTS }
      assertEquals(TileVariant.DISABLED, receiptsTile?.variant)
      assertEquals(R.string.disabled_tile_no_receipts, receiptsTile?.disabledMessageResId)
      assertEquals(0, receiptsTile?.titleArg)
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Receipts tile should navigate to Receipts List screen`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      receiptsFlow.value = listOf(buildReceipt(1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val receiptsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.RECEIPTS }!!
      viewModel.onTileClick(receiptsTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToReceiptsList))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be DISABLED when logged out`() =
    runTest {
      userStoreStateFlow.value = StoreUserState() // logged out

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertEquals(TileVariant.DISABLED, storageTile?.variant)
      assertEquals(R.string.disabled_tile_logged_out, storageTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be NEUTRAL when user state is LOADING`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.LOADING))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertEquals(TileVariant.NEUTRAL, storageTile?.variant)
      uiStateJob.cancel()
    }

  @Test
  fun `Storage tile should be DISABLED with no role message when roles missing`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(roles = listOf(UserRole.APP)))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }
      assertEquals(TileVariant.DISABLED, storageTile?.variant)
      assertEquals(R.string.disabled_tile_no_role, storageTile?.disabledMessageResId)
      uiStateJob.cancel()
    }

  @Test
  fun `Errands tile should be NEUTRAL when idle, online, and has printer`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val errandsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.ERRANDS }
      assertNotNull(errandsTile)
      assertEquals(TileVariant.NEUTRAL, errandsTile?.variant)
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Errands tile should navigate to Errands screen`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val errandsTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.ERRANDS }!!
      viewModel.onTileClick(errandsTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToErrands))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Sell tile should navigate to Select Partner screen`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val sellTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.SELL }!!
      viewModel.onTileClick(sellTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToSelectPartner))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Storage tile navigates to select store when no storage session is in progress`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser)
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }!!
      viewModel.onTileClick(storageTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToSelectStore))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Storage tile resumes at select items when a storage session is in progress but not saved`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.LOADING))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      storageListItemsFlow.value = emptyList()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }!!
      viewModel.onTileClick(storageTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToSelectItemsFromStore))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `Clicking Storage tile resumes at summary when changes were already saved to the API`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.LOADING))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      storageListItemsFlow.value = emptyList()
      isStorageSavedToApiFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<MainScreenEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val storageTile =
        viewModel.uiState.value.tiles
          .find { it.id == MainTileId.STORAGE }!!
      viewModel.onTileClick(storageTile)
      runCurrent()

      assertTrue(events.contains(MainScreenEvent.NavigateToStorageSummary))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `roundInfo should be populated when user has active round`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      val activeRound =
        com.zephyr.boreal.domain.model.Round(
          id = 1,
          user =
            com.zephyr.boreal.domain.model
              .RoundUser(1, "test", "Test User"),
          store =
            com.zephyr.boreal.domain.model
              .RoundStore(1, "S01", "Test Store"),
          partnerList =
            com.zephyr.boreal.domain.model
              .RoundPartnerList(1, "Test Partner List"),
          yearCode = null,
          roundStarted = "2026-06-08T10:00:00Z",
          roundFinished = null,
          lastSerialNumber = null,
          receipts = emptyList(),
        )
      currentUserFlow.value = ApiResource.Success(mockUser.copy(lastRound = activeRound))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val roundInfo = viewModel.uiState.value.roundInfo
      assertNotNull(roundInfo)
      assertEquals("Test Partner List", roundInfo?.partnerListName)
      assertEquals("Test Store", roundInfo?.storeName)
      assertEquals("2026-06-08", roundInfo?.roundStartedDate)

      uiStateJob.cancel()
    }

  @Test
  fun `roundInfo should be null when user has finished round`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      val finishedRound =
        com.zephyr.boreal.domain.model.Round(
          id = 1,
          user =
            com.zephyr.boreal.domain.model
              .RoundUser(1, "test", "Test User"),
          store =
            com.zephyr.boreal.domain.model
              .RoundStore(1, "S01", "Test Store"),
          partnerList =
            com.zephyr.boreal.domain.model
              .RoundPartnerList(1, "Test Partner List"),
          yearCode = null,
          roundStarted = "2026-06-08T10:00:00Z",
          roundFinished = "2026-06-08T12:00:00Z",
          lastSerialNumber = null,
          receipts = emptyList(),
        )
      currentUserFlow.value = ApiResource.Success(mockUser.copy(lastRound = finishedRound))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      val roundInfo = viewModel.uiState.value.roundInfo
      assertNull(roundInfo)

      uiStateJob.cancel()
    }

  private fun buildStoreDetails(id: Int) =
    StoreDetails(
      id = id,
      name = "Store $id",
      code = "S0$id",
      type = "SECONDARY",
      state = "I",
      createdAt = "2026-01-01T00:00:00Z",
      updatedAt = "2026-01-01T00:00:00Z",
      expirations = emptyList(),
    )

  @Test
  fun `LOADING state triggers items and stores refresh when online`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.LOADING))
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(itemsRepository).getItems()
      verify(storesRepository).getStores()
      verify(itemsRepository, never()).getOtherItems()
      uiStateJob.cancel()
    }

  @Test
  fun `ON_ROUND state triggers items, otherItems, partners, partnerLists and priceLists refresh when online`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(itemsRepository).getItems()
      verify(itemsRepository).getOtherItems()
      verify(itemsRepository).getPriceLists()
      verify(partnersRepository).getPartners()
      verify(partnersRepository).getPartnerLists()
      uiStateJob.cancel()
    }

  @Test
  fun `ON_ROUND state repopulates an empty store session from the user's storeInUseId`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND, storeInUseId = 5))
      isInternetReachableFlow.value = true
      val storeDetails = buildStoreDetails(5)
      whenever(storesRepository.getStoreDetails(5)).thenReturn(ApiResource.Success(storeDetails))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(storesRepository).getStoreDetails(5)
      verify(storeSessionStore).setStore(storeDetails)
      uiStateJob.cancel()
    }

  @Test
  fun `ON_ROUND state does not refetch store details when the store session is already populated`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND, storeInUseId = 5))
      isInternetReachableFlow.value = true
      selectedStoreCurrentStateFlow.value = buildStoreDetails(5)

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(storesRepository, never()).getStoreDetails(any(), any())
      uiStateJob.cancel()
    }

  @Test
  fun `no state-driven refresh happens while offline`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(itemsRepository, never()).getItems()
      uiStateJob.cancel()
    }

  @Test
  fun `no state-driven refresh happens when logged out`() =
    runTest {
      userStoreStateFlow.value = StoreUserState()
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(itemsRepository, never()).getItems()
      uiStateJob.cancel()
    }

  @Test
  fun `no state-driven refresh happens when password expired`() =
    runTest {
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", true, "2099"))
      currentUserFlow.value = ApiResource.Success(mockUser.copy(state = DomainUserState.ON_ROUND))
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceTimeBy(MainViewModel.FONT_WARMUP_DELAY_MS + 100)
      runCurrent()

      verify(itemsRepository, never()).getItems()
      uiStateJob.cancel()
    }

  @Test
  fun `pending reset after print performs local resets and logs out when the token is still valid`() =
    runTest {
      isPendingResetAfterPrintFlow.value = true
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099-01-01T00:00:00Z"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      verify(receiptsStore).resetReceipts()
      verify(storeSessionStore).resetSelectedStoreCurrentState()
      verify(userSessionStore).setPendingResetAfterPrint(false)
      verify(userRepository).logout()
      uiStateJob.cancel()
    }

  @Test
  fun `pending reset after print skips network logout when the token is expired`() =
    runTest {
      isPendingResetAfterPrintFlow.value = true
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2020-01-01T00:00:00Z"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      verify(receiptsStore).resetReceipts()
      verify(storeSessionStore).resetSelectedStoreCurrentState()
      verify(userSessionStore).setPendingResetAfterPrint(false)
      verify(userRepository, never()).logout()
      uiStateJob.cancel()
    }

  @Test
  fun `pending reset after print does nothing when the flag is false`() =
    runTest {
      isPendingResetAfterPrintFlow.value = false
      userStoreStateFlow.value = StoreUserState(storedToken = StoredToken("token", false, "2099"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      verify(receiptsStore, never()).resetReceipts()
      verify(userRepository, never()).logout()
      uiStateJob.cancel()
    }
}
