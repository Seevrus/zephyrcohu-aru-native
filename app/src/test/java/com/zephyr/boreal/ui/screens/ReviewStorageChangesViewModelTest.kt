package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.api.dto.request.ExpirationChangeDto
import com.zephyr.boreal.api.dto.request.SaveSelectedItemsRequestDto
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

private fun item(
  itemId: Int,
  expirationId: Int,
  name: String,
  originalQuantity: Double? = 5.0,
  quantityChange: Int? = null,
) = StorageListItem(
  itemId = itemId,
  expirationId = expirationId,
  articleNumber = "ART$itemId",
  name = name,
  expiresAt = "260101",
  itemBarcode = "",
  expirationBarcode = "",
  unitName = "db",
  primaryStoreQuantity = 100.0,
  originalQuantity = originalQuantity,
  quantityChange = quantityChange,
)

private fun storeDetails() =
  StoreDetails(
    id = 1,
    name = "Kamion",
    code = "K1",
    type = "S",
    state = "L",
    createdAt = "",
    updatedAt = "",
    expirations = emptyList(),
  )

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewStorageChangesViewModelTest {
  private val itemsRepository: ItemsRepository = mock()
  private val storageSessionStore: StorageSessionStore = mock()
  private val connectivityObserver: ConnectivityObserver = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val storageListItemsFlow = MutableStateFlow<List<StorageListItem>?>(null)
  private val primaryStoreDetailsFlow = MutableStateFlow<StoreDetails?>(storeDetails())
  private val isInternetReachableFlow = MutableStateFlow(true)

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(storageSessionStore.storageListItems).thenReturn(storageListItemsFlow)
    whenever(storageSessionStore.primaryStoreDetails).thenReturn(primaryStoreDetailsFlow)
    whenever(connectivityObserver.isInternetReachable).thenReturn(isInternetReachableFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
    ReviewStorageChangesViewModel(itemsRepository, storageSessionStore, connectivityObserver)

  @Test
  fun `changedItems filters out unchanged rows and sorts by name`() =
    runTest {
      storageListItemsFlow.value =
        listOf(
          item(1, 11, "Zebra", quantityChange = 2),
          item(2, 12, "Alpha", quantityChange = -1),
          item(3, 13, "Unchanged", quantityChange = null),
        )

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals(
        listOf("Alpha", "Zebra"),
        viewModel.uiState.value.changedItems
          .map { it.name },
      )
      uiStateJob.cancel()
    }

  @Test
  fun `up and down aggregate positive and negative changes separately`() =
    runTest {
      storageListItemsFlow.value =
        listOf(
          item(1, 11, "A", quantityChange = 5),
          item(2, 12, "B", quantityChange = 3),
          item(3, 13, "C", quantityChange = -2),
          item(4, 14, "D", quantityChange = -7),
        )

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertEquals(8, viewModel.uiState.value.up)
      assertEquals(9, viewModel.uiState.value.down)
      uiStateJob.cancel()
    }

  @Test
  fun `reallyUnexpectedBlocker is true when there is no primary store`() =
    runTest {
      primaryStoreDetailsFlow.value = null

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.reallyUnexpectedBlocker)
      assertFalse(viewModel.uiState.value.canConfirm)
      uiStateJob.cancel()
    }

  @Test
  fun `requestConfirm only opens the alert when canConfirm is true`() =
    runTest {
      isInternetReachableFlow.value = false

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.requestConfirm()
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.showConfirmAlert)
      uiStateJob.cancel()
    }

  @Test
  fun `confirmSend maps changed items to ExpirationChangeDto and saves`() =
    runTest {
      storageListItemsFlow.value =
        listOf(item(1, 11, "A", originalQuantity = 5.0, quantityChange = 3))
      whenever(itemsRepository.saveSelectedItems(org.mockito.kotlin.any())).thenReturn(ApiResource.Success(Unit))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onSavedCalled = false
      viewModel.confirmSend { onSavedCalled = true }
      advanceUntilIdle()

      val captor = argumentCaptor<SaveSelectedItemsRequestDto>()
      verify(itemsRepository).saveSelectedItems(captor.capture())
      assertEquals(
        listOf(
          ExpirationChangeDto(expirationId = 11, startingQuantity = 5.0, quantityChange = 3.0, finalQuantity = 8.0),
        ),
        captor.firstValue.data.changes,
      )
      verify(storageSessionStore).setIsStorageSavedToApi(true)
      assertTrue(onSavedCalled)
      assertFalse(viewModel.uiState.value.isSaving)
      uiStateJob.cancel()
    }

  @Test
  fun `confirmSend with zero changes skips the network call but still succeeds`() =
    runTest {
      storageListItemsFlow.value = listOf(item(1, 11, "A", quantityChange = null))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onSavedCalled = false
      viewModel.confirmSend { onSavedCalled = true }
      advanceUntilIdle()

      verifyNoInteractions(itemsRepository)
      verify(storageSessionStore).setIsStorageSavedToApi(true)
      assertTrue(onSavedCalled)
      uiStateJob.cancel()
    }

  @Test
  fun `confirmSend surfaces an error and does not navigate when the save fails`() =
    runTest {
      storageListItemsFlow.value = listOf(item(1, 11, "A", quantityChange = 1))
      whenever(itemsRepository.saveSelectedItems(org.mockito.kotlin.any())).thenReturn(ApiResource.Error("offline"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onSavedCalled = false
      viewModel.confirmSend { onSavedCalled = true }
      advanceUntilIdle()

      assertFalse(onSavedCalled)
      assertTrue(viewModel.uiState.value.isError)
      assertFalse(viewModel.uiState.value.isSaving)
      verify(storageSessionStore, org.mockito.kotlin.never()).setIsStorageSavedToApi(org.mockito.kotlin.any())
      uiStateJob.cancel()
    }
}
