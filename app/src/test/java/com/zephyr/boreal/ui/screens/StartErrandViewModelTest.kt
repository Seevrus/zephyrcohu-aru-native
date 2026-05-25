package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.StoreUser
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StartErrandViewModelTest {
  private val roundsRepository: RoundsRepository = mock()
  private val storesRepository: StoresRepository = mock()
  private val partnersRepository: PartnersRepository = mock()
  private val userRepository: UserRepository = mock()
  private val itemsRepository: ItemsRepository = mock()
  private val storeSessionStore: StoreSessionStore = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val isOnlineFlow = MutableStateFlow(true)
  private val currentUserFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Loading())
  private val storesFlow =
    MutableStateFlow<ApiResource<List<Store>>>(ApiResource.Loading())
  private val partnerListsFlow =
    MutableStateFlow<ApiResource<List<PartnerList>>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isOnlineFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
    whenever(storesRepository.getStores(any())).thenReturn(storesFlow)
    whenever(partnersRepository.getPartnerLists(any())).thenReturn(partnerListsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): StartErrandViewModel =
    StartErrandViewModel(
      roundsRepository,
      storesRepository,
      partnersRepository,
      userRepository,
      itemsRepository,
      storeSessionStore,
      receiptsStore,
      connectivityObserver,
    )

  @Test
  fun `loadInitialData should fetch user using standard getCurrentUser and force refetch stores and partner lists`() =
    runTest {
      val mockUser = mock<User>()
      whenever(mockUser.id).thenReturn(42)
      currentUserFlow.value = ApiResource.Success(mockUser)

      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()

      advanceUntilIdle()

      // Verify getCurrentUser was called (cache is fine for user)
      verify(userRepository).getCurrentUser()

      // Verify stores and partner lists are fetched with forceRefresh = true
      verify(storesRepository).getStores(eq(true))
      verify(partnersRepository).getPartnerLists(eq(true))

      // UI state checks
      val state = viewModel.uiState.value
      assertEquals(mockUser, state.currentUser)
      assertTrue(state.stores.isEmpty())
      assertTrue(state.partnerLists.isEmpty())
      assertFalse(state.isLoading)
      assertNull(state.errorMessage)
    }

  @Test
  fun `loadInitialData handling failures`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Error("Store fetch failed")
      partnerListsFlow.value = ApiResource.Error("Partners fetch failed")

      val viewModel = createViewModel()

      advanceUntilIdle()

      val state = viewModel.uiState.value
      // Verification of failure message propagation
      assertTrue(state.errorMessage != null)
      assertFalse(state.isLoading)
    }

  @Test
  fun `connectivity changes should update UI state isInternetReachable`() =
    runTest {
      isOnlineFlow.value = true
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      advanceUntilIdle()
      assertTrue(viewModel.uiState.value.isInternetReachable)

      isOnlineFlow.value = false
      advanceUntilIdle()
      assertFalse(viewModel.uiState.value.isInternetReachable)

      uiStateJob.cancel()
    }

  @Test
  fun `updateDefaultStore should auto-select store owned by current user if selectedStoreId is null`() =
    runTest {
      val mockUser = mock<User>()
      whenever(mockUser.id).thenReturn(42)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val storeUserOwned = mock<Store>()
      whenever(storeUserOwned.id).thenReturn(101)
      val storeOwner = mock<StoreUser>()
      whenever(storeOwner.id).thenReturn(42)
      whenever(storeUserOwned.owner).thenReturn(storeOwner)

      val otherStore = mock<Store>()
      whenever(otherStore.id).thenReturn(102)
      whenever(otherStore.owner).thenReturn(null)

      storesFlow.value = ApiResource.Success(listOf(otherStore, storeUserOwned))
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals(101, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `updateDefaultStore should not overwrite manually selected store`() =
    runTest {
      val mockUser = mock<User>()
      whenever(mockUser.id).thenReturn(42)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val storeUserOwned = mock<Store>()
      whenever(storeUserOwned.id).thenReturn(101)
      val storeOwner = mock<StoreUser>()
      whenever(storeOwner.id).thenReturn(42)
      whenever(storeUserOwned.owner).thenReturn(storeOwner)

      storesFlow.value = ApiResource.Success(listOf(storeUserOwned))
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      viewModel.onStoreSelected(999)
      advanceUntilIdle()

      assertEquals(999, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `uiState isConfirmEnabled should satisfy specific conditions`() {
    val stateWithNoSelections =
      StartErrandUiState(
        selectedStoreId = null,
        selectedPartnerListId = null,
        isStartingRound = false,
      )
    assertFalse(stateWithNoSelections.isConfirmEnabled)

    val stateWithOnlyStore =
      StartErrandUiState(
        selectedStoreId = 1,
        selectedPartnerListId = null,
        isStartingRound = false,
      )
    assertFalse(stateWithOnlyStore.isConfirmEnabled)

    val stateWithOnlyPartnerList =
      StartErrandUiState(
        selectedStoreId = null,
        selectedPartnerListId = 2,
        isStartingRound = false,
      )
    assertFalse(stateWithOnlyPartnerList.isConfirmEnabled)

    val stateWithSelectionsButStarting =
      StartErrandUiState(
        selectedStoreId = 1,
        selectedPartnerListId = 2,
        isStartingRound = true,
      )
    assertFalse(stateWithSelectionsButStarting.isConfirmEnabled)

    val stateReady =
      StartErrandUiState(
        selectedStoreId = 1,
        selectedPartnerListId = 2,
        isStartingRound = false,
      )
    assertTrue(stateReady.isConfirmEnabled)
  }

  @Test
  fun `uiState displayStores should filter and sort correctly`() {
    val mockUser = mock<User>()
    whenever(mockUser.id).thenReturn(42)

    val store1 = mock<Store>()
    whenever(store1.name).thenReturn("Store B")
    whenever(store1.type).thenReturn(StoreType.SECONDARY)
    whenever(store1.state).thenReturn(UserState.IDLE)
    val owner1 = mock<StoreUser>()
    whenever(owner1.id).thenReturn(42)
    whenever(store1.owner).thenReturn(owner1)

    val store2 = mock<Store>()
    whenever(store2.name).thenReturn("Store A")
    whenever(store2.type).thenReturn(StoreType.SECONDARY)
    whenever(store2.state).thenReturn(UserState.IDLE)
    whenever(store2.owner).thenReturn(null)

    val store3 = mock<Store>()
    whenever(store3.type).thenReturn(StoreType.PRIMARY)
    whenever(store3.state).thenReturn(UserState.IDLE)
    whenever(store3.owner).thenReturn(null)

    val store4 = mock<Store>()
    whenever(store4.type).thenReturn(StoreType.SECONDARY)
    whenever(store4.state).thenReturn(UserState.ON_ROUND)
    whenever(store4.owner).thenReturn(null)

    val store5 = mock<Store>()
    whenever(store5.type).thenReturn(StoreType.SECONDARY)
    whenever(store5.state).thenReturn(UserState.IDLE)
    val owner5 = mock<StoreUser>()
    whenever(owner5.id).thenReturn(99)
    whenever(store5.owner).thenReturn(owner5)

    val state =
      StartErrandUiState(
        currentUser = mockUser,
        stores = listOf(store1, store2, store3, store4, store5),
      )

    val displayStores = state.displayStores
    assertEquals(2, displayStores.size)
    assertEquals(store2, displayStores[0])
    assertEquals(store1, displayStores[1])
  }

  @Test
  fun `uiState displayPartnerLists should sort alphabetically`() {
    val list1 = mock<PartnerList>()
    whenever(list1.name).thenReturn("List Z")
    val list2 = mock<PartnerList>()
    whenever(list2.name).thenReturn("List M")
    val list3 = mock<PartnerList>()
    whenever(list3.name).thenReturn("List A")

    val state = StartErrandUiState(partnerLists = listOf(list1, list2, list3))
    val sorted = state.displayPartnerLists

    assertEquals(3, sorted.size)
    assertEquals(list3, sorted[0])
    assertEquals(list2, sorted[1])
    assertEquals(list1, sorted[2])
  }

  @Test
  fun `uiState formattedDate should format using yyyy-MM-dd`() {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(2026, java.util.Calendar.MAY, 25)
    val state = StartErrandUiState(selectedDate = calendar.time)
    assertEquals("2026-05-25", state.formattedDate)
  }

  @Test
  fun `onStoreSelected should update selectedStoreId`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(105)
      runCurrent()
      assertEquals(105, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `onPartnerListSelected should update selectedPartnerListId`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onPartnerListSelected(205)
      runCurrent()
      assertEquals(205, viewModel.uiState.value.selectedPartnerListId)
      uiStateJob.cancel()
    }

  @Test
  fun `onDateSelected should update selectedDate`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      val testDate = java.util.Date()
      viewModel.onDateSelected(testDate)
      runCurrent()
      assertEquals(testDate, viewModel.uiState.value.selectedDate)
      uiStateJob.cancel()
    }

  @Test
  fun `startRound success resets receipts, fetches details, triggers precaching, and runs onSuccess`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      viewModel.onStoreSelected(101)
      viewModel.onPartnerListSelected(202)

      val mockRound = mock<com.zephyr.boreal.domain.model.Round>()
      whenever(roundsRepository.startRound(any())).thenReturn(ApiResource.Success(mockRound))

      val mockStoreDetails = mock<com.zephyr.boreal.domain.model.StoreDetails>()
      whenever(storesRepository.getStoreDetails(eq(101), any())).thenReturn(ApiResource.Success(mockStoreDetails))

      whenever(itemsRepository.getItems()).thenReturn(flowOf(ApiResource.Success(emptyList())))
      whenever(itemsRepository.getOtherItems()).thenReturn(flowOf(ApiResource.Success(emptyList())))
      whenever(partnersRepository.getPartners()).thenReturn(flowOf(ApiResource.Success(emptyList())))
      whenever(itemsRepository.getPriceLists(any())).thenReturn(flowOf(ApiResource.Success(emptyList())))

      var isSuccessCalled = false
      viewModel.startRound {
        isSuccessCalled = true
      }

      advanceUntilIdle()

      org.mockito.kotlin.argumentCaptor<com.zephyr.boreal.api.dto.request.StartRoundRequestDataDto>().apply {
        verify(roundsRepository).startRound(capture())
        assertEquals(101, firstValue.storeId)
        assertEquals(202, firstValue.partnerListId)
      }

      verify(receiptsStore).resetReceipts()
      verify(storesRepository).getStoreDetails(eq(101), any())
      verify(storeSessionStore).setStore(mockStoreDetails)

      verify(itemsRepository).getItems()
      verify(itemsRepository).getOtherItems()
      verify(partnersRepository).getPartners()
      verify(itemsRepository).getPriceLists(any())

      assertFalse(viewModel.uiState.value.isStartingRound)
      assertTrue(isSuccessCalled)

      uiStateJob.cancel()
    }

  @Test
  fun `startRound failure sets isStartingRound false and exposes error message`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mock())
      storesFlow.value = ApiResource.Success(emptyList())
      partnerListsFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      viewModel.onStoreSelected(101)
      viewModel.onPartnerListSelected(202)

      whenever(roundsRepository.startRound(any())).thenReturn(ApiResource.Error("Network error starting round"))

      var isSuccessCalled = false
      viewModel.startRound {
        isSuccessCalled = true
      }

      advanceUntilIdle()

      verify(roundsRepository).startRound(any())

      assertFalse(viewModel.uiState.value.isStartingRound)
      assertEquals("Network error starting round", viewModel.uiState.value.errorMessage)
      assertFalse(isSuccessCalled)

      uiStateJob.cancel()
    }
}
