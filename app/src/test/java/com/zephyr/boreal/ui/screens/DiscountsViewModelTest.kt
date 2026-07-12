package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.zephyr.boreal.domain.model.Discount
import com.zephyr.boreal.domain.model.DiscountType
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.SelectedDiscount
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.domain.utils.DiscountCalculator
import com.zephyr.boreal.store.receipts.ReceiptsStore
import kotlinx.coroutines.flow.MutableStateFlow
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

class DiscountsViewModelTest {
  private val receiptsStore: ReceiptsStore = mock()
  private val currentReceiptFlow = MutableStateFlow<DraftReceipt?>(null)

  private val absoluteDiscount =
    Discount(
      id = 1,
      name = "Fix kedvezmény",
      type = DiscountType.ABSOLUTE,
      amount = 200.0,
      createdAt = "",
      updatedAt = "",
    )
  private val percentageDiscount =
    Discount(
      id = 2,
      name = "Százalékos kedvezmény",
      type = DiscountType.PERCENTAGE,
      amount = 20.0,
      createdAt = "",
      updatedAt = "",
    )
  private val freeFormDiscount =
    Discount(
      id = 3,
      name = "Egyedi kedvezmény",
      type = DiscountType.FREE_FORM,
      amount = 0.0,
      createdAt = "",
      updatedAt = "",
    )

  @BeforeEach
  fun setUp() {
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
  }

  private fun buildItem(
    id: Int = 1,
    expirationId: Int = 1,
    quantity: Double = 5.0,
    netPrice: Double = 1000.0,
    availableDiscounts: List<Discount> = listOf(absoluteDiscount, percentageDiscount, freeFormDiscount),
    selectedDiscounts: List<SelectedDiscount> = emptyList(),
  ) = DraftReceiptItem(
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
    discountName = null,
    expirationId = expirationId,
    cnCode = "CN001",
    expiresAt = "202607",
    availableDiscounts = availableDiscounts,
    selectedDiscounts = selectedDiscounts,
  )

  private fun createViewModel(
    itemId: Int = 1,
    expirationId: Int = 1,
  ): DiscountsViewModel {
    val handle = SavedStateHandle(mapOf("itemId" to itemId, "expirationId" to expirationId))
    return DiscountsViewModel(handle, receiptsStore)
  }

