package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.model.TempSelection
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SelectOtherItemsViewModelTest {
  private val itemsRepository: ItemsRepository = mock()
  private val receiptsStore: ReceiptsStore = mock()

  private val otherItemsFlow = MutableStateFlow<ApiResource<List<OtherItem>>>(ApiResource.Loading())
  private val currentReceiptFlow = MutableStateFlow<DraftReceipt?>(null)
  private val otherItemSelectionsFlow = MutableStateFlow<Map<Int, TempSelection>>(emptyMap())

  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(itemsRepository.getOtherItems()).thenReturn(otherItemsFlow)
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
    whenever(receiptsStore.otherItemSelections).thenReturn(otherItemSelectionsFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun buildOtherItem(
    id: Int = 1,
    name: String = "Item $id",
    netPrice: Double = 1000.0,
    vatRate: String = "27",
  ) = OtherItem(
    id = id,
    articleNumber = "ART-$id",
    name = name,
    shortName = "I$id",
    unitName = "db",
    vatRate = vatRate,
    netPrice = netPrice,
    createdAt = "2025-01-01",
    updatedAt = "2025-01-01",
  )

  private fun buildReceiptOtherItem(
    id: Int = 1,
    netPrice: Double = 1000.0,
    quantity: Double = 2.0,
    comment: String? = null,
  ) = ReceiptOtherItem(
    id = id,
    articleNumber = "ART-$id",
    name = "Item $id",
    quantity = quantity,
    unitName = "db",
    netPrice = netPrice,
    netAmount = netPrice * quantity,
    vatRate = "27",
    vatAmount = 0.0,
    grossAmount = 0.0,
    comment = comment,
  )

  private fun createViewModel() = SelectOtherItemsViewModel(itemsRepository, receiptsStore)

  @Test
  fun `initially shows loading state`() =
    runTest {
      val vm = createViewModel()
      runCurrent()

      assertTrue(vm.uiState.value.isLoading)
    }

  @Test
  fun `items are sorted alphabetically`() =
    runTest {
      otherItemsFlow.value =
        ApiResource.Success(
          listOf(
            buildOtherItem(id = 1, name = "Zebra"),
            buildOtherItem(id = 2, name = "Apple"),
            buildOtherItem(id = 3, name = "Mango"),
          ),
        )
      val vm = createViewModel()
      runCurrent()

      assertEquals(
        listOf("Apple", "Mango", "Zebra"),
        vm.uiState.value.items
          .map { it.name },
      )
    }

  @Test
  fun `search filters items by name case-insensitively`() =
    runTest {
      otherItemsFlow.value =
        ApiResource.Success(
          listOf(
            buildOtherItem(id = 1, name = "Apple"),
            buildOtherItem(id = 2, name = "Mango"),
            buildOtherItem(id = 3, name = "Lemon"),
          ),
        )
      val vm = createViewModel()
      runCurrent()

      vm.onSearchQueryChanged("a")
      runCurrent()

      // "Apple" and "Mango" both contain "a" (case-insensitive); "Lemon" does not
      assertEquals(
        listOf("Apple", "Mango"),
        vm.uiState.value.items
          .map { it.name },
      )
    }

  @Test
  fun `canAccept is false when no item has quantity`() =
    runTest {
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))
      val vm = createViewModel()
      runCurrent()

      assertFalse(vm.uiState.value.canAccept)
    }

  @Test
  fun `canAccept is true when at least one item has quantity`() =
    runTest {
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 2)
      runCurrent()

      assertTrue(vm.uiState.value.canAccept)
    }

  @Test
  fun `totals are computed correctly from selections`() =
    runTest {
      // netPrice=1000, vatRate="27", qty=3 → net=3000, vat=round(3000*0.27)=810, gross=3810
      val item = buildOtherItem(id = 1, netPrice = 1000.0, vatRate = "27")
      otherItemsFlow.value = ApiResource.Success(listOf(item))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 3)
      runCurrent()

      assertEquals(3000.0, vm.uiState.value.netTotal)
      assertEquals(3810.0, vm.uiState.value.grossTotal)
    }

  @Test
  fun `totals use overridden net price when set`() =
    runTest {
      val item = buildOtherItem(id = 1, netPrice = 1000.0, vatRate = "27")
      otherItemsFlow.value = ApiResource.Success(listOf(item))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 1)
      vm.onNetPriceChanged(1, 800.0)
      runCurrent()

      // net=800, vat=round(800*0.27)=216, gross=1016
      assertEquals(800.0, vm.uiState.value.netTotal)
      assertEquals(1016.0, vm.uiState.value.grossTotal)
    }

  @Test
  fun `pre-populates selections from existing receipt other items`() =
    runTest {
      val receiptItem = buildReceiptOtherItem(id = 1, netPrice = 800.0, quantity = 2.0, comment = "Note")
      currentReceiptFlow.value = DraftReceipt(otherItems = listOf(receiptItem))
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))

      val vm = createViewModel()
      runCurrent()

      val selection = vm.uiState.value.selections[1]
      assertNotNull(selection)
      assertEquals(800.0, selection!!.netPrice)
      assertEquals(2, selection.quantity)
      assertEquals("Note", selection.comment)
    }

  @Test
  fun `init prefers persisted otherItemSelections over currentReceipt otherItems when both are present`() =
    runTest {
      val receiptItem = buildReceiptOtherItem(id = 1, netPrice = 800.0, quantity = 2.0, comment = "From receipt")
      currentReceiptFlow.value = DraftReceipt(otherItems = listOf(receiptItem))
      otherItemSelectionsFlow.value =
        mapOf(1 to TempSelection(netPrice = 950.0, quantity = 5, comment = "From persisted draft"))
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))

      val vm = createViewModel()
      runCurrent()

      val selection = vm.uiState.value.selections[1]
      assertNotNull(selection)
      assertEquals(950.0, selection!!.netPrice)
      assertEquals(5, selection.quantity)
      assertEquals("From persisted draft", selection.comment)
    }

  @Test
  fun `onQuantityChanged writes selections through to receiptsStore`() =
    runTest {
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 3)
      runCurrent()

      val captor = argumentCaptor<Map<Int, TempSelection>>()
      verify(receiptsStore).setOtherItemSelections(captor.capture())
      assertEquals(3, captor.firstValue[1]?.quantity)
    }

  @Test
  fun `onCommentChanged writes selections through to receiptsStore`() =
    runTest {
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))
      val vm = createViewModel()
      runCurrent()

      vm.onCommentChanged(1, "Test comment")
      runCurrent()

      val captor = argumentCaptor<Map<Int, TempSelection>>()
      verify(receiptsStore).setOtherItemSelections(captor.capture())
      assertEquals("Test comment", captor.firstValue[1]?.comment)
    }

  @Test
  fun `onToggleExpanded adds then removes item id`() =
    runTest {
      val vm = createViewModel()
      runCurrent()

      vm.onToggleExpanded(5)
      runCurrent()
      assertTrue(
        vm.uiState.value.expandedItemIds
          .contains(5),
      )

      vm.onToggleExpanded(5)
      runCurrent()
      assertFalse(
        vm.uiState.value.expandedItemIds
          .contains(5),
      )
    }

  @Test
  fun `confirmHandler builds correct ReceiptOtherItems and updates receipt`() =
    runTest {
      val item = buildOtherItem(id = 1, netPrice = 1000.0, vatRate = "27")
      otherItemsFlow.value = ApiResource.Success(listOf(item))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 2)
      vm.onNetPriceChanged(1, 900.0)
      vm.onCommentChanged(1, "Test comment")
      runCurrent()

      val onSuccess: () -> Unit = mock()
      vm.confirmHandler(onSuccess)

      val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
      verify(receiptsStore).updateCurrentReceipt(captor.capture())

      val result = captor.firstValue(DraftReceipt())
      assertEquals(1, result.otherItems.size)
      val resultItem = result.otherItems[0]
      assertEquals(1, resultItem.id)
      assertEquals(2.0, resultItem.quantity)
      assertEquals(900.0, resultItem.netPrice)
      assertEquals(1800.0, resultItem.netAmount)
      // vat = round(1800 * 0.27) = 486
      assertEquals(486.0, resultItem.vatAmount)
      assertEquals(2286.0, resultItem.grossAmount)
      assertEquals("Test comment", resultItem.comment)

      verify(onSuccess).invoke()
    }

  @Test
  fun `confirmHandler uses catalog price when netPrice override is null`() =
    runTest {
      val item = buildOtherItem(id = 1, netPrice = 1000.0, vatRate = "27")
      otherItemsFlow.value = ApiResource.Success(listOf(item))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 1)
      runCurrent()

      val onSuccess: () -> Unit = mock()
      vm.confirmHandler(onSuccess)

      val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
      verify(receiptsStore).updateCurrentReceipt(captor.capture())

      val result = captor.firstValue(DraftReceipt())
      assertEquals(1000.0, result.otherItems[0].netPrice)
    }

  @Test
  fun `confirmHandler excludes items with null or zero quantity`() =
    runTest {
      val items =
        listOf(
          buildOtherItem(id = 1),
          buildOtherItem(id = 2),
        )
      otherItemsFlow.value = ApiResource.Success(items)
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 3) // item 1 selected
      // item 2 left with no quantity
      runCurrent()

      val onSuccess: () -> Unit = mock()
      vm.confirmHandler(onSuccess)

      val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
      verify(receiptsStore).updateCurrentReceipt(captor.capture())

      val result = captor.firstValue(DraftReceipt())
      assertEquals(1, result.otherItems.size)
      assertEquals(1, result.otherItems[0].id)
    }

  @Test
  fun `confirmHandler stores blank comment as null`() =
    runTest {
      otherItemsFlow.value = ApiResource.Success(listOf(buildOtherItem(id = 1)))
      val vm = createViewModel()
      runCurrent()

      vm.onQuantityChanged(1, 1)
      vm.onCommentChanged(1, "   ")
      runCurrent()

      val onSuccess: () -> Unit = mock()
      vm.confirmHandler(onSuccess)

      val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
      verify(receiptsStore).updateCurrentReceipt(captor.capture())

      val result = captor.firstValue(DraftReceipt())
      assertFalse(result.otherItems.isEmpty())
      // blank comment should be stored as null, not as whitespace
      // (onCommentChanged converts blank to null, confirmHandler passes it through)
      assertEquals(null, result.otherItems[0].comment)
    }
}
