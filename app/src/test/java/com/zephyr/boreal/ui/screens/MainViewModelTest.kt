package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.R
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.zephyr.boreal.domain.model.UserState as DomainUserState
import com.zephyr.boreal.store.user.UserState as StoreUserState

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
  private val userSessionStore: UserSessionStore = mock()
  private val cacheMetadataDao: com.zephyr.boreal.data.local.dao.CacheMetadataDao = mock()
  private val userRepository: UserRepository = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val printSettingsStore: PrintSettingsStore = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val isInternetReachableFlow = MutableStateFlow(true)
  private val userStoreStateFlow = MutableStateFlow(StoreUserState())
  private val printSettingsFlow = MutableStateFlow(PrintSettingsState())
  private val currentUserFlow = MutableStateFlow<ApiResource<User>>(ApiResource.Loading())

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
}
