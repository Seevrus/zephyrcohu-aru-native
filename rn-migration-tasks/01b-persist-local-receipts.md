# Task 01b · Sell: persist local receipts across process death

## Goal
Make `ReceiptsStore.receipts` (the list of receipts created during the active round) durable
across app/process restarts, so serial-number derivation and end-of-round reporting stay correct
even if the app is killed and relaunched mid-round.

## Business context
Discovered while testing Task 01 (discounts): finalizing a receipt whose computed `serialNumber`
collides with one that already exists on the backend (same `company_id` + `serialNumber` +
`yearCode`) causes the backend's idempotent duplicate guard to silently skip creation and return
`{"data":[]}` (200 OK, empty body) — not an error. `ReceiptsRepository.createReceipt` crashed on
this (`.first()` on an empty list); that crash is already fixed (retry-with-next-serial-number,
bounded to 5 attempts). But the retry only papers over the symptom — the real problem is that
`nextAvailableSerialNumber()` (`ReviewItemsViewModel`) and `calculateLastSerialNumber()`
(`EndErrandViewModel.finishRound`) both derive their answer purely from
`receiptsStore.receipts.value`, which is a **bare in-memory `MutableStateFlow`**
(`store/receipts/ReceiptsStore.kt`) with no persistence at all. Any process death — app kill,
crash, OS memory pressure, not just an explicit reinstall — resets it to empty mid-round.

Checked the RN reference (`resources/frontend/hooks/useInitializeApp.ts`) to find the intended
parity behavior: RN **never fetches receipts from the server** on startup (its query list is
check-token/items/otherItems/partnerLists/partners/priceLists/stores/storeDetails only — no
receipts). RN's `receiptsSlice` instead survives restarts because the whole Redux store is
persisted (redux-persist) to durable storage. So the correct native parity fix is **local
persistence**, not a server sync — `rn-migration-tasks/00-overview.md`'s claim that "Room
persistence" already covers the receipts repository is inaccurate; only `items`/`partners`/etc.
repositories are Room-backed via `networkBoundResource`, `ReceiptsRepository` is not.

Consequence beyond the serial-number collision: `EndErrandViewModel.finishRound()` also reads
`receiptsStore.receipts.value` to build the round-finish report (`lastSerialNumber` and the full
`receipts` list sent to the backend). If the app restarts mid-round today, that report is built
from an empty list — silently wrong, not just a dev-testing inconvenience.

## Starting / resulting state
- **Start**: `ReceiptsStore.receipts` is an in-memory-only `MutableStateFlow<List<Receipt>>`;
  `addReceipt`/`resetReceipts` only touch memory. Every other domain repository that needs
  offline durability (items, partners, stores, ...) already uses a Room entity + DAO.
- **Result**: receipts created during the active round are written to a Room table as they're
  created and read back into `ReceiptsStore.receipts` on process start, so serial-number
  derivation and round-finish reporting are correct regardless of process death mid-round.

## Data requirements
- New `ReceiptEntity` (Room) mirroring the `Receipt` domain model, plus a `ReceiptDao`
  (`insert`, `getAll` as `Flow<List<ReceiptEntity>>`, `clear`) — follow the exact pattern already
  established for other entities (e.g. `ItemEntity`/`ItemDao`,
  `data/local/converters/ItemConverters.kt` for the `TypeConverter`-via-kotlinx.serialization
  approach needed for the nested `buyer`/`vendor`/`items`/`otherItems`/`vatAmounts` fields).
- Register the entity + DAO on `BorealDatabase` with a schema version bump.

## Behavioral requirements
- `ReceiptsStore.receipts` becomes Room-backed: `addReceipt` persists via the DAO (in addition to
  or instead of the in-memory update — keep the `StateFlow` public API unchanged so no call site
  needs to change); the store's initial value is populated from Room on construction/first
  collection.
- `resetReceipts()` clears the Room table too (it already clears everything else on: new errand
  start, and — flag this, don't fix it here — also on canceling an in-progress draft receipt via
  `ReviewItemsViewModel.cancelReceipt`, which appears to wipe the *entire* round's receipt
  history, not just the draft; worth a follow-up audit but out of scope for this task).
- No behavior change to `nextAvailableSerialNumber()`, `calculateLastSerialNumber()`, or the
  retry-on-collision logic in `ReceiptsRepository.createReceipt` — this task only fixes what data
  they read from.

## API requirements
None — purely local persistence, no new endpoints (matches RN, which has none for this either).

## UI/UX notes
None — no user-visible surface, this is a data-layer durability fix.

## Testing requirements
- `ReceiptsStoreTest` (new, if one doesn't exist): `addReceipt` persists and is readable after
  simulating a fresh store instance backed by the same (in-memory test) Room database; `receipts`
  reflects Room state, not just the in-memory update.
- Regression: existing `ReviewItemsViewModelTest`/`EndErrandViewModelTest` serial-number tests
  still pass unchanged (the store's public `StateFlow<List<Receipt>>` contract doesn't change).

## Non-goals
- No `GET /receipts` server sync (RN doesn't do this either — see business context).
- No fix for the `resetReceipts()`-on-cancel over-clear concern flagged above (separate audit).
- No UI for viewing receipts (Task 04).

## Depends on
Task 01 (where the gap was discovered). Independent of Tasks 04/05/10.

## RN reference paths
- `resources/frontend/hooks/useInitializeApp.ts` (confirms no server-side receipts sync exists)
- `resources/frontend/store/receiptsSlice.ts`, `store/store.ts` (redux-persist configuration)
- native: `store/receipts/ReceiptsStore.kt`, `ui/screens/ReviewItemsViewModel.kt`
  (`nextAvailableSerialNumber`), `ui/screens/EndErrandViewModel.kt` (`calculateLastSerialNumber`,
  `finishRound`), `data/local/converters/ItemConverters.kt` (pattern to mirror for new
  converters)
