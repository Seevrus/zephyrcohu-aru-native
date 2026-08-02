package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.StoreType
import com.zephyr.boreal.domain.model.StoreUser
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserRole
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.store.StorageSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SelectStoreViewModelTest {
  private val storesRepository: StoresRepository = mock()
  private val userRepository: UserRepository = mock()
  private val storageSessionStore: StorageSessionStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val isOnlineFlow = MutableStateFlow(true)
  private val currentUserFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Loading())
  private val storesFlow = MutableStateFlow<ApiResource<List<Store>>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isOnlineFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
    whenever(storesRepository.getStores(any())).thenReturn(storesFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): SelectStoreViewModel =
    SelectStoreViewModel(
      storesRepository,
      userRepository,
      storageSessionStore,
      connectivityObserver,
    )

  private fun mockUser(
    id: Int,
    roles: List<UserRole> = emptyList(),
  ): User =
    mock<User>().also {
      whenever(it.id).thenReturn(id)
      whenever(it.roles).thenReturn(roles)
    }

  private fun mockStore(
    id: Int,
    name: String = "Store $id",
    type: StoreType = StoreType.SECONDARY,
    state: UserState = UserState.IDLE,
    ownerId: Int? = null,
  ): Store =
    mock<Store>().also { store ->
      val owner = ownerId?.let { id -> mock<StoreUser>().also { whenever(it.id).thenReturn(id) } }
      whenever(store.id).thenReturn(id)
      whenever(store.name).thenReturn(name)
      whenever(store.type).thenReturn(type)
      whenever(store.state).thenReturn(state)
      whenever(store.owner).thenReturn(owner)
    }

  @Test
  fun `loadInitialData fetches user and force-refetches stores`() =
    runTest {
      val mockUser = mockUser(id = 42)
      currentUserFlow.value = ApiResource.Success(mockUser)
      storesFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      advanceUntilIdle()

      verify(userRepository).getCurrentUser()
      verify(storesRepository).getStores(eq(true))

      val state = viewModel.uiState.value
      assertEquals(mockUser, state.currentUser)
      assertTrue(state.stores.isEmpty())
      assertFalse(state.isLoading)
      assertNull(state.errorMessage)
    }

  @Test
  fun `loadInitialData handling failures`() =
    runTest {
      currentUserFlow.value = ApiResource.Error("User fetch failed")
      storesFlow.value = ApiResource.Error("Store fetch failed")

      val viewModel = createViewModel()
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertTrue(state.errorMessage != null)
      assertFalse(state.isLoading)
    }

  @Test
  fun `connectivity changes should update UI state isInternetReachable`() =
    runTest {
      isOnlineFlow.value = true
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      storesFlow.value = ApiResource.Success(emptyList())

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
  fun `displayStores filters by type, state, ownership and canLoadAnyStore`() {
    val mockUser = mockUser(id = 42)

    val ownedStore = mockStore(id = 1, name = "Store B", ownerId = 42)
    val unownedStore = mockStore(id = 2, name = "Store A", ownerId = null)
    val primaryStore = mockStore(id = 3, type = StoreType.PRIMARY, ownerId = null)
    val onRoundStore = mockStore(id = 4, state = UserState.ON_ROUND, ownerId = null)
    val otherUsersStore = mockStore(id = 5, ownerId = 99)

    val state =
      SelectStoreUiState(
        currentUser = mockUser,
        stores = listOf(ownedStore, unownedStore, primaryStore, onRoundStore, otherUsersStore),
      )

    val displayStores = state.displayStores
    assertEquals(2, displayStores.size)
    assertEquals(unownedStore, displayStores[0])
    assertEquals(ownedStore, displayStores[1])
  }

  @Test
  fun `displayStores includes stores owned by others when user canLoadAnyStore`() {
    val mockUser = mockUser(id = 42, roles = listOf(UserRole.LOAD_ANY))
    val otherUsersStore = mockStore(id = 5, ownerId = 99)

    val state =
      SelectStoreUiState(
        currentUser = mockUser,
        stores = listOf(otherUsersStore),
      )

    assertEquals(listOf(otherUsersStore), state.displayStores)
  }

  @Test
  fun `updateDefaultStore should auto-select an eligible store owned by current user`() =
    runTest {
      val mockUser = mockUser(id = 42)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val ownedStore = mockStore(id = 101, ownerId = 42)
      val otherStore = mockStore(id = 102, ownerId = null)
      storesFlow.value = ApiResource.Success(listOf(otherStore, ownedStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals(101, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `updateDefaultStore should not overwrite manually selected store`() =
    runTest {
      val mockUser = mockUser(id = 42)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val ownedStore = mockStore(id = 101, ownerId = 42)
      storesFlow.value = ApiResource.Success(listOf(ownedStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }

      viewModel.onStoreSelected(999)
      advanceUntilIdle()

      assertEquals(999, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `onStoreSelected should update selectedStoreId`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      storesFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(105)
      runCurrent()
      assertEquals(105, viewModel.uiState.value.selectedStoreId)
      uiStateJob.cancel()
    }

  @Test
  fun `canSubmit requires a selection and not currently submitting`() {
    assertFalse(SelectStoreUiState(selectedStoreId = null, isSubmitting = false).canSubmit)
    assertFalse(SelectStoreUiState(selectedStoreId = 1, isSubmitting = true).canSubmit)
    assertTrue(SelectStoreUiState(selectedStoreId = 1, isSubmitting = false).canSubmit)
  }

  @Test
  fun `submit success locks store, fetches both details, stores session, and runs onSuccess`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      val primaryStore = mockStore(id = 1, type = StoreType.PRIMARY)
      storesFlow.value = ApiResource.Success(listOf(primaryStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(101)

      whenever(storesRepository.selectStore(eq(101))).thenReturn(ApiResource.Success(mock()))
      val primaryDetails = mock<StoreDetails>()
      val selectedDetails = mock<StoreDetails>()
      whenever(storesRepository.getStoreDetails(eq(1), any())).thenReturn(ApiResource.Success(primaryDetails))
      whenever(storesRepository.getStoreDetails(eq(101), any())).thenReturn(ApiResource.Success(selectedDetails))

      var isSuccessCalled = false
      viewModel.submit { isSuccessCalled = true }

      advanceUntilIdle()

      verify(storesRepository).selectStore(eq(101))
      verify(storesRepository).getStoreDetails(eq(1), any())
      verify(storesRepository).getStoreDetails(eq(101), any())
      verify(storageSessionStore).setStores(primaryDetails, selectedDetails)

      assertFalse(viewModel.uiState.value.isSubmitting)
      assertNull(viewModel.uiState.value.errorMessage)
      assertTrue(isSuccessCalled)

      uiStateJob.cancel()
    }

  @Test
  fun `submit failure at lock stage sets errorMessage and does not fetch details`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      val primaryStore = mockStore(id = 1, type = StoreType.PRIMARY)
      storesFlow.value = ApiResource.Success(listOf(primaryStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(101)

      whenever(storesRepository.selectStore(eq(101))).thenReturn(ApiResource.Error("Store is already locked."))

      var isSuccessCalled = false
      viewModel.submit { isSuccessCalled = true }

      advanceUntilIdle()

      verify(storesRepository).selectStore(eq(101))
      verifyNoInteractions(storageSessionStore)

      assertFalse(viewModel.uiState.value.isSubmitting)
      assertEquals("Store is already locked.", viewModel.uiState.value.errorMessage)
      assertFalse(isSuccessCalled)

      uiStateJob.cancel()
    }

  @Test
  fun `submit failure fetching store details sets errorMessage and does not store session`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      val primaryStore = mockStore(id = 1, type = StoreType.PRIMARY)
      storesFlow.value = ApiResource.Success(listOf(primaryStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(101)

      whenever(storesRepository.selectStore(eq(101))).thenReturn(ApiResource.Success(mock()))
      whenever(storesRepository.getStoreDetails(eq(1), any()))
        .thenReturn(ApiResource.Error("Primary store details fetch failed"))
      whenever(storesRepository.getStoreDetails(eq(101), any()))
        .thenReturn(ApiResource.Success(mock()))

      var isSuccessCalled = false
      viewModel.submit { isSuccessCalled = true }

      advanceUntilIdle()

      verifyNoInteractions(storageSessionStore)
      assertFalse(viewModel.uiState.value.isSubmitting)
      assertEquals("Primary store details fetch failed", viewModel.uiState.value.errorMessage)
      assertFalse(isSuccessCalled)

      uiStateJob.cancel()
    }

  @Test
  fun `submit does nothing when no primary store is present`() =
    runTest {
      currentUserFlow.value = ApiResource.Success(mockUser(id = 1))
      storesFlow.value = ApiResource.Success(listOf(mockStore(id = 101, ownerId = 1)))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.onStoreSelected(101)

      var isSuccessCalled = false
      viewModel.submit { isSuccessCalled = true }
      advanceUntilIdle()

      verifyNoInteractions(storageSessionStore)
      assertFalse(isSuccessCalled)
      assertTrue(viewModel.uiState.value.errorMessage != null)

      uiStateJob.cancel()
    }
}
