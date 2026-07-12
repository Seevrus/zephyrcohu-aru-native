# Task 04 · Receipts: list of receipts

## Goal
Show the salesperson a list of the receipts created during the current session/round, each
tappable to open its details, with canceled (storno) receipts visually distinguished.

## Business context
Receipts created in the sell flow are persisted locally. The "Számlák" (receipts) main-screen
tile opens a list of them. Each row shows the buyer (delivery name), the receipt number
`serialNumber/yearCode`, and the gross total; a storno'd receipt is highlighted and additionally
shows its cancel number `cancelSerialNumber/cancelYearCode` with a "Sztornózva" label.

## Starting / resulting state
- **Start**: receipts are persisted (`ReceiptsStore`/`ReceiptsRepository` exist and are written
  during finalization) and the receipts main-screen tile already computes an enabled/disabled
  state, but there is **no list screen** to navigate to.
- **Result**: tapping the receipts tile opens a receipts list; rows render buyer, number and
  total; canceled rows are highlighted; tapping a row navigates to receipt details (Task 05).

## Data requirements
- Read all persisted receipts from the receipts store. No network fetch is required to view the
  list (it reflects locally-held receipts for the active session).
- Row fields: `buyer.deliveryName`, `serialNumber`, `yearCode`, `grossAmount`,
  `cancelSerialNumber`, `cancelYearCode`.

## Behavioral requirements
- Register the list screen and wire the receipts tile to open it (the tile's enabled/disabled
  logic already exists — just complete the navigation target).
- Order/keying: list keyed by serial number; preserve insertion/creation order as in RN
  (no extra sorting).
- Canceled receipt = both `cancelSerialNumber` and `cancelYearCode` present → highlighted
  background (storno color `#a951f3`) and a second row showing the cancel number + "Sztornózva".
- Empty list renders an empty screen (no crash).
- Tapping a row opens details for that `serialNumber`.

## API requirements
- None for this screen. (Startup sync that populates receipts from `GET /receipts` is covered by
  Task 10; this screen only reads local state.)

## UI/UX notes
- Reuse list/card styling consistent with other Boreal lists; `formatPrice` equivalent for the
  total; neutral card background, storno highlight per palette. Header "Számlák".

## Testing requirements
- ViewModel/state unit test: exposes receipts from the store; canceled-vs-active derivation.
- Instrumented tests: list renders rows with correct fields; canceled row shows highlight +
  cancel number; tapping a row navigates with the right serial number; empty state renders.

## Non-goals
- No server-side date/type filtering (that exists only in the admin web UI). No details/cancel/
  print here (Task 05).

## Depends on
Task 03 (details screen it links to needs the shared print section). Receipts data layer exists.

## RN reference paths
- `resources/frontend/screens/receipts/ReceiptList.tsx`, `ReceiptListItem.tsx`
- `resources/frontend/store/receiptsSlice.ts`, `store/hooks/receipts.ts`
