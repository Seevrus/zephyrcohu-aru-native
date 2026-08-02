package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Expiration
import com.zephyr.boreal.domain.model.Item
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

private fun item(
  id: Int,
  articleNumber: String = "ART$id",
  name: String = "Item $id",
  barcode: String? = "ITEMBC$id",
  expirations: List<Expiration> = emptyList(),
): Item =
  Item(
    id = id,
    articleNumber = articleNumber,
    name = name,
    shortName = name,
    unitName = "db",
    vatRate = "AJT",
    netPrice = 100.0,
    cnCode = "",
    barcode = barcode,
    productCatalogCode = "",
    expirations = expirations,
    createdAt = "",
    updatedAt = "",
  )

private fun expiration(
  id: Int,
  expiresAt: String = "260101 00:00:00",
  barcode: String? = "EXPBC$id",
): Expiration = Expiration(id = id, barcode = barcode, expiresAt = expiresAt, createdAt = "", updatedAt = "")

private fun storeDetails(expirations: List<StoreDetailsExpiration>): StoreDetails =
  StoreDetails(
    id = 1,
    name = "Store",
    code = "S",
    type = "SECONDARY",
    state = "IDLE",
    createdAt = "",
    updatedAt = "",
    expirations = expirations,
  )

private fun storeExpiration(
  itemId: Int,
  expirationId: Int,
  quantity: Double,
): StoreDetailsExpiration =
  StoreDetailsExpiration(
    itemId = itemId,
    articleNumber = "ART$itemId",
    expirationId = expirationId,
    expiresAt = "260101",
    quantity = quantity,
  )

@OptIn(ExperimentalCoroutinesApi::class)
class SelectItemsFromStoreViewModelTest {
  private val itemsRepository: ItemsRepository = mock()
  private val storesRepository: StoresRepository = mock()
  private val storageSessionStore: StorageSessionStore = mock()
  private val userRepository: UserRepository = mock()

  private val testDispatcher = StandardTestDispatcher()

  private val itemsFlow = MutableStateFlow<ApiResource<List<Item>>>(ApiResource.Loading())
  private val storageListItemsFlow = MutableStateFlow<List<StorageListItem>?>(null)
  private val primaryStoreDetailsFlow = MutableStateFlow<StoreDetails?>(null)
  private val selectedStoreInitialStateFlow = MutableStateFlow<StoreDetails?>(null)

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(itemsRepository.getItems()).thenReturn(itemsFlow)
    whenever(storageSessionStore.storageListItems).thenReturn(storageListItemsFlow)
    whenever(storageSessionStore.primaryStoreDetails).thenReturn(primaryStoreDetailsFlow)
    whenever(storageSessionStore.selectedStoreInitialState).thenReturn(selectedStoreInitialStateFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): SelectItemsFromStoreViewModel =
    SelectItemsFromStoreViewModel(itemsRepository, storesRepository, storageSessionStore, userRepository)

  @Test
  fun `builds the working list from items, primary and selected-initial stock`() =
    runTest {
      val exp1 = expiration(id = 11)
      val exp2 = expiration(id = 12)
      primaryStoreDetailsFlow.value =
        storeDetails(listOf(storeExpiration(itemId = 1, expirationId = 11, quantity = 50.0)))
      selectedStoreInitialStateFlow.value =
        storeDetails(listOf(storeExpiration(itemId = 1, expirationId = 11, quantity = 3.0)))
      itemsFlow.value = ApiResource.Success(listOf(item(id = 1, expirations = listOf(exp1, exp2))))

      createViewModel()
      advanceUntilIdle()

      val captor = argumentCaptor<List<StorageListItem>>()
      verify(storageSessionStore).setStorageListItems(captor.capture())

      val built = captor.firstValue
      assertEquals(2, built.size)

      val row1 = built.first { it.expirationId == 11 }
      assertEquals(50.0, row1.primaryStoreQuantity)
      assertEquals(3.0, row1.originalQuantity)
      assertNull(row1.quantityChange)

      val row2 = built.first { it.expirationId == 12 }
      assertNull(row2.primaryStoreQuantity)
      assertNull(row2.originalQuantity)
    }

  @Test
  fun `resumes an existing session working list without rebuilding`() =
    runTest {
      storageListItemsFlow.value =
        listOf(
          StorageListItem(
            itemId = 1,
            expirationId = 11,
            articleNumber = "ART1",
            name = "Item 1",
            expiresAt = "260101",
            itemBarcode = "",
            expirationBarcode = "",
            unitName = "db",
            primaryStoreQuantity = 5.0,
            originalQuantity = 2.0,
            quantityChange = 1,
          ),
        )

      createViewModel()
      advanceUntilIdle()

      verifyNoInteractions(itemsRepository)
      verify(storageSessionStore, never()).setStorageListItems(any())
    }

