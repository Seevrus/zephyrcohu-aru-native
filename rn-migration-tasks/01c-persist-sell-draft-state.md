# Task 01c · Sell: persist in-progress draft state across process death

## Goal
Make the in-progress sell-flow state — the draft receipt, draft order, item/order-quantity
selections, and other-items temp selections — durable across app/process restarts, so a
salesperson mid-sale doesn't lose their cart if the app is killed (backgrounding, OS memory
pressure, a call interrupting the flow) before finalizing.

## Business context
Found while auditing persistence parity after Task 01b (see that task's context — the receipts
list gap). Cross-referenced RN's actual persistence architecture against the native app state by
state (`resources/frontend/store/store.ts`): RN's Zustand `persist` middleware is configured with
**no `partialize` filter**, so nearly the entire store — not just server-cache data — persists to
AsyncStorage unconditionally, including `sellFlowSlice` (`selectedItems`, `selectedOtherItems`,
`reviewItems`, `selectedPartner`, `isAdhocPartner`, `maxNewPartnerIdInUse`) and `receiptsSlice`
(`currentReceipt`) / `ordersSlice` (`currentOrder`). A killed RN app resumes the user exactly where
they left off mid-sale.

Native's equivalent state is entirely in-memory:
- `store/receipts/ReceiptsStore.kt`: `_currentReceipt: MutableStateFlow<DraftReceipt?>`,
  `_currentOrder: MutableStateFlow<DraftOrder?>`, `_selectedItems`, `_selectedOrderItems` — all
  bare `MutableStateFlow`s, no DataStore/Room backing.
- `ui/screens/SelectOtherItemsViewModel.kt`: the in-progress other-items quantity/price/comment
  selections (`_uiState.selections`) live only in ViewModel state.

Unlike Task 01b's gap (which could cause *wrong data submitted to the backend* — a duplicate
serial number, or a wrong end-of-round report), this gap's blast radius is "the user has to redo
their item picking" — nothing incorrect gets submitted, since none of this state has been sent to
the API yet. Still a real UX regression vs. RN parity, and worth closing.

Note: `StoreSessionStore`'s live per-round inventory snapshot (`selectedStoreCurrentState`/
`selectedStoreInitialState`) has the same in-memory-only shape, but is lower priority — the
backend decrements `ExpirationStore.quantity` immediately per finalized receipt
(`ReceiptController::create_receipts`), so a fresh re-fetch after process death already reflects
every receipt submitted so far this round correctly. Only the *not-yet-submitted* optimistic view
is lost. Consider covering it here if convenient (same store-restructuring effort), but don't let
it block the higher-value draft-receipt/order persistence.

## Starting / resulting state
- **Start**: `ReceiptsStore._currentReceipt`, `_currentOrder`, `_selectedItems`,
  `_selectedOrderItems` and `SelectOtherItemsViewModel`'s in-progress selections are memory-only;
  killing the app mid-sell-flow (any point before `ReviewItemsViewModel` successfully finalizes)
  loses all of it. The user is still correctly routed back into the sell flow on restart (round
  state is Room-backed via `UserEntity`/`RoundEntity`), just with an empty cart.
- **Result**: the same state survives process death — restarting mid-sell-flow resumes with the
  partner, picked items/quantities, discounts, other items, and any draft order intact, matching
  RN.

## Data requirements
- Persist `DraftReceipt`, `DraftOrder`, `selectedItems: Map<Int, Map<Int, Double>>`,
  `selectedOrderItems: Map<Int, Double>` — likely DataStore (Proto or a JSON-encoded Preferences
  value, following `data/local/converters/ItemConverters.kt`'s kotlinx.serialization pattern) is
  a better fit than Room here: this is a single current-session blob, not a queryable collection,
  closer in shape to `UserSessionStore`/`PrintSettingsStore` than to the catalog entities. Decide
  during implementation which fits the existing `store/core/StoreModule.kt` DataStore setup best.
- Persist the other-items temp-selection map from `SelectOtherItemsViewModel` similarly (or fold
  it into the same persisted blob as part of the draft receipt, since RN keeps it in the same
  `sellFlowSlice` alongside the rest of the draft).

## Behavioral requirements
- `ReceiptsStore`'s public `StateFlow` surface should not need to change for callers — same
  approach as Task 01b (persist underneath, keep the reactive API the same).
- `resetReceipts()` must clear the persisted draft state too, not just in-memory.
- On successful finalization (`ReviewItemsViewModel.submitValidatedReceipt` success path) or on
  explicit cancel (`ReviewItemsViewModel.cancelReceipt`), the persisted draft is cleared — mirror
  whatever `resetReceipts()` already does today, just make it durable.
- No change to business logic, validation, or the discount/totals calculations — this is purely a
  durability change underneath already-correct in-memory behavior.

## API requirements
None — purely local persistence (matches RN, which has no server-side draft concept either).

## UI/UX notes
None — no new user-visible surface. (Optional stretch: if a resumed draft is detected on app
start, RN doesn't show any special "resumed" indicator — it's silent/seamless. Match that; don't
invent a new UI affordance.)

## Testing requirements
- Unit tests: draft receipt/order/selections persist and are readable after simulating a fresh
  store instance backed by the same test DataStore; `resetReceipts()` clears the persisted draft,
  not just memory.
- Regression: existing `ReviewItemsViewModelTest`, `SelectItemsViewModelTest`,
  `SelectOtherItemsViewModelTest` suites still pass unchanged (public API contracts don't move).

## Non-goals
- No change to `StoreSessionStore`'s inventory-snapshot persistence (optional stretch, see
  business context — don't block on it).
- No new UI for resuming a draft.
- No fix to `resetReceipts()` also being called from `ReviewItemsViewModel.cancelReceipt`, which
  appears to wipe the whole round's receipt history, not just the current draft (flagged in Task
  01b too — separate audit, out of scope here).

## Depends on
Task 01b (same store, same persistence pattern to follow — doing 01b first establishes the
DataStore/converter conventions this task reuses).

## RN reference paths
- `resources/frontend/store/store.ts` (Zustand `persist` config — no `partialize`, confirms
  everything below is expected to survive restart)
- `resources/frontend/store/sellFlowSlice.ts`, `store/receiptsSlice.ts`, `store/ordersSlice.ts`
- native: `store/receipts/ReceiptsStore.kt`, `ui/screens/SelectOtherItemsViewModel.kt`,
  `store/core/StoreModule.kt` (existing DataStore wiring to extend),
  `store/user/UserSessionStore.kt` (closest existing pattern for a DataStore-backed session blob)
