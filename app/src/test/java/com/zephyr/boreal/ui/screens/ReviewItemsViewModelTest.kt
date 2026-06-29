package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewItemsViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val currentReceiptFlow = MutableStateFlow<DraftReceipt?>(null)
  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun buildItem(
    id: Int = 1,
    expirationId: Int = 1,
    grossAmount: Double = 1000.0,
  ) = ReceiptItem(
    id = id,
    articleNumber = "ART-$id",
    name = "Item $id",
    quantity = 2.0,
    unitName = "db",
    netPrice = 500.0,
    netAmount = 1000.0,
    vatRate = "27%",
    vatAmount = 270.0,
    grossAmount = grossAmount,
    discountName = null,
    expirationId = expirationId,
    cnCode = "CN001",
    expiresAt = "2026-12-31",
  )

  private fun buildOtherItem(
    id: Int = 10,
    grossAmount: Double = 500.0,
  ) = ReceiptOtherItem(
    id = id,
    articleNumber = "OTH-$id",
    name = "Other Item $id",
    quantity = 1.0,
    unitName = "db",
    netPrice = grossAmount / 1.27,
    netAmount = grossAmount / 1.27,
    vatRate = "27",
    vatAmount = grossAmount - (grossAmount / 1.27),
    grossAmount = grossAmount,
    comment = null,
  )

  @Test
  fun `initially should populate items and grossTotal from store`() =
    runTest {
      val item1 = buildItem(id = 1, grossAmount = 1000.0)
      val item2 = buildItem(id = 2, expirationId = 2, grossAmount = 2000.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(item1, item2))

      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      assertEquals(listOf(item1, item2), viewModel.uiState.value.items)
      assertEquals(3000.0, viewModel.uiState.value.grossTotal)
      assertEquals(emptyList<ReceiptOtherItem>(), viewModel.uiState.value.otherItems)
    }

  @Test
  fun `onToggleExpanded adds key when not yet expanded`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.onToggleExpanded("1_1")

      assertTrue(
        viewModel.uiState.value.expandedItemKeys
          .contains("1_1"),
      )
    }

  @Test
  fun `onToggleExpanded removes key when already expanded`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.onToggleExpanded("1_1")
      viewModel.onToggleExpanded("1_1")

      assertFalse(
        viewModel.uiState.value.expandedItemKeys
          .contains("1_1"),
      )
    }

  @Test
  fun `showCancelDialog should set showCancelConfirmation to true`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)

      viewModel.showCancelDialog()

      assertTrue(viewModel.uiState.value.showCancelConfirmation)
    }

  @Test
  fun `dismissCancelDialog should set showCancelConfirmation to false`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)
      viewModel.showCancelDialog()

      viewModel.dismissCancelDialog()

      assertFalse(viewModel.uiState.value.showCancelConfirmation)
    }

  @Test
  fun `removeItem calls upsertSelectedItem and updateCurrentReceipt`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(id = 1), buildItem(id = 2, expirationId = 2)))
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = {})

      verify(receiptsStore).upsertSelectedItem(1, 1, null)
      verify(receiptsStore).updateCurrentReceipt(any())
    }

  @Test
  fun `removeItem navigates home when receipt becomes empty`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = emptyList())
      val onNavigateHome: () -> Unit = mock()
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = onNavigateHome)

      verify(receiptsStore).resetReceipts()
      verify(onNavigateHome).invoke()
    }

  @Test
  fun `removeItem does not navigate when items remain`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(id = 2, expirationId = 2)))
      val onNavigateHome: () -> Unit = mock()
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.removeItem(id = 1, expirationId = 1, onNavigateHome = onNavigateHome)

      verifyNoInteractions(onNavigateHome)
    }

  @Test
  fun `cancelReceipt calls resetReceipts and navigates home`() =
    runTest {
      val onNavigateHome: () -> Unit = mock()
      val viewModel = ReviewItemsViewModel(receiptsStore)

      viewModel.cancelReceipt(onNavigateHome)

      verify(receiptsStore).resetReceipts()
      verify(onNavigateHome).invoke()
    }

  @Test
  fun `grossTotal includes other items gross amounts`() =
    runTest {
      val item = buildItem(id = 1, grossAmount = 1000.0)
      val otherItem = buildOtherItem(id = 10, grossAmount = 500.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(item), otherItems = listOf(otherItem))

      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      assertEquals(1500.0, viewModel.uiState.value.grossTotal)
    }

  @Test
  fun `otherItems are populated from store`() =
    runTest {
      val otherItem = buildOtherItem(id = 10, grossAmount = 500.0)
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(otherItem))

      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      assertEquals(listOf(otherItem), viewModel.uiState.value.otherItems)
    }

  @Test
  fun `onToggleOtherItemExpanded adds id when not expanded`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.onToggleOtherItemExpanded(10)

      assertTrue(
        viewModel.uiState.value.expandedOtherItemIds
          .contains(10),
      )
    }

  @Test
  fun `onToggleOtherItemExpanded removes id when already expanded`() =
    runTest {
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.onToggleOtherItemExpanded(10)
      viewModel.onToggleOtherItemExpanded(10)

      assertFalse(
        viewModel.uiState.value.expandedOtherItemIds
          .contains(10),
      )
    }

  @Test
  fun `removeOtherItem calls updateCurrentReceipt`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(buildOtherItem(id = 10)))
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.removeOtherItem(10)

      verify(receiptsStore).updateCurrentReceipt(any())
    }

  @Test
  fun `removeOtherItem does not reset flow even when other items become empty`() =
    runTest {
      currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()), otherItems = listOf(buildOtherItem(id = 10)))
      val onNavigateHome: () -> Unit = mock()
      val viewModel = ReviewItemsViewModel(receiptsStore)
      runCurrent()

      viewModel.removeOtherItem(10)

      verifyNoInteractions(onNavigateHome)
      verify(receiptsStore, never()).resetReceipts()
    }
}