  @Test
  fun `displayItems filters by name, expiresAt, item barcode and expiration barcode`() {
    val items =
      listOf(
        StorageListItem(1, 11, "A1", "Alpha", "260101", "ITEMBC1", "EXPBC1", "db", null, null),
        StorageListItem(2, 12, "A2", "Beta", "260202", "ITEMBC2", "EXPBC2", "db", null, null),
        StorageListItem(3, 13, "A3", "Gamma", "260303", "ITEMBC3", "EXPBC3", "db", null, null),
      )

    assertEquals(
      listOf("Alpha"),
      SelectItemsFromStoreUiState(items = items, searchQuery = "alp").displayItems.map { it.name },
    )
    assertEquals(
      listOf("Beta"),
      SelectItemsFromStoreUiState(items = items, searchQuery = "2602").displayItems.map { it.name },
    )
    assertEquals(
      listOf("Gamma"),
      SelectItemsFromStoreUiState(items = items, searchQuery = "ITEMBC3").displayItems.map { it.name },
    )
    assertEquals(
      listOf("Alpha"),
      SelectItemsFromStoreUiState(items = items, searchQuery = "EXPBC1").displayItems.map { it.name },
    )
    assertEquals(3, SelectItemsFromStoreUiState(items = items, searchQuery = "").displayItems.size)
  }

  @Test
  fun `isAnyItemChanged is true once any row has a quantityChange`() {
    val unchanged = StorageListItem(1, 11, "A1", "Alpha", "260101", "", "", "db", null, null)
    val changed = unchanged.copy(itemId = 2, expirationId = 12, quantityChange = 3)

    assertFalse(SelectItemsFromStoreUiState(items = listOf(unchanged)).isAnyItemChanged)
    assertTrue(SelectItemsFromStoreUiState(items = listOf(unchanged, changed)).isAnyItemChanged)
  }

  @Test
  fun `resolveQuantityChange applies RN bounds and clearing rules`() {
    // normal increment / decrement, comma-decimal parsing, flooring
    assertEquals(3, resolveQuantityChange("3", originalQuantity = 10.0))
    assertEquals(3, resolveQuantityChange("3,7", originalQuantity = 10.0))
    assertEquals(-3, resolveQuantityChange("-2,5", originalQuantity = 10.0))

    // empty / zero / NaN clear the change
    assertNull(resolveQuantityChange("", originalQuantity = 10.0))
    assertNull(resolveQuantityChange("0", originalQuantity = 10.0))
    assertNull(resolveQuantityChange("abc", originalQuantity = 10.0))

    // -1-at-zero-original shortcut
    assertNull(resolveQuantityChange("-1", originalQuantity = 0.0))
    assertEquals(-1, resolveQuantityChange("-1", originalQuantity = 5.0))

    // lower-bound clamping
    assertEquals(-10, resolveQuantityChange("-25", originalQuantity = 10.0))
    assertEquals(-10, resolveQuantityChange("-10", originalQuantity = 10.0))

    // no upper bound against primaryStoreQuantity (not a parameter at all)
    assertEquals(1000, resolveQuantityChange("1000", originalQuantity = 10.0))
  }

  @Test
  fun `requestAbandon with changes shows confirmation and does not unlock yet`() =
    runTest {
      storageListItemsFlow.value =
        listOf(StorageListItem(1, 11, "A1", "Alpha", "260101", "", "", "db", null, null, quantityChange = 1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onAbandonedCalled = false
      viewModel.requestAbandon { onAbandonedCalled = true }
      advanceUntilIdle()

      assertTrue(viewModel.uiState.value.showAbandonConfirm)
      verifyNoInteractions(storesRepository)
      assertFalse(onAbandonedCalled)
      uiStateJob.cancel()
    }

  @Test
  fun `requestAbandon without changes unlocks and navigates immediately`() =
    runTest {
      storageListItemsFlow.value =
        listOf(StorageListItem(1, 11, "A1", "Alpha", "260101", "", "", "db", null, null))
      whenever(storesRepository.deselectStore()).thenReturn(ApiResource.Success(Unit))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onAbandonedCalled = false
      viewModel.requestAbandon { onAbandonedCalled = true }
      advanceUntilIdle()

      verify(storesRepository).deselectStore()
      verify(userRepository).refreshCurrentUser()
      verify(storageSessionStore).clearStores()
      assertTrue(onAbandonedCalled)
      assertFalse(viewModel.uiState.value.showAbandonConfirm)
      uiStateJob.cancel()
    }

  @Test
  fun `confirmAbandon unlocks, clears the session and navigates even if unlock fails`() =
    runTest {
      storageListItemsFlow.value =
        listOf(StorageListItem(1, 11, "A1", "Alpha", "260101", "", "", "db", null, null, quantityChange = 1))
      whenever(storesRepository.deselectStore()).thenReturn(ApiResource.Error("offline"))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      var onAbandonedCalled = false
      viewModel.confirmAbandon { onAbandonedCalled = true }
      advanceUntilIdle()

      verify(storesRepository).deselectStore()
      verify(userRepository).refreshCurrentUser()
      verify(storageSessionStore).clearStores()
      assertTrue(onAbandonedCalled)
      assertFalse(viewModel.uiState.value.showAbandonConfirm)
      uiStateJob.cancel()
    }

  @Test
  fun `dismissAbandonConfirm hides the confirmation without unlocking`() =
    runTest {
      storageListItemsFlow.value =
        listOf(StorageListItem(1, 11, "A1", "Alpha", "260101", "", "", "db", null, null, quantityChange = 1))

      val viewModel = createViewModel()
      val uiStateJob = backgroundScope.launch { viewModel.uiState.collect() }
      advanceUntilIdle()

      viewModel.requestAbandon {}
      advanceUntilIdle()
      assertTrue(viewModel.uiState.value.showAbandonConfirm)

      viewModel.dismissAbandonConfirm()
      advanceUntilIdle()

      assertFalse(viewModel.uiState.value.showAbandonConfirm)
      uiStateJob.cancel()
      verifyNoInteractions(storesRepository)
    }
}
