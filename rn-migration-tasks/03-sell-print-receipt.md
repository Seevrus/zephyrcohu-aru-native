# Task 03 · Sell: print sales receipt (shared print section)

## Goal
Print a finalized sales invoice/receipt to the Datecs Bluetooth printer from the Summary screen,
producing the correct number of copies, tracking that the original was printed, and formatting
the receipt text from receipt + partner + company data. Deliver this as a **reusable print
section** that Task 05 (receipt details / storno reprint) also uses.

## Business context
Every finalized receipt must be printable as a paper document. The number of copies and the
"format" wording depend on the buyer's invoice settings: electronic-invoice partners
(`invoiceType == 'E'`) and any **re**print produce 2 copies ("elektronikus"); otherwise the
partner's configured `invoiceCopies` count ("papír alapú"). The customer keeps one copy. The
first successful print marks the receipt as printed so later prints are treated as copies. The
receipt body is a fixed textual layout (vendor block, buyer block, line items with quantities and
prices, VAT breakdown, totals, rounding, payment terms) built from local data.

## Starting / resulting state
- **Start**: Summary screen (Task 02, done) has a print slot but no printing; the Datecs printer
  module (`printer/BorealPrinter.kt`, `BluetoothHelper.kt`) and print settings already exist.
  **Correction**: `PrintEndErrandScreen.kt` is still a TODO stub with no printer call — it is
  *not* a working connect-then-print UI pattern to copy, despite earlier notes in this backlog.
  `PrintSettingsViewModel.kt` is the only screen currently driving `BorealPrinter.connect` (for a
  test print); that connect/error-handling shape is the closest existing reference, but the
  connect-then-print-N-times *flow* (RN's `PrinterConnector.tsx`) doesn't have a native precedent
  yet and needs to be built fresh here.
- **Result**: a reusable **PrintSection** composable + supporting logic that, given a receipt
  serial number and a cancel/normal flag, connects to the printer, prints the correctly-formatted
  receipt N times, and records the printed state. Summary hosts it for the just-created receipt.

## Starting point for Task 03

- **The print slot**: `SummaryPrintSectionSlot(receipt: Receipt?)`, a private composable near the
  bottom of `SummaryScreen.kt`, called from `SummaryScreenContent` right after the `SuccessCard`.
  Replace its body; it should end up looking like RN's `PrintSection` — taking `isCancel: Boolean`
  and `receiptSerialNumber: Int` (RN's actual prop shape) rather than a whole `Receipt`, so Task 05
  can reuse it identically for storno reprints. `SummaryViewModel.uiState.receipt` already exposes
  the just-finalized `Receipt` (read as `receiptsStore.receipts.value.lastOrNull()`) to source
  `receiptSerialNumber` from.
- **`Receipt.isPrinted` does not exist yet** on either the domain model
  (`domain/model/Receipt.kt`) or the Room entity (`data/local/ReceiptEntity.kt`) — this task must
  add it (plus the mapper in `data/mapper/ReceiptMapper.kt`). `BorealDatabase.kt` is at
  `version = 18` with `fallbackToDestructiveMigration(true)` (`di/DatabaseModule.kt:38`) and no
  exported schema, so bumping to `version = 19` is sufficient — no explicit `Migration` object
  needed, matching how prior schema bumps in this app were done.
- **Updating a persisted receipt's `isPrinted` flag**: `ReceiptDao` currently only has
  `getAllReceipts()`/`insertReceipt()`/`clearReceipts()` — add an update/upsert method for
  flipping the flag on the existing row (an `@Update` or a partial-update `@Query`), and a
  `ReceiptsStore` method that calls it, following the `addReceipt`/`resetReceipts` pattern already
  there.
- **Printer connect pattern to build from**: `BorealPrinter.connect(address): Result<Unit>` plus
  whatever print/text-send method it exposes (read the rest of the file — `connect` was the only
  part read while scoping Task 02) and `PrintSettingsViewModel.kt`'s usage of it for
  connect-state/error handling conventions. There is a `"00:00:00:00:00:00"` dev-mock address
  short-circuit in `connect` worth knowing about for tests.

## Data requirements
- Read the receipt (by serial number) from the receipts store, the matching partner (by buyer id)
  from the partners cache, and the logged-in user's company for the vendor block.
- Determine ad-hoc partner (empty `partnerCode`) vs. listed partner — printing must work for both
  (ad-hoc partners have no partner record; fall back to receipt buyer data).
- Persist an `isPrinted` flag (and honor the existing `originalCopiesPrinted` concept) on the
  receipt so copy-count logic and later reprints behave correctly.

## Behavioral requirements
- Copy count: `invoiceType == 'E'` **or** already-printed → 2; else partner's `invoiceCopies`.
  Show the format label ("elektronikus" / "papír alapú") and a Hungarian description stating how
  many copies print and that one is the customer's.
- If required data is missing (no user / receipt / partner-and-not-ad-hoc), show "Nem áll
  rendelkezésre minden adat a nyomtatáshoz." and disable printing.
- Printing flows through the existing printer connect UI (reuse the end-errand
  connect-then-print pattern); on print success, set `isPrinted` if it was not already set.
- Build the receipt text to match the RN `createPrintReceipt` layout for both normal and cancel
  (storno) variants — the same builder is reused by Task 05, so support the `isCancel` flag now.
- The builder must render discounted line amounts and the per-VAT-rate breakdown consistently
  with the finalized totals.

## API requirements
- None for printing. (If the RN app reports "original copies printed" back to the server, treat
  that as out of scope here unless an existing endpoint already supports it — the receipts
  response intentionally dropped that field, so keep it local.)

## UI/UX notes
- Reuse the printer connector component/pattern and `LoadingIndicator`. Hungarian copy from
  `PrintSection.tsx`. Monospace-style receipt text suitable for a 32/48-column thermal printer;
  match the RN column widths/formatting.

## Testing requirements
- Unit tests for the receipt-text builder: normal vs. cancel layout, ad-hoc vs. listed partner,
  multi-VAT-rate and discounted lines, copy-count logic (E / reprint / paper), and the format
  label.
- Unit test: `isPrinted` transitions on first successful print only.
- Instrumented test (may stub the printer transport): Summary shows the print section with the
  right copy count and description; print action invokes the printer and flips printed state.

## Non-goals
- No changes to Bluetooth/Datecs transport itself. No server sync of print counts.

## Depends on
Task 02 (Summary hosts the section). Reused by Task 05.

## RN reference paths
- `resources/frontend/components/print-section/PrintSection.tsx`, `usePrintSection.ts`,
  `createPrintReceipt.ts`
- `resources/frontend/containers/printer-connector/PrinterConnector.tsx`
- native `printer/BorealPrinter.kt`, `printer/BluetoothHelper.kt`, `ui/screens/PrintEndErrandScreen.kt`

## Status: ✅ Done

Implemented as a fully reusable component tree, all under `com.zephyr.boreal`:

- **`domain/utils/PrintReceiptTextBuilder.kt`** — pure Kotlin port of `createPrintReceipt.ts` +
  `usePrintSection.ts`'s copy-count logic. `copyCount(partner, receipt)`,
  `receiptFormatLabel(partner)`, and `build(isCancel, user, receipt, partner)` are the public API;
  everything else (vendor/buyer/delivery/metadata/item-list/VAT-list/totals blocks, `padLeftSide`/
  `padRightSide`, the 67-char row-width contract, the STORNO/negated-amounts cancel branch, the
  multi-copy concatenation) is a line-for-line port of the RN source, including its literal
  Hungarian body text (kept as constants in this file, not `strings.xml`, matching RN's own
  choice to keep receipt-body text out of its i18n layer).
- **`ui/components/PrinterConnector.kt` + `PrinterConnectorViewModel.kt`** — the reusable
  connect-then-print state machine (`IDLE → CONNECTING → CONNECTED → PRINTING`), a faithful port
  of `PrinterConnector.tsx`'s reducer including the "timeout" → "A nyomtató valószínűleg nincs
  bekapcsolva." and "Broken pipe" → same-message-plus-disconnect special cases. Reads the selected
  printer address from the existing `PrintSettingsStore` and drives `BorealPrinter.connect`/
  `printTaggedText`/`disconnect` directly — this is the **first real caller** of those methods in
  the native app (previously only `PrintSettingsViewModel` used `BorealPrinter`, and only for
  device *selection*, not connection).
- **`ui/components/PrintSection.kt` + `ui/screens/PrintSectionViewModel.kt`** — the top-level
  reusable slot. Takes `isCancel: Boolean, receiptSerialNumber: Int` (RN's exact prop shape, not a
  whole `Receipt`), so Task 05 can mount `PrintSection(isCancel = true, receiptSerialNumber = ...)`
  unchanged for storno reprints. Not nav-scoped (it's an embedded composable, not a route), so it
  takes those params via an `initialize()` call (guarded to run once) from a `LaunchedEffect`
  rather than `SavedStateHandle`.
- **`Receipt.isPrinted: Boolean = false`** added to the domain model, `ReceiptEntity`, and
  `ReceiptMapper` (client-side-only, like RN's `ContextReceipt.isPrinted` — never sent to/read
  from the API). `BorealDatabase` bumped `version = 18 → 19` (no explicit `Migration`, per
  `fallbackToDestructiveMigration(true)`). New `ReceiptDao.updatePrintedState(id, isPrinted)` +
  `ReceiptsStore.markReceiptPrinted(id)`, called by `PrintSectionViewModel.onPrintSuccess()` only
  once per receipt (idempotent, mirrors RN's `printSuccessCallback` guard).
- **Summary wiring**: `SummaryScreen.kt`'s `SummaryPrintSectionSlot(receipt: Receipt?)` now renders
  `PrintSection(isCancel = false, receiptSerialNumber = receipt.serialNumber)` when a receipt
  exists, nothing otherwise — matches RN Summary's `{currentReceipt ? <PrintSection .../> : null}`.

**Confirmed during implementation:** `Receipt.items` (`ReceiptItem`) already comes pre-expanded
per discount by the time a receipt is finalized — each row has its own baked-in `netAmount`/
`vatAmount`/`grossAmount`/`discountName`, exactly one discount per row. So the item-list builder
in `PrintReceiptTextBuilder` just formats existing rows (sorted by name, HU collator) — it does
**not** re-run `DiscountCalculator`, avoiding a second implementation of the same math.

**Deliberate deviations from RN, both intentional:**
- The UI-facing copy-count description always uses the *correct* count (partner `null` → 3,
  matching `createPrintReceipt`'s own internal default) rather than reproducing a latent RN bug
  where `usePrintSection`'s displayed `numberOfPrints` is `undefined` for ad-hoc partners.
- `PrinterConnector` assumes Bluetooth permissions are already granted (a printer must already be
  selected in Print Settings — which itself gates on permissions — before this screen is
  reachable), so it has no permission-request UI of its own, unlike RN's `PrinterConnector.tsx`
  which handles that inline.

**Testing added:** `PrintReceiptTextBuilderTest` (copy-count branches, format label, cancel vs.
normal layout, rounding-line presence, discount-comment line, other-items, row-width contract),
`PrinterConnectorViewModelTest` (full state-machine coverage including both special-cased error
messages), `PrintSectionViewModelTest` (receipt lookup by serial number, `canPrint` branches
including the ad-hoc-partner case, idempotent `onPrintSuccess`), a `ReceiptsStoreTest` case for
`markReceiptPrinted`, and instrumented `PrintSectionContentTest`/`PrinterConnectorContentTest`
covering the stateless content composables (the real `PrinterConnector` is swapped for a stub
in `PrintSectionContentTest` via a new `printerConnector` slot param, since it resolves a Hilt
ViewModel that a plain `composeTestRule` can't provide). All passed on the connected emulator.
