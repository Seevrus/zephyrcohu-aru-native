# Other Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full other-items sell flow in the native Android app — a dedicated selection screen launched from the Review Screen, accordion display of selected other items in Review, and correct combined totals and reset behaviour throughout.

**Architecture:** A new `SelectOtherItemsScreen` + `SelectOtherItemsViewModel` is added as a Compose Navigation destination reachable from the Review Screen's "Extra tételek" button. The ViewModel holds temporary selection state (`Map<Int, TempSelection>`) pre-seeded from `DraftReceipt.otherItems` on entry; pressing Accept commits the list back to the receipt, pressing Back discards it. The Review Screen's ViewModel and UI are extended to display and allow removal of committed other items, and the footer gross total includes both regular and other items.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, `kotlinx.coroutines` (Flow, StateFlow, combine), Mockito-Kotlin + JUnit 5, `AmountCalculator` for VAT math, existing `ItemsRepository.getOtherItems()` + `ReceiptsStore`.

## Global Constraints

- 2-space indentation throughout Kotlin source
- Every Kotlin change must pass `./gradlew ktlintFormat` and `./gradlew detekt` before the task is declared done
- Run tests with `./gradlew test --tests "<fully.qualified.TestClass>"` (not `php artisan`)
- All new `@Composable` functions that emit UI must have a `Modifier` parameter
- `@Suppress("MagicNumber")` where numeric literals appear in Compose layout code
- Exact package: `com.zephyr.boreal.ui.screens` for ViewModels/Screens, `com.zephyr.boreal.ui.components` for components
- Test file mirrors source path under `app/src/test/java/`

---

## File Map

| Action | Path |
|--------|------|
| **Create** | `app/src/main/java/com/zephyr/boreal/ui/components/QuantityStepper.kt` |
| **Modify** | `app/src/main/java/com/zephyr/boreal/ui/screens/SelectItemsScreen.kt` |
| **Create** | `app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModel.kt` |
| **Create** | `app/src/test/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModelTest.kt` |
| **Create** | `app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsScreen.kt` |
| **Modify** | `app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModel.kt` |
| **Modify** | `app/src/test/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModelTest.kt` |
| **Modify** | `app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsScreen.kt` |
| **Modify** | `app/src/main/java/com/zephyr/boreal/MainActivity.kt` |
| **Modify** | `app/src/main/res/values/strings.xml` |

---

## Task 1: Extract QuantityStepper component

**Files:**
- Create: `app/src/main/java/com/zephyr/boreal/ui/components/QuantityStepper.kt`
- Modify: `app/src/main/java/com/zephyr/boreal/ui/screens/SelectItemsScreen.kt`

**Interfaces:**
- Produces: `QuantityStepper(label: String, quantity: Double?, maxQuantity: Double?, onQuantityChange: (Double?) -> Unit, modifier: Modifier = Modifier)` — consumed by Tasks 3 and the updated Task 1 call site in `SelectItemsScreen`

- [ ] **Step 1: Create `QuantityStepper.kt`**

```kotlin
package com.zephyr.boreal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.theme.BorealColors

@Suppress("MagicNumber")
@Composable
fun QuantityStepper(
  label: String,
  quantity: Double?,
  maxQuantity: Double?,
  onQuantityChange: (Double?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentQty = quantity ?: 0.0

  val handleIncrease = {
    val next = currentQty + 1.0
    if (maxQuantity == null || next <= maxQuantity) {
      onQuantityChange(next)
    } else {
      onQuantityChange(maxQuantity)
    }
  }

  val handleDecrease = {
    val next = currentQty - 1.0
    if (next <= 0) {
      onQuantityChange(null)
    } else {
      onQuantityChange(next)
    }
  }

  val handleTextChange = { text: String ->
    val cleanText = text.trim().replace(",", ".")
    val num = cleanText.toDoubleOrNull()
    if (num == null || num <= 0) {
      onQuantityChange(null)
    } else if (maxQuantity != null && num > maxQuantity) {
      onQuantityChange(maxQuantity)
    } else {
      onQuantityChange(num)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      color = Color.White,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      IconButton(onClick = handleDecrease) {
        Icon(
          painter = painterResource(id = R.drawable.remove_circle),
          contentDescription = stringResource(R.string.select_items_decrease),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      TextField(
        value =
          if (quantity != null) {
            (if (quantity % 1 == 0.0) quantity.toInt().toString() else quantity.toString())
          } else {
            ""
          },
        onValueChange = handleTextChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(140.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = BorealColors.Input,
            unfocusedContainerColor = BorealColors.Input,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        singleLine = true,
      )

      Spacer(modifier = Modifier.width(16.dp))

      IconButton(onClick = handleIncrease) {
        Icon(
          painter = painterResource(id = R.drawable.add_circle),
          contentDescription = stringResource(R.string.select_items_increase),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }
    }
  }
}
```

- [ ] **Step 2: Update `SelectItemsScreen.kt` — replace private `SelectionRow` with `QuantityStepper`**

