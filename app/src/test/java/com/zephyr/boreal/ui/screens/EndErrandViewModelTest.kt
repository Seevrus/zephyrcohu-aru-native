package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.R
import com.zephyr.boreal.api.dto.request.FinishRoundRequestDataDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.domain.model.Store
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EndErrandViewModelTest {
  private val roundsRepository: RoundsRepository = mock()
  private val userRepository: UserRepository = mock()
  private val storesRepository: StoresRepository = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val isOnlineFlow = MutableStateFlow(true)
  private val currentUserFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Loading())
  private val storesFlow = MutableStateFlow<ApiResource<List<Store>>>(ApiResource.Loading())
  private val receiptsFlow = MutableStateFlow<List<Receipt>>(emptyList())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isOnlineFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
    whenever(storesRepository.getStores(any())).thenReturn(storesFlow)
    whenever(storesRepository.getStores()).thenReturn(storesFlow)
    whenever(receiptsStore.receipts).thenReturn(receiptsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    EndErrandViewModel(
      roundsRepository,
      userRepository,
      storesRepository,
      receiptsStore,
      connectivityObserver,
    )

  @Test
  fun `checkActiveRound initializes correctly when user has active round and store is found`() =
    runTest {
      val mockRound = mock<Round>()
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(101)
      storesFlow.value = ApiResource.Success(listOf(mockStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }

      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.isUserPending)
      assertTrue(state.canFinishRound)
      assertNull(state.disableReasonResId)

      uiStateJob.cancel()
    }

  @Test
  fun `checkActiveRound disables finish when user has no store in use`() =
    runTest {
      val mockRound = mock<Round>()
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(null)
      currentUserFlow.value = ApiResource.Success(mockUser)
      storesFlow.value = ApiResource.Success(emptyList())

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }

      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.canFinishRound)
      assertEquals(R.string.end_errand_error_no_store, state.disableReasonResId)

      uiStateJob.cancel()
    }

  @Test
  fun `checkActiveRound disables finish when active store is not found`() =
    runTest {
      val mockRound = mock<Round>()
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(999) // Different store
      storesFlow.value = ApiResource.Success(listOf(mockStore))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }

      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertFalse(state.canFinishRound)
      assertEquals(R.string.end_errand_error_store_not_found, state.disableReasonResId)

      uiStateJob.cancel()
    }

  @Test
  fun `finishRound success calls refreshCurrentUser and invokes callback`() =
    runTest {
      val mockRound = mock<Round>()
      whenever(mockRound.id).thenReturn(55)
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(101)
      whenever(mockStore.firstAvailableSerialNumber).thenReturn(10)
      whenever(mockStore.yearCode).thenReturn(2026)
      storesFlow.value = ApiResource.Success(listOf(mockStore))

      // Setup receipts flow
      receiptsFlow.value = emptyList()

      whenever(roundsRepository.finishRound(any())).thenReturn(ApiResource.Success(mock()))
      whenever(userRepository.refreshCurrentUser()).thenReturn(ApiResource.Success(mock()))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }
      advanceUntilIdle()

      var callbackInvoked = false
      viewModel.finishRound { callbackInvoked = true }
      advanceUntilIdle()

      // Verify lastSerialNumber logic when empty: max(0, firstAvailableSerialNumber - 1)
      argumentCaptor<FinishRoundRequestDataDto>().apply {
        verify(roundsRepository).finishRound(capture())
        assertEquals(55, firstValue.roundId)
        assertEquals(9, firstValue.lastSerialNumber)
        assertEquals(2026, firstValue.yearCode)
        assertTrue(firstValue.receipts.isEmpty())
      }

      verify(userRepository).refreshCurrentUser()
      assertTrue(callbackInvoked)
      assertFalse(viewModel.uiState.value.isLoading)

      uiStateJob.cancel()
    }

  @Suppress("LongMethod")
  @Test
  fun `finishRound calculates lastSerialNumber correctly with non-empty receipts`() =
    runTest {
      val mockRound = mock<Round>()
      whenever(mockRound.id).thenReturn(55)
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(101)
      whenever(mockStore.firstAvailableSerialNumber).thenReturn(10)
      storesFlow.value = ApiResource.Success(listOf(mockStore))
      val receipt1 =
        Receipt(
          id = 1,
          companyId = 1,
          companyCode = "CC",
          partnerId = 2,
          partnerCode = "PC",
          partnerSiteCode = "PSC",
          serialNumber = 12,
          yearCode = 2026,
          cancelSerialNumber = null,
          cancelYearCode = null,
          vendor =
            com.zephyr.boreal.domain.model
              .ReceiptVendor("V", "HU", "1", "Budapest", "A", "F", "I", "B", "VAT"),
          invoiceDate = "2026-05-30",
          fulfillmentDate = "2026-05-30",
          invoiceType = com.zephyr.boreal.domain.model.InvoiceType.PAPER,
          paidDate = "2026-05-30",
          user = null,
          buyer =
            com.zephyr.boreal.domain.model.ReceiptBuyer(
              1,
              "Test Buyer",
              "HU",
              "1000",
              "Budapest",
              "A",
              "DN",
              "DC",
              "DPC",
              "DC",
              "DA",
              null,
              null,
              null,
            ),
          quantity = 5.0,
          netAmount = 100.0,
          vatAmount = 27.0,
          grossAmount = 127.0,
          roundAmount = 0.0,
          roundedAmount = 127.0,
          lastDownloadedAt = null,
          createdAt = "",
          updatedAt = "",
        )

      val receipt2 = receipt1.copy(id = 2, serialNumber = 15, cancelSerialNumber = 18)

      receiptsFlow.value = listOf(receipt1, receipt2)

      whenever(roundsRepository.finishRound(any())).thenReturn(ApiResource.Success(mock()))
      whenever(userRepository.refreshCurrentUser()).thenReturn(ApiResource.Success(mock()))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }
      advanceUntilIdle()

      viewModel.finishRound {}
      advanceUntilIdle()

      argumentCaptor<FinishRoundRequestDataDto>().apply {
        verify(roundsRepository).finishRound(capture())
        // Max between 12, 15, and 18 (cancelSerialNumber)
        assertEquals(18, firstValue.lastSerialNumber)
      }

      uiStateJob.cancel()
    }

  @Test
  fun `finishRound failure shows default error message for non-dev`() =
    runTest {
      val mockRound = mock<Round>()
      whenever(mockRound.id).thenReturn(55)
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      whenever(mockUser.isDev).thenReturn(false)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(101)
      whenever(mockStore.firstAvailableSerialNumber).thenReturn(10)
      storesFlow.value = ApiResource.Success(listOf(mockStore))

      whenever(roundsRepository.finishRound(any())).thenReturn(ApiResource.Error("API error message"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }
      advanceUntilIdle()

      var callbackInvoked = false
      viewModel.finishRound { callbackInvoked = true }
      advanceUntilIdle()

      assertFalse(callbackInvoked)
      assertEquals("Kör zárása sikertelen.", viewModel.uiState.value.errorMessage)
      assertFalse(viewModel.uiState.value.isLoading)

      uiStateJob.cancel()
    }

  @Test
  fun `finishRound failure shows detailed error message for dev`() =
    runTest {
      val mockRound = mock<Round>()
      whenever(mockRound.id).thenReturn(55)
      val mockUser = mock<User>()
      whenever(mockUser.lastRound).thenReturn(mockRound)
      whenever(mockUser.storeInUseId).thenReturn(101)
      whenever(mockUser.isDev).thenReturn(true)
      currentUserFlow.value = ApiResource.Success(mockUser)

      val mockStore = mock<Store>()
      whenever(mockStore.id).thenReturn(101)
      whenever(mockStore.firstAvailableSerialNumber).thenReturn(10)
      storesFlow.value = ApiResource.Success(listOf(mockStore))

      whenever(roundsRepository.finishRound(any())).thenReturn(ApiResource.Error("API error message"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect { } }
      advanceUntilIdle()

      viewModel.finishRound {}
      advanceUntilIdle()

      assertEquals("API error message", viewModel.uiState.value.errorMessage)

      uiStateJob.cancel()
    }
}
