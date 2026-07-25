# Task 03c · Receipts store: ordering, scaling & draft-write correctness

## Goal
Make `ReceiptsStore` and `ReceiptDao` safe to build a **list screen** on top of, before Task 04
does exactly that. Three separate problems: the receipt query has no defined order despite two
call sites depending on order, the store keeps every receipt fully deserialized in memory forever
and redecodes the whole table on every write, and the sell-flow draft is persisted with a
write-per-keystroke that has a real (if rare) lost-update race.

## Business context
Nothing user-facing changes. This is the load-bearing cleanup that Task 04 (receipts list) and
Task 09 (round history) both sit on. Today N (receipts per round) is small enough that all three
problems are invisible; Task 04 turns `ReceiptsStore.receipts` into the backing flow for a list
screen, at which point they stop being invisible. Doing this after Task 04 means doing it while a
screen depends on the current behavior.

## Starting / resulting state

**Start:**
- `ReceiptDao.getAllReceipts()` is `SELECT * FROM receipts` with **no `ORDER BY`**.
  `SummaryViewModel` (`init`, the `combine` block) calls `receipts.lastOrNull()` to mean "the
  receipt that was just created". This currently works only because `ReceiptEntity.id` is an
  `INTEGER PRIMARY KEY` (a rowid alias) and SQLite happens to return an ascending rowid scan —
  unspecified behavior that a future index or query-planner change can break silently.
- `ReceiptsStore.receipts` is `stateIn(scope, SharingStarted.Eagerly, emptyList())` over
  `getAllReceipts().map { it.map(ReceiptEntity::toDomain) }`. Every `ReceiptEntity.toDomain()`
  runs **six** `kotlinx.serialization` decodes (vendor, buyer, user, items, otherItems,
  vatAmounts). So `markReceiptPrinted(id)` — a single-column `UPDATE` — redecodes the entire
  table, and `Eagerly` pins the fully-decoded list in memory for the process lifetime whether or
  not anything is subscribed. Every other `stateIn` in the codebase (`ErrandsViewModel`,
  `MainViewModel`, `SelectItemsViewModel`, `SelectPartnerViewModel`) uses
  `SharingStarted.WhileSubscribed(5000)`; `ReceiptsStore` is the lone outlier.
- `ReceiptsStore.persistDraftState()` captures a snapshot synchronously, then fires an
  independent `scope.launch { dataStore.edit { … json.encodeToString(snapshot) } }`. It is called
  from `upsertSelectedItem` / `upsertOrderItem` / `setCurrentReceipt` / `setCurrentOrder` /
  `updateCurrentReceipt` / `setOtherItemSelections` — i.e. **once per quantity change** on Select
  Items. Each call serializes the whole draft (all items + all discounts) and writes to disk.
- `ReviewItemsViewModel.nextAvailableSerialNumber` reads `receiptsStore.receipts.value`, a flow
  the store's own KDoc documents as lagging behind `addReceipt`, and scans it with `maxOf`.

**Result:** a defined query order, a store that scales to a list screen, a draft persister with a
single serialized writer, and serial-number allocation that reads the source of truth.

## Data requirements
No new fields, DTOs, or domain models. `ReceiptEntity` / `Receipt` shapes are unchanged. No Room
schema change and therefore **no database version bump** — `ORDER BY` and new read-only queries
do not alter the schema.

## Behavioral requirements

### 1. Defined receipt ordering
- Give `getAllReceipts()` an explicit `ORDER BY`. Prefer newest-first (`ORDER BY id DESC`) since
  that is what Task 04's list will want; whichever direction is chosen, it must be explicit and
  the two consumers updated to match.
- `SummaryViewModel` must stop inferring "the receipt I just created" from list position. It
  already knows which receipt it created — the sell flow allocated the serial number. Hold that
  identifier and look the receipt up by it, so Summary is correct regardless of query order and
  regardless of what else lands in the table.

### 2. Store scaling
- Change `ReceiptsStore.receipts` to `SharingStarted.WhileSubscribed(5000)`, matching every other
  `stateIn` in the codebase. Verify the two existing one-shot `.value` reads
  (`PrintSectionViewModel.initialize`, `ReviewItemsViewModel.nextAvailableSerialNumber`) still
  behave — note that requirement 3 below removes the second one, and `WhileSubscribed` means
  `.value` can be the `initialValue` when nothing is subscribed, so any surviving `.value` read
  must be re-examined rather than assumed safe. This interacts with Task 03d's
  `PrintSectionViewModel` rework; sequence accordingly.