In `SelectItemsScreen.kt`:
1. Delete the entire private `SelectionRow` composable (the function definition starting at `@Suppress("LongMethod", "MagicNumber")` on the line before `private fun SelectionRow`).
2. Add import: `import com.zephyr.boreal.ui.components.QuantityStepper`
3. Replace both `SelectionRow(` call sites with `QuantityStepper(` — the parameter names and types are identical, so only the function name changes.

The two call sites look like this after the change:
```kotlin
// First call site (inside items.expirations.forEach):
QuantityStepper(
  label = "${exp.expiresAt.take(6)} (${exp.quantity.toInt()})",
  quantity = currentQty,
  maxQuantity = exp.quantity,
  onQuantityChange = { onUpsertSelectedItem(exp.expirationId, it) },
)

// Second call site (order row):
QuantityStepper(
  label = stringResource(R.string.select_items_order_row),
  quantity = orderQuantity,
  maxQuantity = null,
  onQuantityChange = { onUpsertOrderItem(it) },
)
```

- [ ] **Step 3: Verify compilation and run ktlint**

```bash
cd d:/zephyrcohu-aru-native
./gradlew :app:compileDebugKotlin
./gradlew ktlintFormat
```

Expected: BUILD SUCCESSFUL with no errors. If ktlint reformats anything, check the diff is cosmetic only.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/ui/components/QuantityStepper.kt \
        app/src/main/java/com/zephyr/boreal/ui/screens/SelectItemsScreen.kt
git commit -m "refactor: extract QuantityStepper into shared component"
```

---

## Task 2: SelectOtherItemsViewModel + tests

**Files:**
- Create: `app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModel.kt`
- Create: `app/src/test/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModelTest.kt`

**Interfaces:**
- Consumes: `ItemsRepository.getOtherItems(): Flow<ApiResource<List<OtherItem>>>`, `ReceiptsStore.currentReceipt: StateFlow<DraftReceipt?>`, `ReceiptsStore.updateCurrentReceipt((DraftReceipt) -> DraftReceipt)`, `AmountCalculator.calculateAmounts(netPrice, quantity, vatRate): AmountCalculationResult`, `AmountCalculator.calculateGrossAmount(netAmount, vatRate): Double`
- Produces:
  - `data class TempSelection(val netPrice: Double?, val quantity: Int?, val comment: String?)`
  - `data class SelectOtherItemsUiState(val isLoading: Boolean, val searchQuery: String, val items: List<OtherItem>, val selections: Map<Int, TempSelection>, val netTotal: Double, val grossTotal: Double, val canAccept: Boolean, val expandedItemIds: Set<Int>)`
  - `class SelectOtherItemsViewModel @Inject constructor(private val itemsRepository: ItemsRepository, private val receiptsStore: ReceiptsStore)` with methods: `onSearchQueryChanged(String)`, `onToggleExpanded(Int)`, `onQuantityChanged(Int, Int?)`, `onNetPriceChanged(Int, Double?)`, `onCommentChanged(Int, String?)`, `confirmHandler(() -> Unit)`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModelTest.kt`:

```kotlin
package com.zephyr.boreal.ui.screens

import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.OtherItem
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    whenever(itemsRepository.getOtherItems()).thenReturn(otherItemsFlow)
    whenever(receiptsStore.currentReceipt).thenReturn(currentReceiptFlow)
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

      assertEquals(listOf("Apple", "Mango", "Zebra"), vm.uiState.value.items.map { it.name })
    }

  @Test
  fun `search filters items by name case-insensitively`() =
    runTest {
      otherItemsFlow.value =
        ApiResource.Success(
          listOf(
            buildOtherItem(id = 1, name = "Apple"),
            buildOtherItem(id = 2, name = "Mango"),
            buildOtherItem(id = 3, name = "Zebra"),
          ),
        )
      val vm = createViewModel()
      runCurrent()

      vm.onSearchQueryChanged("a")
      runCurrent()

      // "Apple" and "Mango" both contain "a" (case-insensitive)
      assertEquals(listOf("Apple", "Mango"), vm.uiState.value.items.map { it.name })
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
  fun `onToggleExpanded adds then removes item id`() =
    runTest {
      val vm = createViewModel()
      runCurrent()

      vm.onToggleExpanded(5)
      runCurrent()
      assertTrue(vm.uiState.value.expandedItemIds.contains(5))

      vm.onToggleExpanded(5)
      runCurrent()
      assertFalse(vm.uiState.value.expandedItemIds.contains(5))
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
```