  @Test
  fun `prefills blank fields and default freeForm price when no discount is selected`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(netPrice = 1000.0)))

    val viewModel = createViewModel()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Item 1", state.name)
    assertEquals("", state.absoluteQuantityText)
    assertEquals("", state.percentageQuantityText)
    assertEquals("", state.freeFormQuantityText)
    assertEquals("1000", state.freeFormPriceText)
  }

  @Test
  fun `prefills quantities and price from an existing selection`() {
    val selected =
      listOf(
        SelectedDiscount(1, "Fix kedvezmény", DiscountType.ABSOLUTE, quantity = 2.0, amount = 200.0),
        SelectedDiscount(2, "Százalékos kedvezmény", DiscountType.PERCENTAGE, quantity = 1.0, amount = 20.0),
        SelectedDiscount(3, "Egyedi kedvezmény", DiscountType.FREE_FORM, quantity = 1.0, price = 50.0),
      )
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(selectedDiscounts = selected)))

    val viewModel = createViewModel()

    val state = viewModel.uiState.value
    assertEquals("2", state.absoluteQuantityText)
    assertEquals("1", state.percentageQuantityText)
    assertEquals("1", state.freeFormQuantityText)
    assertEquals("50", state.freeFormPriceText)
  }

  @Test
  fun `sets isLoading false and blank fields when the item is not found`() {
    currentReceiptFlow.value = DraftReceipt(items = emptyList())

    val viewModel = createViewModel(itemId = 99, expirationId = 99)

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("", state.name)
  }

  @Test
  fun `applyDiscounts rejects non-numeric input without touching the store`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("abc")

    val onSuccess: () -> Unit = mock()
    viewModel.applyDiscounts(onSuccess)

    assertEquals("Csak számok adhatóak meg.", viewModel.uiState.value.formErrorMessage)
    verify(receiptsStore, never()).updateCurrentReceipt(any())
    verifyNoInteractions(onSuccess)
  }

  @Test
  fun `applyDiscounts rejects a total discounted quantity larger than the line quantity`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(quantity = 3.0)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("2")
    viewModel.onPercentageQuantityChanged("2")

    viewModel.applyDiscounts(mock())

    val state = viewModel.uiState.value
    assertEquals("Túl nagy megadott mennyiség.", state.formErrorMessage)
    assertTrue(state.absoluteQuantityError)
    assertTrue(state.percentageQuantityError)
    assertTrue(state.freeFormQuantityError)
    verify(receiptsStore, never()).updateCurrentReceipt(any())
  }

  @Test
  fun `applyDiscounts rejects a negative freeForm price when freeForm quantity is used`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()))
    val viewModel = createViewModel()
    viewModel.onFreeFormQuantityChanged("1")
    viewModel.onFreeFormPriceChanged("-50")

    viewModel.applyDiscounts(mock())

    val state = viewModel.uiState.value
    assertEquals("Az új árat pozitív számként lehetséges megadni.", state.formErrorMessage)
    assertTrue(state.freeFormPriceError)
    assertFalse(state.absoluteQuantityError)
  }

  @Test
  fun `applyDiscounts reports both errors together when they co-occur`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(quantity = 1.0)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("1")
    viewModel.onFreeFormQuantityChanged("1")
    viewModel.onFreeFormPriceChanged("-50")

    viewModel.applyDiscounts(mock())

    val message = viewModel.uiState.value.formErrorMessage
    requireNotNull(message)
    assertTrue(message.contains("Túl nagy megadott mennyiség."))
    assertTrue(message.contains("Az új árat pozitív számként lehetséges megadni."))
  }

  @Test
  fun `applyDiscounts with all-zero quantities clears selection and reverts amounts`() {
    val selected = listOf(SelectedDiscount(1, "Fix kedvezmény", DiscountType.ABSOLUTE, quantity = 2.0, amount = 200.0))
    currentReceiptFlow.value =
      DraftReceipt(items = listOf(buildItem(quantity = 5.0, netPrice = 1000.0, selectedDiscounts = selected)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("0")

    val onSuccess: () -> Unit = mock()
    viewModel.applyDiscounts(onSuccess)

    val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
    verify(receiptsStore).updateCurrentReceipt(captor.capture())
    val result = captor.firstValue(currentReceiptFlow.value!!).items.first()

    assertTrue(result.selectedDiscounts.isEmpty())
    val expected = AmountCalculator.calculateAmounts(1000.0, 5.0, "27")
    assertEquals(expected.netAmount, result.netAmount)
    assertEquals(expected.vatAmount, result.vatAmount)
    assertEquals(expected.grossAmount, result.grossAmount)
    verify(onSuccess).invoke()
  }

  @Test
  fun `applyDiscounts with a single absolute discount writes the correct selection and amounts`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(quantity = 5.0, netPrice = 1000.0)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("2")

    viewModel.applyDiscounts(mock())

    val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
    verify(receiptsStore).updateCurrentReceipt(captor.capture())
    val result = captor.firstValue(currentReceiptFlow.value!!).items.first()

    assertEquals(1, result.selectedDiscounts.size)
    val discount = result.selectedDiscounts.first()
    assertEquals(1, discount.id)
    assertEquals(DiscountType.ABSOLUTE, discount.type)
    assertEquals(2.0, discount.quantity)
    assertEquals(200.0, discount.amount)

    // 2 units at (1000-200)=800, 3 units at 1000, vatRate 27
    // discounted: net=1600, vat=round(1600*.27)=432, gross=2032
    // remainder: net=3000, vat=810, gross=3810
    assertEquals(4600.0, result.netAmount)
    assertEquals(1242.0, result.vatAmount)
    assertEquals(5842.0, result.grossAmount)
  }

  @Test
  fun `applyDiscounts with all three discount types combines their amounts with the remainder`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(quantity = 5.0, netPrice = 1000.0)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("1")
    viewModel.onPercentageQuantityChanged("1")
    viewModel.onFreeFormQuantityChanged("1")
    viewModel.onFreeFormPriceChanged("100")

    viewModel.applyDiscounts(mock())

    val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
    verify(receiptsStore).updateCurrentReceipt(captor.capture())
    val result = captor.firstValue(currentReceiptFlow.value!!).items.first()

    assertEquals(3, result.selectedDiscounts.size)
    val expected =
      DiscountCalculator.calculateDiscountedLineAmounts(
        originalNetPrice = 1000.0,
        quantity = 5.0,
        vatRate = "27",
        selectedDiscounts = result.selectedDiscounts,
      )
    assertEquals(expected.netAmount, result.netAmount)
    assertEquals(expected.vatAmount, result.vatAmount)
    assertEquals(expected.grossAmount, result.grossAmount)
  }

  @Test
  fun `applyDiscounts does not invoke onSuccess when validation fails`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem()))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("abc")

    val onSuccess: () -> Unit = mock()
    viewModel.applyDiscounts(onSuccess)

    verifyNoInteractions(onSuccess)
  }

  @Test
  fun `applyDiscounts leaves discountName untouched since API row splitting owns it`() {
    currentReceiptFlow.value = DraftReceipt(items = listOf(buildItem(quantity = 5.0, netPrice = 1000.0)))
    val viewModel = createViewModel()
    viewModel.onAbsoluteQuantityChanged("2")

    viewModel.applyDiscounts(mock())

    val captor = argumentCaptor<(DraftReceipt) -> DraftReceipt>()
    verify(receiptsStore).updateCurrentReceipt(captor.capture())
    val result = captor.firstValue(currentReceiptFlow.value!!).items.first()

    assertNull(result.discountName)
  }
}
