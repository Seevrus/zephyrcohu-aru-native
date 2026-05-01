package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.R
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.store.user.UserState
import com.zephyr.boreal.ui.components.TileVariant
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ErrandsViewModelTest {
  private val userSessionStore: UserSessionStore = mock()
  private val userRepository: UserRepository = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val printSettingsStore: PrintSettingsStore = mock()
  private val roundsRepository: RoundsRepository = mock()
  private val testDispatcher = StandardTestDispatcher()

  private val isInternetReachableFlow = MutableStateFlow(true)
  private val userStoreStateFlow = MutableStateFlow(UserState())
  private val printSettingsFlow = MutableStateFlow(PrintSettingsState())
  private val currentUserFlow = MutableStateFlow<ApiResource<User>>(ApiResource.Loading())
  private val roundsFlow = MutableStateFlow<ApiResource<List<Round>>>(ApiResource.Success(emptyList()))

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
    whenever(userSessionStore.userState).thenReturn(userStoreStateFlow)
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsFlow)
    whenever(roundsRepository.getRounds()).thenReturn(roundsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    ErrandsViewModel(
      userSessionStore,
      userRepository,
      connectivityObserver,
      printSettingsStore,
      roundsRepository,
    )

  @Test
  fun `should navigate back if offline`() =
    runTest {
      isInternetReachableFlow.value = false
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<ErrandsEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      runCurrent()

      assertTrue(events.contains(ErrandsEvent.NavigateBack))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `should navigate back if printer is missing`() =
    runTest {
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = null)
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<ErrandsEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      runCurrent()

      assertTrue(events.contains(ErrandsEvent.NavigateBack))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `should navigate back if password is expired`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", true, "2099-01-01T00:00:00Z"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      val events = mutableListOf<ErrandsEvent>()
      val job = launch { viewModel.events.collect { events.add(it) } }

      runCurrent()

      assertTrue(events.contains(ErrandsEvent.NavigateBack))
      job.cancel()
      uiStateJob.cancel()
    }

  @Test
  fun `tiles should be present when ready and access allowed`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }

      runCurrent()

      assertTrue(viewModel.uiState.value.isReady)
      assertTrue(
        viewModel.uiState.value.tiles
          .isNotEmpty(),
      )
      assertNotNull(
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.START },
      )
    }

  @Test
  fun `Start tile should be OK when user is IDLE`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val mockUser = mock<User>()
      whenever(mockUser.state).thenReturn(com.zephyr.boreal.domain.model.UserState.IDLE)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val startTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.START }
      assertEquals(TileVariant.OK, startTile?.variant)
    }

  @Test
  fun `Start tile should be DISABLED with message when user is ON_ROUND`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val mockUser = mock<User>()
      whenever(mockUser.state).thenReturn(com.zephyr.boreal.domain.model.UserState.ON_ROUND)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val startTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.START }
      assertEquals(TileVariant.DISABLED, startTile?.variant)
      assertEquals(R.string.errands_start_disabled_on_round, startTile?.disabledMessageResId)
    }

  @Test
  fun `End tile should be WARNING when user is ON_ROUND and online`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val mockUser = mock<User>()
      whenever(mockUser.state).thenReturn(com.zephyr.boreal.domain.model.UserState.ON_ROUND)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val endTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.END }
      assertEquals(TileVariant.WARNING, endTile?.variant)
    }

  @Test
  fun `End tile should be DISABLED when user is IDLE`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true

      val mockUser = mock<User>()
      whenever(mockUser.state).thenReturn(com.zephyr.boreal.domain.model.UserState.IDLE)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val endTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.END }
      assertEquals(TileVariant.DISABLED, endTile?.variant)
      assertEquals(R.string.errands_end_disabled_not_on_round, endTile?.disabledMessageResId)
    }

  @Test
  fun `List tile should be NEUTRAL when rounds are present`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true
      roundsFlow.value = ApiResource.Success(listOf(mock<Round>()))

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val listTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.LIST }
      assertEquals(TileVariant.NEUTRAL, listTile?.variant)
    }

  @Test
  fun `List tile should be DISABLED when no rounds are present`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true
      roundsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val listTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.LIST }
      assertEquals(TileVariant.DISABLED, listTile?.variant)
      assertEquals(R.string.errands_list_disabled_empty, listTile?.disabledMessageResId)
    }

  @Test
  fun `Clicking disabled tile should trigger alert`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true
      roundsFlow.value = ApiResource.Success(emptyList()) // Disables LIST tile

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val listTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.LIST }!!
      viewModel.onTileClick(listTile)
      runCurrent()

      assertNotNull(viewModel.uiState.value.alertState)
      assertEquals(
        R.string.errands_list_disabled_empty,
        viewModel.uiState.value.alertState
          ?.messageResId,
      )
    }

  @Test
  fun `Alert dismissal should clear alert state`() =
    runTest {
      userStoreStateFlow.value = UserState(storedToken = StoredToken("token", false, "2099"))
      printSettingsFlow.value = PrintSettingsState(selectedPrinterMacAddress = "00:11:22:33:44:55")
      isInternetReachableFlow.value = true
      roundsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      backgroundScope.launch { viewModel.uiState.collect() }
      runCurrent()

      val listTile =
        viewModel.uiState.value.tiles
          .find { it.id == ErrandTileId.LIST }!!
      viewModel.onTileClick(listTile)
      runCurrent()
      assertNotNull(viewModel.uiState.value.alertState)

      viewModel.dismissAlert()
      runCurrent()
      assertTrue(viewModel.uiState.value.alertState == null)
    }
}