- [ ] **Step 2: Run tests to confirm they fail (class doesn't exist yet)**

```bash
cd d:/zephyrcohu-aru-native
./gradlew test --tests "com.zephyr.boreal.ui.screens.SelectOtherItemsViewModelTest"
```

Expected: BUILD FAILED — `error: unresolved reference: SelectOtherItemsViewModel`

- [ ] **Step 3: Create `SelectOtherItemsViewModel.kt`**

```kotlin
package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TempSelection(
  val netPrice: Double? = null,
  val quantity: Int? = null,
  val comment: String? = null,
)

data class SelectOtherItemsUiState(
  val isLoading: Boolean = true,
  val searchQuery: String = "",
  val items: List<OtherItem> = emptyList(),
  val selections: Map<Int, TempSelection> = emptyMap(),
  val netTotal: Double = 0.0,
  val grossTotal: Double = 0.0,
  val canAccept: Boolean = false,
  val expandedItemIds: Set<Int> = emptySet(),
)

@HiltViewModel
class SelectOtherItemsViewModel
  @Inject
  constructor(
    private val itemsRepository: ItemsRepository,
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val catalogFlow = MutableStateFlow(com.zephyr.boreal.data.repository.ApiResource.Loading<List<OtherItem>>())
    private val tempSelectionsFlow = MutableStateFlow<Map<Int, TempSelection>>(emptyMap())
    private val searchQueryFlow = MutableStateFlow("")
    private val expandedItemIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    init {
      viewModelScope.launch {
        itemsRepository.getOtherItems().collect { catalogFlow.value = it }
      }
      // Pre-populate from current receipt (read once — not reactive)
      val existing = receiptsStore.currentReceipt.value?.otherItems ?: emptyList()
      tempSelectionsFlow.value =
        existing.associate { item ->
          item.id to
            TempSelection(
              netPrice = item.netPrice,
              quantity = item.quantity.toInt(),
              comment = item.comment,
            )
        }
    }

    val uiState: StateFlow<SelectOtherItemsUiState> =
      combine(
        catalogFlow,
        tempSelectionsFlow,
        searchQueryFlow,
        expandedItemIdsFlow,
      ) { catalog, selections, search, expandedIds ->
        val allItems = catalog.getOrNull() ?: emptyList()
        val filteredItems =
          allItems
            .filter { it.name.contains(search, ignoreCase = true) }
            .sortedBy { it.name }

        var netTotal = 0.0
        var grossTotal = 0.0

        selections.forEach { (itemId, sel) ->
          if ((sel.quantity ?: 0) > 0) {
            val item = allItems.find { it.id == itemId }
            if (item != null) {
              val effectivePrice = sel.netPrice ?: item.netPrice
              val amounts = AmountCalculator.calculateAmounts(effectivePrice, sel.quantity!!.toDouble(), item.vatRate)
              netTotal += amounts.netAmount
              grossTotal += amounts.grossAmount
            }
          }
        }

        SelectOtherItemsUiState(
          isLoading = catalog.isLoading,
          searchQuery = search,
          items = filteredItems,
          selections = selections,
          netTotal = netTotal,
          grossTotal = grossTotal,
          canAccept = selections.any { (_, sel) -> (sel.quantity ?: 0) > 0 },
          expandedItemIds = expandedIds,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelectOtherItemsUiState(),
      )

    fun onSearchQueryChanged(query: String) {
      searchQueryFlow.value = query
    }

    fun onToggleExpanded(itemId: Int) {
      expandedItemIdsFlow.update { current ->
        if (current.contains(itemId)) current - itemId else current + itemId
      }
    }

    fun onQuantityChanged(
      itemId: Int,
      quantity: Int?,
    ) {
      tempSelectionsFlow.update { current ->
        val existing = current[itemId] ?: TempSelection()
        current + (itemId to existing.copy(quantity = quantity))
      }
    }

    fun onNetPriceChanged(
      itemId: Int,
      netPrice: Double?,
    ) {
      tempSelectionsFlow.update { current ->
        val existing = current[itemId] ?: TempSelection()
        current + (itemId to existing.copy(netPrice = netPrice))
      }
    }

    fun onCommentChanged(
      itemId: Int,
      comment: String?,
    ) {
      tempSelectionsFlow.update { current ->
        val existing = current[itemId] ?: TempSelection()
        current + (itemId to existing.copy(comment = comment?.takeIf { it.isNotBlank() }))
      }
    }

    fun confirmHandler(onSuccess: () -> Unit) {
      val catalog = catalogFlow.value.getOrNull() ?: return
      val selections = tempSelectionsFlow.value

      val receiptOtherItems =
        selections
          .filter { (_, sel) -> (sel.quantity ?: 0) > 0 }
          .mapNotNull { (itemId, sel) ->
            val item = catalog.find { it.id == itemId } ?: return@mapNotNull null
            val effectivePrice = sel.netPrice ?: item.netPrice
            val qty = sel.quantity!!.toDouble()
            val amounts = AmountCalculator.calculateAmounts(effectivePrice, qty, item.vatRate)
            ReceiptOtherItem(
              id = item.id,
              articleNumber = item.articleNumber,
              name = item.name,
              quantity = qty,
              unitName = item.unitName,
              netPrice = effectivePrice,
              netAmount = amounts.netAmount,
              vatRate = item.vatRate,
              vatAmount = amounts.vatAmount,
              grossAmount = amounts.grossAmount,
              comment = sel.comment,
            )
          }

      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(otherItems = receiptOtherItems)
      }
      onSuccess()
    }
  }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests "com.zephyr.boreal.ui.screens.SelectOtherItemsViewModelTest"
```

Expected: BUILD SUCCESSFUL, all 11 tests pass.

- [ ] **Step 5: Run ktlint**

```bash
./gradlew ktlintFormat
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModel.kt \
        app/src/test/java/com/zephyr/boreal/ui/screens/SelectOtherItemsViewModelTest.kt
git commit -m "feat: add SelectOtherItemsViewModel with selection and confirmation logic"
```

---

## Task 3: SelectOtherItemsScreen + string resources

**Files:**
- Create: `app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `SelectOtherItemsViewModel` (Task 2), `QuantityStepper` (Task 1), `BorealTopAppBar`, `BorealSearchField`, `BorealTextInput`, `BorealButton`, `BorealColors`, `NunitoSansFamily`, `BorealFontSizes`, `AmountCalculator`
- Produces: `SelectOtherItemsScreen(viewModel: SelectOtherItemsViewModel, onNavigateBack: () -> Unit)` — consumed by Task 6 (navigation)

- [ ] **Step 1: Add string resources to `strings.xml`**

Inside the `<resources>` block in `app/src/main/res/values/strings.xml`, add these entries (place them after the existing `review_items_*` block):

```xml
<string name="select_other_items_title">Extra tételek</string>
<string name="select_other_items_search_hint">Keresés…</string>
<string name="select_other_items_quantity_label">Mennyiség</string>
<string name="select_other_items_net_price_label">Nettó egységár</string>
<string name="select_other_items_comment_label">Megjegyzés</string>
<string name="select_other_items_net_total_label">Nettó összesen:</string>
<string name="select_other_items_gross_total_label">Bruttó összesen:</string>
<string name="select_other_items_accept_description">Elfogadás</string>
```

- [ ] **Step 2: Create `SelectOtherItemsScreen.kt`**

```kotlin
package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.ui.components.BorealSearchField
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.QuantityStepper
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import java.text.NumberFormat
import java.util.Locale

private const val ANIMATION_DURATION_MS = 300

@Composable
fun SelectOtherItemsScreen(
  viewModel: SelectOtherItemsViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SelectOtherItemsScreenContent(
    uiState = uiState,
    onSearchQueryChanged = viewModel::onSearchQueryChanged,
    onToggleExpanded = viewModel::onToggleExpanded,
    onQuantityChanged = viewModel::onQuantityChanged,
    onNetPriceChanged = viewModel::onNetPriceChanged,
    onCommentChanged = viewModel::onCommentChanged,
    onConfirm = { viewModel.confirmHandler(onNavigateBack) },
  )
}

@Composable
internal fun SelectOtherItemsScreenContent(
  uiState: SelectOtherItemsUiState,
  onSearchQueryChanged: (String) -> Unit,
  onToggleExpanded: (Int) -> Unit,
  onQuantityChanged: (Int, Int?) -> Unit,
  onNetPriceChanged: (Int, Double?) -> Unit,
  onCommentChanged: (Int, String?) -> Unit,
  onConfirm: () -> Unit,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("hu", "HU")) }

  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.select_other_items_title),
        actions = {
          IconButton(
            onClick = onConfirm,
            enabled = uiState.canAccept,
          ) {
            Icon(
              painter = painterResource(id = R.drawable.arrow_forward),
              contentDescription = stringResource(R.string.select_other_items_accept_description),
              tint = if (uiState.canAccept) BorealColors.White else BorealColors.Disabled,
            )
          }
        },
      )
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        SelectOtherItemsFooter(
          netTotal = uiState.netTotal,
          grossTotal = uiState.grossTotal,
          currencyFormat = currencyFormat,
        )
      }
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      BorealSearchField(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChanged,
        placeholderText = stringResource(R.string.select_other_items_search_hint),
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(modifier = Modifier.weight(1f)) {
        items(uiState.items, key = { it.id }) { item ->
          val selection = uiState.selections[item.id]
          val isExpanded = uiState.expandedItemIds.contains(item.id)
          OtherItemSelectionAccordion(
            item = item,
            selection = selection,
            isExpanded = isExpanded,
            currencyFormat = currencyFormat,
            onHeaderClick = { onToggleExpanded(item.id) },
            onQuantityChange = { onQuantityChanged(item.id, it?.toInt()) },
            onNetPriceChange = { onNetPriceChanged(item.id, it) },
            onCommentChange = { onCommentChanged(item.id, it) },
          )
        }
      }
    }
  }
}

