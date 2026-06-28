# Other Items — Full Implementation Design

**Date:** 2026-06-28  
**Status:** Approved

---

## Context

The sell flow already supports regular items (with expiration) and order items. A third category — "other items" — exists in the backend (`/other_items` API), the domain models (`OtherItem`, `ReceiptOtherItem`), and `DraftReceipt.otherItems`, but has no UI in the native Android app.

On the Review Screen, the "Extra tételek" button is a placeholder with an empty `onClick`. The goal is to implement the full other-items flow: a dedicated selection screen launched from Review, accordion display of selected items in Review, and correct totals and reset behaviour throughout.

Other items differ from regular items in that they have no expiration date, cannot be ordered (purchase only), and their quantity/price/comment are all user-editable at point of sale.

---

## Architecture

```
ReviewItemsScreen
  ├─ "Extra tételek" button → navigates to "select_other_items"
  └─ Other items LazyColumn section (same accordion style, no expiration)

SelectOtherItemsScreen  [new]
  ├─ Back → popBackStack() — DraftReceipt.otherItems unchanged, temp state discarded
  └─ Accept → DraftReceipt.otherItems = computed list, popBackStack()
```

`SelectOtherItemsViewModel` is scoped to the `NavBackStackEntry` and is re-created on each navigation to the route. On `init` it reads `currentReceipt.otherItems` once to pre-populate `tempSelections`, so re-entering the screen always reflects what is currently committed to the draft receipt.

`ReceiptsStore` gains no new state. `DraftReceipt.otherItems` is the single source of truth for committed other items.

---

## Section 1 — Component extraction: QuantityStepper

Extract `SelectionRow` (currently `private` in `SelectItemsScreen.kt`, lines 400–503) into a new standalone composable `ui/components/QuantityStepper.kt`.

**Signature (unchanged from current private version):**
```kotlin
@Composable
fun QuantityStepper(
  label: String,
  quantity: Double?,
  maxQuantity: Double?,
  onQuantityChange: (Double?) -> Unit,
  modifier: Modifier = Modifier,
)
```

`SelectItemsScreen.kt` is updated to call the extracted component in place of the now-deleted private `SelectionRow`. No behaviour change.

---

## Section 2 — SelectOtherItemsViewModel

**New file:** `ui/screens/SelectOtherItemsViewModel.kt`

### Data classes

```kotlin
data class TempSelection(
  val netPrice: Double?,
  val quantity: Int?,
  val comment: String?,
)

data class SelectOtherItemsUiState(
  val isLoading: Boolean = true,
  val searchQuery: String = "",
  val items: List<OtherItem> = emptyList(),       // filtered + sorted catalog
  val selections: Map<Int, TempSelection> = emptyMap(),
  val netTotal: Double = 0.0,
  val grossTotal: Double = 0.0,
  val canAccept: Boolean = false,                  // true when ≥1 item has quantity > 0
)
```

### Init

Collect `itemsRepository.getOtherItems()` to populate the catalog. Read `receiptsStore.currentReceipt.value?.otherItems` once (not as a flow) to seed `tempSelectionsFlow` — mapping each `ReceiptOtherItem` back to a `TempSelection` keyed by `id`. This gives pre-population without ongoing coupling to the receipt flow.

### Actions

- `onSearchQueryChanged(query: String)`
- `onQuantityChanged(itemId: Int, quantity: Int?)` — updates `tempSelectionsFlow`
- `onNetPriceChanged(itemId: Int, netPrice: Double?)` — updates `tempSelectionsFlow`
- `onCommentChanged(itemId: Int, comment: String?)` — updates `tempSelectionsFlow`

### confirmHandler(onSuccess: () -> Unit)

1. For each `(itemId, selection)` where `selection.quantity > 0`:
   - Look up the `OtherItem` from catalog
   - Resolve effective `netPrice`: `selection.netPrice ?: item.netPrice`
   - Call `AmountCalculator.calculateAmounts(netPrice, quantity.toDouble(), item.vatRate)`
   - Build `ReceiptOtherItem(id, articleNumber, name, quantity, unitName, netPrice, netAmount, vatRate, vatAmount, grossAmount, comment)`
2. `receiptsStore.updateCurrentReceipt { draft -> draft.copy(otherItems = newList) }`
3. `onSuccess()`

### Totals (derived in uiState combine)

- `netTotal` = sum of `netPrice * quantity` for all selections with quantity > 0, using effective netPrice
- `grossTotal` = sum of `AmountCalculator.calculateGrossAmount(netAmount, vatRate)`

---

## Section 3 — SelectOtherItemsScreen

**New file:** `ui/screens/SelectOtherItemsScreen.kt`

### Layout

```
Scaffold(
  topBar = BorealTopAppBar(
    title = "Extra tételek",
    actions = [check icon button, enabled when canAccept, onClick = confirmHandler]
  ),
  bottomBar = SelectOtherItemsFooter(netTotal, grossTotal),
  containerColor = BorealColors.Background,
) {
  Column {
    BorealSearchField(...)
    LazyColumn {
      items(uiState.items, key = { it.id }) { item ->
        OtherItemAccordion(item, selection, isExpanded, ...)
      }
    }
  }
}
```

### OtherItemAccordion

- **Header**: item name; background `BorealColors.Ok` if `selection?.quantity ?: 0 > 0`, else `BorealColors.Neutral`; click toggles expansion
- **Always-visible content**: quantity + `item.unitName`, gross amount (formatted as currency; zero until qty set)
- **Expanded content**:
  - `QuantityStepper(label, quantity?.toDouble(), maxQuantity = null, onQuantityChange)`
  - `BorealTextInput` for net unit price (numeric, pre-filled with `selection?.netPrice ?: item.netPrice`)
  - `BorealTextInput` for comment (max 100 chars, multiline)

