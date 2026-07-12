# Task 08 · Storage: review, save & summary (load + unlock + print)

## Goal
Review the pending stock changes, submit them to the server as a storage load, then show a
summary that unlocks the store, optionally prints the load document, and returns home — closing
the "Rakodás" flow in a working, lock-free state.

## Business context
This completes warehouse loading. The review step lists the changed batches and the aggregate
quantities moving **up** (loaded onto the van) and **down** (returned). Confirming sends the
changes to the server (`storage/load`), which records a storage receipt and moves stock between
the primary and selected stores. The summary step then **releases the lock**
(`unlock_from_user`) — this must happen on any exit so a store is never left locked — lets the
user print the change document, and returns to the main screen. Loading requires connectivity.

## Starting / resulting state
- **Start**: Task 07 produced a set of changed batches in storage-flow session state; the store is
  locked to the user.
- **Result**: Review → confirm → server load → Summary (unlock + print + home). After completion
  the store is unlocked and storage session state is reset.

## Data requirements
- Changed batches = storage-flow items with a non-null `quantityChange`, sorted by name.
- Aggregate: `up` = sum of positive changes, `down` = sum of absolute negative changes.
- Load request per batch: `expirationId, startingQuantity (originalQuantity), quantityChange,
  finalQuantity (starting + change)` (RN `mapSaveSelectedItemsRequest`).
- Summary print uses the **initial** store snapshot + the changed items + user (RN
  `createPrintStorageChanges`), honoring a "print full storage vs. only changes" setting.
- A flag marking "changes saved to API" so the tile/flow can resume directly at the summary if the
  user leaves after saving but before unlocking.

## Behavioral requirements
- **Review**: show changed lines and up/down totals; confirm is enabled only when online and the
  primary store id is known. If there are zero changes, confirming still proceeds (nothing to
  send) to summary. On send error, stay on review and surface an error state.
- On successful send, set "saved to API" and navigate (stack reset) to Summary.
- **Summary**: releasing the lock (`unlock_from_user`) must run on **every** exit path
  (system back, return button) exactly once, then reset storage + storage-flow session state.
  Guard against leaving the store locked.
- Print the storage-change document via the printer (reuse the printer connector pattern; the
  print builder mirrors RN `createPrintStorageChanges`).
- Offline before save: block confirm with the standard "internet required" messaging.

## API requirements
- `POST /storage/load` (`StorageController@load`), Sanctum + `X-Device-Id`,
  `{ "data": { "changes": [ { expirationId, startingQuantity, quantityChange, finalQuantity } ] } }`.
  Validates the caller currently has a store in use; records a storage receipt and updates stock.
- `POST /storage/unlock_from_user` (`unlock_from_user`) to release the lock. Both online-only.

## UI/UX notes
- Reuse list/card components, `BorealButton`, `ErrorCard`, `LoadingIndicator`, printer connector.
  Review header "Áttekintés"; Summary header "Összegzés" with back suppressed (exit via the
  unlock-and-return handler). Hungarian copy from the RN screens.

## Testing requirements
- ViewModel unit tests: changed-items filter + up/down aggregation; load request mapping
  (final = start + change); confirm gating (online + primary store); saved-flag transition;
  unlock-runs-once-on-exit; reset semantics; zero-change path.
- Instrumented tests: review shows changes + totals; confirm offline blocked; successful save →
  summary; exiting summary unlocks and returns home; resume-at-summary when already saved.

## Non-goals
- No partial/streaming save. No manual re-lock. Storage receipts history viewing is not required
  (no in-app screen exists for it).

## Depends on
Task 07 (changes) and Task 03 (printer connector pattern; the storage print builder is separate
but reuses the same connect-then-print flow).

## RN reference paths
- `resources/frontend/screens/storage/review/ReviewStorageChanges.tsx`,
  `useReviewStorageChangesData.ts`, `review/ReviewExpirationItem.tsx`
- `resources/frontend/screens/storage/summary/StorageChangesSummary.tsx`,
  `useStorageChangesSummaryData.ts`, `summary/createPrintStorageChanges.ts`
- `resources/frontend/api/mutations/useSaveSelectedItems.ts`, `useDeselectStore.ts`,
  `api/request-mappers/mapSaveSelectedItemsRequest.ts`
- backend `app/Http/Controllers/StorageController.php` (`load`, `unlock_from_user`)