@Suppress("MagicNumber")
@Composable
private fun OtherItemSelectionAccordion(
  item: OtherItem,
  selection: TempSelection?,
  isExpanded: Boolean,
  currencyFormat: NumberFormat,
  onHeaderClick: () -> Unit,
  onQuantityChange: (Double?) -> Unit,
  onNetPriceChange: (Double?) -> Unit,
  onCommentChange: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val qty = (selection?.quantity ?: 0).toDouble()
  val effectivePrice = selection?.netPrice ?: item.netPrice
  val grossAmount =
    if (qty > 0) {
      AmountCalculator.calculateAmounts(effectivePrice, qty, item.vatRate).grossAmount
    } else {
      0.0
    }
  val quantityStr = if (qty % 1 == 0.0) qty.toInt().toString() else qty.toString()
  val hasQuantity = (selection?.quantity ?: 0) > 0
  val headerColor = if (hasQuantity) BorealColors.Ok else BorealColors.Neutral

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerColor)
          .clickable(onClick = onHeaderClick)
          .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.titleLarge,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
    }

    HorizontalDivider(color = Color.White, thickness = 1.dp)

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      OtherItemRow(
        label = stringResource(R.string.review_items_quantity_label),
        value = "$quantityStr ${item.unitName}",
      )
      OtherItemRow(
        label = stringResource(R.string.review_items_gross_label),
        value = currencyFormat.format(grossAmount),
      )
    }

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
      ) {
        QuantityStepper(
          label = stringResource(R.string.select_other_items_quantity_label),
          quantity = if (qty > 0) qty else null,
          maxQuantity = null,
          onQuantityChange = onQuantityChange,
        )
        Spacer(modifier = Modifier.height(8.dp))
        BorealTextInput(
          label = stringResource(R.string.select_other_items_net_price_label),
          value = effectivePrice.toInt().toString(),
          onValueChange = { onNetPriceChange(it.toDoubleOrNull()) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BorealTextInput(
          label = stringResource(R.string.select_other_items_comment_label),
          value = selection?.comment ?: "",
          onValueChange = { onCommentChange(it.ifBlank { null }) },
          maxLength = 100,
        )
      }
    }
  }
}

