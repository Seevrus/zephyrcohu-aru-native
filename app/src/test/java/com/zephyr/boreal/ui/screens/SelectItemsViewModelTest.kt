package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Item
import com.zephyr.boreal.domain.model.StoreDetails
import com.zephyr.boreal.domain.model.StoreDetailsExpiration
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SelectItemsViewModelTest {
  private val itemsRepository: ItemsRepository = mock()
  private val storeSessionStore: StoreSessionStore = mock()
  private val receiptsStore: ReceiptsStore = mock()
  private val userRepository: UserRepository = mock()
  private val storesRepository: StoresRepository = mock()

  private val testDispatcher = UnconfinedTestDispatcher()

  private val itemsFlow = MutableStateFlow<ApiResource<List<Item>>>(ApiResource.Loading())
  private val selectedItemsFlow = MutableStateFlow<Map<Int, Map<Int, Double>>>(emptyMap())
  private val selectedOrderItemsFlow = MutableStateFlow<Map<Int, Double>>(emptyMap())
  private val currentReceiptFlow = MutableStateFlow<com.zephyr.boreal.domain.model.DraftReceipt?>(null)
  private val storeStateFlow = MutableStateFlow<StoreDetails?>(null)
  private val currentUserFlow = MutableStateFlow<ApiResource<User?>>(ApiResource.Loading())

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(itemsRepository.getItems()).thenReturn(itemsFlow)
    whenever(receiptsStore.selectedItems).thenReturn(selectedItemsFlow)
    whenever(receiptsStore.selectedOrderItems).thenReturn(selectedOrderItemsFlow)
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
    whenever(storeSessionStore.selectedStoreCurrentState).thenReturn(storeStateFlow)
    whenever(userRepository.getCurrentUser()).thenReturn(currentUserFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): SelectItemsViewModel =
    SelectItemsViewModel(
      itemsRepository,
      storeSessionStore,
      receiptsStore,
      userRepository,
      storesRepository,
    )

  @Test
  fun `quantity mapping should use partial articleNumber and varied date formats as fallback`() =
    runTest {
      val item1 = mock<Item>()
      whenever(item1.id).thenReturn(1)
      whenever(item1.name).thenReturn("Item 1")
      whenever(item1.articleNumber).thenReturn("2881304210") // Catalog has NO prefix
      whenever(item1.unitName).thenReturn("kg")
      whenever(item1.vatRate).thenReturn("27")
      whenever(item1.cnCode).thenReturn("1234")

      val exp1 = mock<com.zephyr.boreal.domain.model.Expiration>()
      whenever(exp1.id).thenReturn(10)
      whenever(exp1.expiresAt).thenReturn("2803") // Catalog has short date
      whenever(item1.expirations).thenReturn(listOf(exp1))

      itemsFlow.value = ApiResource.Success(listOf(item1))

      val storeDetails = mock<StoreDetails>()
      val storeExp1 =
        StoreDetailsExpiration(
          itemId = 999,
          articleNumber = "ART-2881304210", // Inventory HAS prefix
          expirationId = 888,
          expiresAt = "202803", // Inventory HAS long date
          quantity = 16.0,
        )
      whenever(storeDetails.expirations).thenReturn(listOf(storeExp1))
      storeStateFlow.value = storeDetails

      val viewModel = createViewModel()
      val job = backgroundScope.launch { viewModel.uiState.collect {} }
      advanceUntilIdle()

      val sellItem =
        viewModel.uiState.value.items
          .first()
      val sellExp = sellItem.expirations.first()
      assertEquals(16.0, sellExp.quantity)
      job.cancel()
    }

  @Test
  fun `search query should filter items correctly`() =
    runTest {
      val item1 = mock<Item>()
      whenever(item1.id).thenReturn(1)
      whenever(item1.name).thenReturn("Apple")
      whenever(item1.articleNumber).thenReturn("A1")
      whenever(item1.unitName).thenReturn("kg")
      whenever(item1.vatRate).thenReturn("27")
      whenever(item1.cnCode).thenReturn("1234")
      whenever(item1.netPrice).thenReturn(100.0)
      whenever(item1.expirations).thenReturn(emptyList())
      whenever(item1.barcode).thenReturn(null)

      val item2 = mock<Item>()
      whenever(item2.id).thenReturn(2)
      whenever(item2.name).thenReturn("Banana")
      whenever(item2.articleNumber).thenReturn("B1")
      whenever(item2.unitName).thenReturn("kg")
      whenever(item2.vatRate).thenReturn("27")
      whenever(item2.cnCode).thenReturn("1234")
      whenever(item2.netPrice).thenReturn(100.0)
      whenever(item2.expirations).thenReturn(emptyList())
      whenever(item2.barcode).thenReturn(null)

      itemsFlow.value = ApiResource.Success(listOf(item1, item2))

      val viewModel = createViewModel()
      val job = backgroundScope.launch { viewModel.uiState.collect {} }

      viewModel.onSearchQueryChanged("app")
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertEquals(1, state.items.size)
      assertEquals("Apple", state.items.first().name)
      assertEquals("app", state.searchQuery)
      assertEquals("", state.barcodeQuery)

      job.cancel()
    }

  @Test
  fun `upsert should call receiptsStore`() {
    val viewModel = createViewModel()
    viewModel.upsertSelectedItem(1, 10, 5.0)
    verify(receiptsStore).upsertSelectedItem(1, 10, 5.0)

    viewModel.upsertOrderItem(2, 3.0)
    verify(receiptsStore).upsertOrderItem(2, 3.0)
  }

  @Test
  fun `confirmItemsHandler should update draft receipt and trigger success`() =
    runTest {
      val item1 = mock<Item>()
      whenever(item1.id).thenReturn(1)
      whenever(item1.name).thenReturn("Item 1")
      whenever(item1.articleNumber).thenReturn("ART-1")
      whenever(item1.unitName).thenReturn("kg")
      whenever(item1.vatRate).thenReturn("27")
      whenever(item1.netPrice).thenReturn(100.0)
      whenever(item1.cnCode).thenReturn("1234")
      val exp1 = mock<com.zephyr.boreal.domain.model.Expiration>()
      whenever(exp1.id).thenReturn(10)
      whenever(exp1.expiresAt).thenReturn("2026-03")
      whenever(item1.expirations).thenReturn(listOf(exp1))

      itemsFlow.value = ApiResource.Success(listOf(item1))

      val draftReceipt =
        com.zephyr.boreal.domain.model
          .DraftReceipt(partnerId = 99)
      currentReceiptFlow.value = draftReceipt
      selectedItemsFlow.value = mapOf(1 to mapOf(10 to 2.0))

      val viewModel = createViewModel()
      val job = backgroundScope.launch { viewModel.uiState.collect {} }
      advanceUntilIdle()

      var successCalled = false
      viewModel.confirmItemsHandler {
        successCalled = true
      }

      assertTrue(successCalled)
      verify(receiptsStore).updateCurrentReceipt(any())
      job.cancel()
    }
}