- Add a **projection query** for list consumers that does not decode the item/vatAmount blobs —
  Task 04's list needs serial number, partner name, date, gross total and `isPrinted`, not the
  full item breakdown. A `@Query` returning a small `data class` (Room supports POJO projections)
  keeps the list screen O(rows) in cheap columns instead of O(rows × 6 JSON decodes).
- Full-receipt decoding should happen only where the full receipt is actually needed (print,
  details).

### 3. Serial-number allocation reads the database
- Replace the in-memory `maxOf` scan with a DAO query
  (`SELECT MAX(...)` over `serialNumber` and `cancelSerialNumber`) executed as a suspend read at
  allocation time. This is both correct — it cannot observe a stale flow — and O(1) instead of
  O(N).
- Keep the existing backend duplicate-guard retry (commit 24d0ecf) as a belt-and-braces measure;
  it stops being the primary defense but should not be removed.

### 4. Draft persistence: one writer, debounced
- Replace the per-mutation `scope.launch { dataStore.edit { … } }` with a **single** collector:
  hold the draft snapshot in a `MutableStateFlow`, and in `init` launch one
  `snapshot.debounce(…).collectLatest { persist(it) }`.
- This fixes two things at once. **Write amplification**: serialization + disk write becomes
  proportional to settled state, not keystrokes. **Lost update**: today each mutation captures its
  snapshot synchronously but writes from an independent coroutine, and coroutine dispatch order is
  not FIFO-guaranteed, so a stale snapshot can complete last and win — silently discarding the
  user's most recent edit on process death. A single collector makes ordering structural rather
  than incidental.
- Pick the debounce interval deliberately and state it in a comment: it is the maximum window in
  which a process death loses draft edits. Something in the 200–500 ms range is the usual
  trade-off; whatever is chosen, `resetReceipts` / `resetSellFlowDraft` must **not** be debounced —
  a clear must land immediately and must not be overtaken by an in-flight stale snapshot.

## API requirements
None. Entirely local — Room and DataStore only. No backend contract is touched.

## UI/UX notes
No visual changes. `SummaryScreen` renders the same content; the change is only how
`SummaryViewModel` identifies which receipt to render.

## Testing requirements
- Unit: `ReceiptDao` returns receipts in the specified order (insert out of id order, assert the
  contract holds) — an instrumented Room test, since this asserts SQLite behavior.
- Unit: `SummaryViewModel` surfaces the correct receipt when the table contains other receipts
  ordered ahead of it — this test must fail against the current `lastOrNull()` implementation.
- Unit: the projection query returns the list-screen fields without materializing item lists.
- Unit: serial-number allocation returns the right next number immediately after `addReceipt`,
  i.e. in the exact window where the in-memory flow is documented as stale. This is the
  regression test for the bug the backend retry currently papers over.
- Unit: draft persistence writes once (not N times) for a burst of N rapid `upsertSelectedItem`
  calls, and the persisted value is the **last** snapshot — use `runTest` virtual time to advance
  past the debounce.
- Unit: `resetSellFlowDraft` clears persisted state even when a debounced write is in flight, and
  the in-flight stale write does not resurrect the cleared draft.

## Non-goals
- No change to the `ReceiptEntity` column types / DTO-as-storage question — that is Task 12, and
  deliberately out of scope here to keep this task migration-free.
- No new UI, no receipts-list screen — that is Task 04, which this task exists to unblock.
- No change to what `addReceipt` / `markReceiptPrinted` mean semantically.

## Depends on
Task 01b (Room-backed receipts) and Task 01c (persisted draft) for the code under change.
Interacts with Task 03d (which reworks `PrintSectionViewModel`'s `.value` read) — if both are
done, sequence 03c's `WhileSubscribed` change after 03d's rework, or resolve the `.value` read in
whichever lands second.

**Blocks Task 04** (receipts list) and, less directly, Task 09 (round history).

## RN reference paths
None — this task has no RN counterpart. It corrects native-only decisions made in Tasks 01b/01c.
Behavior visible to the user must not change, so the RN app is not a reference here; the existing
native tests are the contract.