@Composable
private fun OtherItemRow(
  label: String,
  value: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = value,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
    )
  }
}

@Composable
private fun SelectOtherItemsFooter(
  netTotal: Double,
  grossTotal: Double,
  currencyFormat: NumberFormat,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "${stringResource(R.string.select_other_items_net_total_label)} ${currencyFormat.format(netTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Text(
      text = "${stringResource(R.string.select_other_items_gross_total_label)} ${currencyFormat.format(grossTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
  }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run ktlint**

```bash
./gradlew ktlintFormat
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/ui/screens/SelectOtherItemsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat: add SelectOtherItemsScreen UI and string resources"
```

---

## Task 4: ReviewItemsViewModel changes + tests

**Files:**
- Modify: `app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModel.kt`
- Modify: `app/src/test/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModelTest.kt`

**Interfaces:**
- Consumes: `ReceiptsStore.currentReceipt` (now reads `draft?.otherItems` in addition to `draft?.items`)
- Produces (additions to existing `ReviewItemsUiState`):
  - `otherItems: List<ReceiptOtherItem>` — consumed by Task 5 (screen)
  - `expandedOtherItemIds: Set<Int>` — consumed by Task 5 (screen)
  - `grossTotal` now = `items.sumOf { grossAmount } + otherItems.sumOf { grossAmount }`
- Produces (new methods):
  - `onToggleOtherItemExpanded(id: Int)`
  - `removeOtherItem(id: Int)` — removes from `DraftReceipt.otherItems`; does **not** check items.isEmpty() or navigate

- [ ] **Step 1: Update `ReviewItemsViewModel.kt`**

Replace the entire file content:

```kotlin
package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItemsUiState(
  val items: List<ReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
  val grossTotal: Double = 0.0,
  val expandedItemKeys: Set<String> = emptySet(),
  val expandedOtherItemIds: Set<Int> = emptySet(),
  val showCancelConfirmation: Boolean = false,
)

@HiltViewModel
class ReviewItemsViewModel
  @Inject
  constructor(
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewItemsUiState())
    val uiState: StateFlow<ReviewItemsUiState> = _uiState.asStateFlow()

    init {
      viewModelScope.launch {
        receiptsStore.currentReceipt.collect { draft ->
          val items = draft?.items ?: emptyList()
          val otherItems = draft?.otherItems ?: emptyList()
          _uiState.update {
            it.copy(
              items = items,
              otherItems = otherItems,
              grossTotal = items.sumOf { item -> item.grossAmount } + otherItems.sumOf { item -> item.grossAmount },
            )
          }
        }
      }
    }

    fun onToggleExpanded(key: String) {
      _uiState.update { state ->
        val newKeys =
          if (state.expandedItemKeys.contains(key)) {
            state.expandedItemKeys - key
          } else {
            state.expandedItemKeys + key
          }
        state.copy(expandedItemKeys = newKeys)
      }
    }

    fun onToggleOtherItemExpanded(id: Int) {
      _uiState.update { state ->
        val newIds =
          if (state.expandedOtherItemIds.contains(id)) {
            state.expandedOtherItemIds - id
          } else {
            state.expandedOtherItemIds + id
          }
        state.copy(expandedOtherItemIds = newIds)
      }
    }

    fun removeItem(
      id: Int,
      expirationId: Int,
      onNavigateHome: () -> Unit,
    ) {
      receiptsStore.upsertSelectedItem(id, expirationId, null)
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(
          items = draft.items.filter { !(it.id == id && it.expirationId == expirationId) },
        )
      }
      if (receiptsStore.currentReceipt.value
          ?.items
          .isNullOrEmpty()
      ) {
        receiptsStore.resetReceipts()
        onNavigateHome()
      }
    }

    fun removeOtherItem(id: Int) {
      receiptsStore.updateCurrentReceipt { draft ->
        draft.copy(otherItems = draft.otherItems.filter { it.id != id })
      }
    }

    fun showCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    fun dismissCancelDialog() {
      _uiState.update { it.copy(showCancelConfirmation = false) }
    }

    fun cancelReceipt(onNavigateHome: () -> Unit) {
      receiptsStore.resetReceipts()
      onNavigateHome()
    }
  }
```

