# Task 07 · Storage: select items from store (batches, barcode, quantities)

## Goal
Let the user set, per item **expiration batch**, how much stock to load onto (or remove from) the
locked store, with search and barcode lookup, tracking each batch's change against its current
and primary-store quantities.

## Business context
After locking a store (Task 06), the user goes item-by-item, expiration-batch by batch, entering
a **quantity change** (positive = load onto the van store, negative = return). Each batch line
shows how much is currently in the selected store (its starting quantity) and how much is
available in the primary/central store. Items are searchable by name and can be found by scanning
an item or expiration barcode. Only lines with a quantity change proceed to review.

## Starting / resulting state
- **Start**: Task 06 has locked a store and loaded primary + selected store details into storage
  session state; select-items is a stub route.
- **Result**: a scrollable, searchable list of item→expiration lines with editable quantity
  changes; a forward action (enabled once any line changed) navigates to review (Task 08).

## Data requirements
- Build the working list (RN `StorageListItem`) by combining item master data with the selected
  store's per-expiration stock and the primary store's per-expiration stock:
  `itemId, expirationId, articleNumber, name, expiresAt, itemBarcode, expirationBarcode,
  unitName, primaryStoreQuantity, originalQuantity (current in selected store), quantityChange`.
- Persist the working list + changes in storage-flow session state so it survives navigating to
  review and back, and so the tile can resume an in-progress load.
- Keep the immutable initial store snapshot (from Task 06) for the review/summary/print later.

## Behavioral requirements
- Search filters by item name; the list is grouped/sorted consistently with RN (by name, batches
  under an item by expiry).
- Quantity entry per batch (reuse `QuantityStepper`/numeric input): integer changes; a change may
  be positive or negative. Validate against available primary-store quantity where RN does
  (cannot load more than the primary store holds); returns limited by what the batch currently
  holds. Match RN's exact bounds.
- Barcode scan: scanning jumps to / filters the matching item or expiration batch and brings it
  into view (RN scrolls the matched row into view). Reuse `BarcodeScanner`; handle unknown codes
  gracefully.
- Forward affordance appears once **any** line has a non-null change; it navigates to review.
- A cancel/abandon path prompts a confirmation (abandoning the load) consistent with RN's
  cancel-storage alert; abandoning must also release the store lock (unlock) — coordinate with
  Task 08's unlock behavior so a lock is never left dangling.
- Offline handling consistent with the rest of the flow (loading requires connectivity, enforced
  at save time in Task 08).

## API requirements
- None new here — the data comes from the store details already fetched in Task 06. (No mutation
  until Task 08's `load`.)

## UI/UX notes
- Reuse `Container`, `BorealSearchField`, `Input`/`QuantityStepper`, `BorealAlert`,
  `BarcodeScanner`, forward-icon app-bar action. Header "Tételek". Show current + primary
  quantities on each line (recent RN change surfaced current qty prominently).

## Testing requirements
- ViewModel unit tests: list assembly from item + selected-store + primary-store data; search
  filtering; quantity-change bounds (over primary stock, negative below current); "any changed"
  derivation; barcode → target row resolution (item vs. expiration code); abandon→unlock.
- Instrumented tests: enter a change and see totals/forward enable; search narrows list; scanning
  focuses a row.

## Non-goals
- No submission to the server (Task 08). No editing of item master data or expirations.

## Depends on
Task 06.

## RN reference paths
- `resources/frontend/screens/storage/select-items/SelectItemsFromStore.tsx`,
  `useSelectItemsFromStoreData.ts`, `ExpirationAccordionDetails.tsx`, `ScanBarCodeInStorage.tsx`
- `resources/frontend/store/storageFlowSlice.ts` (`StorageListItem`), `store/storageSlice.ts`