### SelectOtherItemsFooter

Matches the style of `SelectItemsFooter`: two rows (net total, gross total) using Hungarian currency format, inside a `BorealColors.Neutral` bottom bar.

---

## Section 4 — ReviewItemsViewModel changes

### UiState additions

```kotlin
data class ReviewItemsUiState(
  val items: List<ReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),   // ← new
  val grossTotal: Double = 0.0,                            // now covers both lists
  val expandedItemKeys: Set<String> = emptySet(),
  val expandedOtherItemIds: Set<Int> = emptySet(),         // ← new
  val showCancelConfirmation: Boolean = false,
)
```

### Updated init collector

```kotlin
receiptsStore.currentReceipt.collect { draft ->
  val items = draft?.items ?: emptyList()
  val otherItems = draft?.otherItems ?: emptyList()
  _uiState.update {
    it.copy(
      items = items,
      otherItems = otherItems,
      grossTotal = items.sumOf { i -> i.grossAmount } + otherItems.sumOf { o -> o.grossAmount },
    )
  }
}
```

### New actions

```kotlin
fun onToggleOtherItemExpanded(id: Int) { ... }   // mirrors onToggleExpanded

fun removeOtherItem(id: Int) {
  receiptsStore.updateCurrentReceipt { draft ->
    draft.copy(otherItems = draft.otherItems.filter { it.id != id })
  }
  // No home-navigation check — only regular item removal triggers flow reset
}
```

---

## Section 5 — ReviewItemsScreen changes

### Signature change

```kotlin
fun ReviewItemsScreen(
  viewModel: ReviewItemsViewModel,
  onNavigateHome: () -> Unit,
  onNavigateToOtherItems: () -> Unit,   // ← new
)
```

### "Extra tételek" button

`onClick = onNavigateToOtherItems` (was `{}`)

### LazyColumn — second items block

After the existing regular-item `items(...)` block, add:

```kotlin
items(uiState.otherItems, key = { "other_${it.id}" }) { item ->
  val isExpanded = uiState.expandedOtherItemIds.contains(item.id)
  ReviewOtherItemAccordion(
    item = item,
    isExpanded = isExpanded,
    currencyFormat = currencyFormat,
    onHeaderClick = { viewModel.onToggleOtherItemExpanded(item.id) },
    onRemoveClick = { viewModel.removeOtherItem(item.id) },
  )
}
```

### ReviewOtherItemAccordion

- Header: item name only (no expiration date column)
- Always-visible: quantity + unitName, gross amount
- Expanded: article number row, comment row (only rendered if `item.comment` is non-null/non-blank), Delete button

### Footer

No changes needed — `grossTotal` already reflects both lists once the ViewModel is updated.

---

## Section 6 — Navigation (MainActivity.kt)

```kotlin
composable("select_other_items") {
  SelectOtherItemsRoute(navController)
}
```

```kotlin
private fun SelectOtherItemsRoute(navController: NavHostController) {
  val viewModel: SelectOtherItemsViewModel = hiltViewModel()
  SelectOtherItemsScreen(
    viewModel = viewModel,
    onNavigateBack = { navController.popBackStack() },
  )
}
```

Update `ReviewItemsRoute` to pass:
```kotlin
onNavigateToOtherItems = { navController.navigate("select_other_items") }
```

---

## Section 7 — String resources (strings.xml)

New entries needed for `SelectOtherItemsScreen`:

| Key | Value (Hungarian) |
|-----|-------------------|
| `select_other_items_title` | Extra tételek |
| `select_other_items_search_hint` | Keresés... |
| `select_other_items_net_total` | Nettó összesen: |
| `select_other_items_gross_total` | Bruttó összesen: |
| `select_other_items_accept` | Elfogadás |
| `select_other_items_quantity_label` | Mennyiség |
| `select_other_items_net_price_label` | Nettó egységár |
| `select_other_items_comment_label` | Megjegyzés |
| `select_other_items_net_total_label` | Nettó összesen: |
| `select_other_items_gross_total_label` | Bruttó összesen: |

Review screen other-item accordion:
| Key | Value (Hungarian) |
|-----|-------------------|
| `review_items_other_comment_label` | Megjegyzés: |

---

## What is NOT changing

- `ReceiptsStore` — no new fields
- `DraftReceipt` — `otherItems: List<ReceiptOtherItem>` field already exists
- `OtherItem` domain model, `OtherItemEntity`, `OtherItemDao`, `ItemsRepository.getOtherItems()` — all complete
- The flow-reset logic in `removeItem()` — untouched; only regular items trigger it
- Order items handling — untouched

---

## Verification

1. Run `./gradlew ktlintFormat` and `./gradlew detekt` — must pass
2. Unit test `SelectOtherItemsViewModel`: confirm/accept builds correct `ReceiptOtherItem` list; back discards (state not in store); pre-population from receipt; totals calculation; canAccept logic
3. Unit test `ReviewItemsViewModel`: `grossTotal` includes other items; `removeOtherItem` updates receipt but never navigates home; `removeItem` (regular) still resets when items list becomes empty even if otherItems remain
4. Manual: launch Review → tap Extra tételek → select items → Accept → verify items appear in Review accordion with correct totals → remove individual other items → verify footer updates → remove all regular items → verify full reset