- [ ] **Step 2: Update `ReviewItemsViewModelTest.kt`**

Add the following tests **after** the existing `cancelReceipt calls resetReceipts and navigates home` test. Also update the existing `initially should populate items and grossTotal from store` test to verify combined grossTotal:

First, update the existing test at line 65 — find the `initially should populate items and grossTotal from store` test and replace its body to also cover the new `otherItems` field:

```kotlin
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
```

Then add these new tests at the end of the class:

```kotlin
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

    assertTrue(viewModel.uiState.value.expandedOtherItemIds.contains(10))
  }

@Test
fun `onToggleOtherItemExpanded removes id when already expanded`() =
  runTest {
    val viewModel = ReviewItemsViewModel(receiptsStore)
    runCurrent()

    viewModel.onToggleOtherItemExpanded(10)
    viewModel.onToggleOtherItemExpanded(10)

    assertFalse(viewModel.uiState.value.expandedOtherItemIds.contains(10))
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
    org.mockito.kotlin.verifyNoMoreInteractions(receiptsStore.let {
      // resetReceipts should NOT have been called
      verify(it, org.mockito.kotlin.never()).resetReceipts()
      it
    })
  }
```

Also add the `buildOtherItem` helper inside the test class next to `buildItem`:

```kotlin
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
```

And add the missing import at the top of the test file:
```kotlin
import com.zephyr.boreal.domain.model.ReceiptOtherItem
```

- [ ] **Step 3: Run the updated test suite**

```bash
./gradlew test --tests "com.zephyr.boreal.ui.screens.ReviewItemsViewModelTest"
```

Expected: BUILD SUCCESSFUL, all tests pass (existing + new).

- [ ] **Step 4: Run ktlint**

```bash
./gradlew ktlintFormat
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModel.kt \
        app/src/test/java/com/zephyr/boreal/ui/screens/ReviewItemsViewModelTest.kt
git commit -m "feat: extend ReviewItemsViewModel with otherItems, combined total, and removeOtherItem"
```

---

## Task 5: ReviewItemsScreen changes + string resources

**Files:**
- Modify: `app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ReviewItemsViewModel` (Task 4 additions), `ReceiptOtherItem` domain model
- Produces: Updated `ReviewItemsScreen(viewModel, onNavigateHome, onNavigateToOtherItems: () -> Unit)` — consumed by Task 6 (navigation)

- [ ] **Step 1: Add comment label string resource**

In `strings.xml`, after the `select_other_items_*` block added in Task 3, add:

```xml
<string name="review_items_other_comment_label">Megjegyzés:</string>
```

- [ ] **Step 2: Update `ReviewItemsScreen.kt`**

Replace the entire file content:

