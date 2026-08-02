package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsState
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.store.StorageSessionStore
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSummaryViewModelTest {
  private val storesRepository: StoresRepository = mock()
  private val userRepository: UserRepository = mock()
  private val storageSessionStore: StorageSessionStore = mock()
  private val printSettingsStore: PrintSettingsStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val isInternetReachableFlow = MutableStateFlow(true)
  private val selectedStoreInitialStateFlow = MutableStateFlow<StoreDetails?>(null)
  private val storageListItemsFlow = MutableStateFlow<List<StorageListItem>?>(null)
  private val printSettingsFlow = MutableStateFlow(PrintSettingsState())

  private val user =
    User(
      id = 1,
      userName = "user1",
      state = UserState.LOADING,
      name = "Test User",
      phoneNumber = null,
      isDev = false,
      roles = emptyList(),
      storeInUseId = 2,
      storeOwnedId = null,
      lastActive = "",
      createdAt = "",
      updatedAt = "",
      company = mock(),
      lastRound = null,
    )

  private fun storeDetails() =
    StoreDetails(
      id = 2,
      name = "Kamion",
      code = "K1",
      type = "S",
      state = "L",
      createdAt = "",
      updatedAt = "",
      expirations = emptyList(),
    )

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
    whenever(storageSessionStore.selectedStoreInitialState).thenReturn(selectedStoreInitialStateFlow)
    whenever(storageSessionStore.storageListItems).thenReturn(storageListItemsFlow)
    whenever(printSettingsStore.printSettingsState).thenReturn(printSettingsFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(flowOf(ApiResource.Success(user)))
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    StorageSummaryViewModel(
      storesRepository,
      userRepository,
      storageSessionStore,
      printSettingsStore,
      connectivityObserver,
      testDispatcher,
    )

  @Test
  fun `builds print text when user and initial store snapshot are available`() =
    runTest {
      selectedStoreInitialStateFlow.value = storeDetails()
      storageListItemsFlow.value = listOf()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertNotNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `suggestedFileName is derived from the store's name, code and today's date`() =
    runTest {
      selectedStoreInitialStateFlow.value = storeDetails()
      storageListItemsFlow.value = listOf()

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(
        viewModel.uiState.value.suggestedFileName
          .startsWith("Kamion_K1_"),
      )
      assertTrue(
        viewModel.uiState.value.suggestedFileName
          .endsWith(".txt"),
      )
      uiStateJob.cancel()
    }

  @Test
  fun `suggestedFileName is blank when there is no initial store snapshot`() =
    runTest {
      selectedStoreInitialStateFlow.value = null

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals("", viewModel.uiState.value.suggestedFileName)
      uiStateJob.cancel()
    }

  @Test
  fun `leaves textToPrint null when there is no initial store snapshot`() =
    runTest {
      selectedStoreInitialStateFlow.value = null

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertNull(viewModel.uiState.value.textToPrint)
      uiStateJob.cancel()
    }

  @Test
  fun `isInternetReachable reflects the connectivity observer`() =
    runTest {
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals(false, viewModel.uiState.value.isInternetReachable)
      uiStateJob.cancel()
    }

  @Test
  fun `onLeave unlocks, refreshes the user, clears the session and navigates home`() =
    runTest {
      whenever(storesRepository.deselectStore()).thenReturn(ApiResource.Success(Unit))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var navigatedHome = false
      viewModel.onLeave { navigatedHome = true }
      advanceUntilIdle()

      verify(storesRepository).deselectStore()
      verify(userRepository).refreshCurrentUser()
      verify(storageSessionStore).clearStores()
      assertEquals(true, navigatedHome)
      uiStateJob.cancel()
    }

  @Test
  fun `onLeave runs the unlock-and-reset sequence only once across repeated calls`() =
    runTest {
      whenever(storesRepository.deselectStore()).thenReturn(ApiResource.Success(Unit))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var navigateHomeCallCount = 0
      viewModel.onLeave { navigateHomeCallCount++ }
      viewModel.onLeave { navigateHomeCallCount++ }
      advanceUntilIdle()

      verify(storesRepository, times(1)).deselectStore()
      verify(storageSessionStore, times(1)).clearStores()
      assertEquals(1, navigateHomeCallCount)
      uiStateJob.cancel()
    }

  @Test
  fun `onLeave proceeds to reset and navigate home even if unlock fails`() =
    runTest {
      whenever(storesRepository.deselectStore()).thenReturn(ApiResource.Error("offline"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var navigatedHome = false
      viewModel.onLeave { navigatedHome = true }
      advanceUntilIdle()

      verify(storageSessionStore).clearStores()
      assertEquals(true, navigatedHome)
      uiStateJob.cancel()
    }
}