```kotlin
package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.domain.model.ReceiptOtherItem
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private const val ANIMATION_DURATION_MS = 300

@Composable
fun ReviewItemsScreen(
  viewModel: ReviewItemsViewModel,
  onNavigateHome: () -> Unit,
  onNavigateToOtherItems: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  if (uiState.showCancelConfirmation) {
    BorealAlert(
      title = stringResource(R.string.review_items_cancel_title),
      message = stringResource(R.string.review_items_cancel_message),
      confirmButtonText = stringResource(R.string.review_items_cancel_confirm),
      confirmButtonVariant = ButtonVariant.WARNING,
      cancelButtonText = stringResource(R.string.review_items_cancel_dismiss),
      onConfirmClick = { viewModel.cancelReceipt(onNavigateHome) },
      onCancelClick = { viewModel.dismissCancelDialog() },
      onDismissRequest = { viewModel.dismissCancelDialog() },
    )
  }

  ReviewItemsScreenContent(
    uiState = uiState,
    onToggleExpanded = viewModel::onToggleExpanded,
    onToggleOtherItemExpanded = viewModel::onToggleOtherItemExpanded,
    onRemoveItem = { id, expirationId -> viewModel.removeItem(id, expirationId, onNavigateHome) },
    onRemoveOtherItem = viewModel::removeOtherItem,
    onCancelClick = viewModel::showCancelDialog,
    onNavigateToOtherItems = onNavigateToOtherItems,
  )
}

@Composable
internal fun ReviewItemsScreenContent(
  uiState: ReviewItemsUiState,
  onToggleExpanded: (String) -> Unit,
  onToggleOtherItemExpanded: (Int) -> Unit,
  onRemoveItem: (Int, Int) -> Unit,
  onRemoveOtherItem: (Int) -> Unit,
  onCancelClick: () -> Unit,
  onNavigateToOtherItems: () -> Unit,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("hu", "HU")) }

  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.review_items_title))
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        ReviewItemsFooter(
          grossTotal = uiState.grossTotal,
          currencyFormat = currencyFormat,
          onCancelClick = onCancelClick,
        )
      }
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        BorealButton(
          text = stringResource(R.string.review_items_extra_items),
          variant = ButtonVariant.OK,
          onClick = onNavigateToOtherItems,
        )
      }

      LazyColumn(
        modifier = Modifier.weight(1f),
      ) {
        items(
          items = uiState.items,
          key = { "${it.id}_${it.expirationId}" },
        ) { item ->
          val key = "${item.id}_${item.expirationId}"
          val isExpanded = uiState.expandedItemKeys.contains(key)

          ReviewItemAccordion(
            item = item,
            isExpanded = isExpanded,
            currencyFormat = currencyFormat,
            onHeaderClick = { onToggleExpanded(key) },
            onRemoveClick = { onRemoveItem(item.id, item.expirationId) },
          )
        }

        items(
          items = uiState.otherItems,
          key = { "other_${it.id}" },
        ) { item ->
          val isExpanded = uiState.expandedOtherItemIds.contains(item.id)

          ReviewOtherItemAccordion(
            item = item,
            isExpanded = isExpanded,
            currencyFormat = currencyFormat,
            onHeaderClick = { onToggleOtherItemExpanded(item.id) },
            onRemoveClick = { onRemoveOtherItem(item.id) },
          )
        }
      }
    }
  }
}

@Suppress("MagicNumber")
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReviewItemAccordion(
  item: ReceiptItem,
  isExpanded: Boolean,
  currencyFormat: NumberFormat,
  onHeaderClick: () -> Unit,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val headerColor = if (isExpanded) BorealColors.Ok else BorealColors.Neutral
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val quantityStr =
    if (item.quantity % 1 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2)
      bringIntoViewRequester.bringIntoView()
    }
  }

  Column(
    modifier =
      modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    ReviewItemAccordionHeader(
      name = item.name,
      expiresAt = item.expiresAt,
      headerColor = headerColor,
      onHeaderClick = onHeaderClick,
    )

    HorizontalDivider(color = Color.White, thickness = 1.dp)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      ReviewItemRow(stringResource(R.string.review_items_quantity_label), "$quantityStr ${item.unitName}")
      ReviewItemRow(stringResource(R.string.review_items_gross_label), currencyFormat.format(item.grossAmount))
    }

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      ReviewItemAccordionExpandedContent(
        articleNumber = item.articleNumber,
        onRemoveClick = onRemoveClick,
      )
    }
  }
}

@Suppress("MagicNumber")
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReviewOtherItemAccordion(
  item: ReceiptOtherItem,
  isExpanded: Boolean,
  currencyFormat: NumberFormat,
  onHeaderClick: () -> Unit,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val headerColor = if (isExpanded) BorealColors.Ok else BorealColors.Neutral
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val quantityStr =
    if (item.quantity % 1 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2)
      bringIntoViewRequester.bringIntoView()
    }
  }

  Column(
    modifier =
      modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerColor)
          .clickable(onClick = onHeaderClick)
          .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.titleLarge,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
    }

    HorizontalDivider(color = Color.White, thickness = 1.dp)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      ReviewItemRow(stringResource(R.string.review_items_quantity_label), "$quantityStr ${item.unitName}")
      ReviewItemRow(stringResource(R.string.review_items_gross_label), currencyFormat.format(item.grossAmount))
    }

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
      ) {
        ReviewItemRow(
          label = stringResource(R.string.review_items_article_number_label),
          value = item.articleNumber,
        )
        if (!item.comment.isNullOrBlank()) {
          ReviewItemRow(
            label = stringResource(R.string.review_items_other_comment_label),
            value = item.comment,
          )
        }
        BorealButton(
          text = stringResource(R.string.review_items_delete),
          variant = ButtonVariant.WARNING,
          onClick = onRemoveClick,
          modifier = Modifier.padding(top = 12.dp),
        )
      }
    }
  }
}

@Composable
private fun ReviewItemAccordionHeader(
  name: String,
  expiresAt: String,
  headerColor: androidx.compose.ui.graphics.Color,
  onHeaderClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(headerColor)
        .clickable(onClick = onHeaderClick)
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.titleLarge,
      color = BorealColors.White,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = expiresAt,
      style = MaterialTheme.typography.titleLarge,
      color = BorealColors.White,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ReviewItemAccordionExpandedContent(
  articleNumber: String,
  onRemoveClick: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
    ReviewItemRow(
      label = stringResource(R.string.review_items_article_number_label),
      value = articleNumber,
    )
    BorealButton(
      text = stringResource(R.string.review_items_delete),
      variant = ButtonVariant.WARNING,
      onClick = onRemoveClick,
      modifier = Modifier.padding(top = 12.dp),
    )
  }
}

@Composable
private fun ReviewItemRow(
  label: String,
  value: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = value,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
    )
  }
}

@Composable
private fun ReviewItemsFooter(
  grossTotal: Double,
  currencyFormat: NumberFormat,
  onCancelClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "${stringResource(R.string.review_items_total_label)} ${currencyFormat.format(grossTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 12.dp),
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      BorealButton(
        text = stringResource(R.string.review_items_cancel),
        variant = ButtonVariant.WARNING,
        onClick = onCancelClick,
      )
      BorealButton(
        text = stringResource(R.string.review_items_finalize),
        variant = ButtonVariant.OK,
        onClick = {},
      )
    }
  }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run ktlint**

```bash
./gradlew ktlintFormat
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/ui/screens/ReviewItemsScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat: add other items accordion and navigation hook to ReviewItemsScreen"
```

---

## Task 6: Navigation wiring + final quality checks

**Files:**
- Modify: `app/src/main/java/com/zephyr/boreal/MainActivity.kt`

**Interfaces:**
- Consumes: `SelectOtherItemsScreen` (Task 3), updated `ReviewItemsScreen` signature (Task 5), `SelectOtherItemsViewModel` (Task 2)

- [ ] **Step 1: Update `MainActivity.kt`**

In `MainActivity.kt`, make three changes:

**1. Add the new composable route** inside the `NavHost` block, after the existing `composable("review_items")` block:

```kotlin
composable("select_other_items") {
  SelectOtherItemsRoute(navController)
}
```

**2. Update `ReviewItemsRoute`** to add the `onNavigateToOtherItems` callback:

```kotlin
private fun ReviewItemsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: ReviewItemsViewModel = hiltViewModel()
  ReviewItemsScreen(
    viewModel = viewModel,
    onNavigateHome = {
      navController.navigate("main") {
        popUpTo(0) { inclusive = true }
      }
    },
    onNavigateToOtherItems = {
      navController.navigate("select_other_items")
    },
  )
}
```

**3. Add the new `SelectOtherItemsRoute` function** near the bottom of the file, after `ReviewItemsRoute`:

```kotlin
@Composable
private fun SelectOtherItemsRoute(navController: androidx.navigation.NavHostController) {
  val viewModel: SelectOtherItemsViewModel = hiltViewModel()
  SelectOtherItemsScreen(
    viewModel = viewModel,
    onNavigateBack = { navController.popBackStack() },
  )
}
```

- [ ] **Step 2: Verify full compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run full test suite**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 4: Run ktlint and detekt**

```bash
./gradlew ktlintFormat
./gradlew detekt
```

Expected: Both pass with no errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/zephyr/boreal/MainActivity.kt
git commit -m "feat: wire other items navigation route and connect Review Screen button"
```

---

## Self-Review Checklist

**Spec coverage:**
- ✅ Section 1 (QuantityStepper extraction) → Task 1
- ✅ Section 2 (SelectOtherItemsViewModel — TempSelection, pre-populate, totals, confirmHandler) → Task 2
- ✅ Section 3 (SelectOtherItemsScreen — search, accordions, footer) → Task 3
- ✅ Section 4 (ReviewItemsViewModel — otherItems, combined grossTotal, removeOtherItem, toggle) → Task 4
- ✅ Section 5 (ReviewItemsScreen — button, other items accordion, comment row) → Task 5
- ✅ Section 6 (Navigation — composable route, SelectOtherItemsRoute, ReviewItemsRoute update) → Task 6
- ✅ Section 7 (String resources) → Tasks 3 and 5

**Placeholder scan:** No TBDs, no "add appropriate handling" — all steps have complete code.

**Type consistency:**
- `TempSelection` defined in Task 2, consumed in Task 3 (screen reads `uiState.selections[item.id]: TempSelection?`) — consistent.
- `onQuantityChanged(Int, Int?)` defined in Task 2, wired in Task 3 as `{ onQuantityChanged(item.id, it?.toInt()) }` — consistent.
- `ReviewItemsUiState.otherItems`, `.expandedOtherItemIds`, `.grossTotal` defined in Task 4, read in Task 5 — consistent.
- `ReviewItemsScreen(viewModel, onNavigateHome, onNavigateToOtherItems)` signature changed in Task 5, call site updated in Task 6 — consistent.
- `SelectOtherItemsScreen(viewModel, onNavigateBack)` defined in Task 3, instantiated in Task 6 — consistent.
