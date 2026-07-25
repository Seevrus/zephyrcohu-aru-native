# Task 03d · Print path: state-machine hardening & ViewModel state discipline

## Goal
Close the dead-end states and stale-state hazards in the Task 03/03b print path before Task 05
reuses `PrintSection` for reprint and storno. Every issue here is either a latent bug that only
Summary's single-receipt, single-instance usage currently hides, or a mutable ViewModel field that
duplicates something already in UI state.

## Business context
`PrintSection` / `PrinterConnector` / `PrintSectionViewModel` were built for exactly one caller
(Summary, one receipt, one instance, printed once). Task 05 changes all four of those assumptions:
receipt details reuses `PrintSection(isCancel, receiptSerialNumber)` for both a reprint **and** a
storno, potentially for different receipts within one navigation entry. Task 08 reuses it again for
storage prints. The current code has guards that silently discard a receipt change and a phase that
can strand the print button permanently disabled — both harmless with one receipt and one print,
both live bugs the moment the component is genuinely reused.

## Starting / resulting state

**Start** — five defects and four state-discipline problems, all in `ui/components/PrinterConnector*.kt`,
`ui/components/PrintSection.kt`, `ui/screens/PrintSectionViewModel.kt`, `printer/BorealPrinter.kt`:

1. **`PRINTING` is a dead-end.** `PrinterConnectorViewModel.print()` sets `phase = PRINTING`; the
   `PrintOutcome.SaveToFile` branch only sets `saveFileRequest` and never leaves `PRINTING`. Only
   `onSaveFileResult` returns to `CONNECTED`. But `rememberLauncherForActivityResult` unregisters
   on dispose, so if the composition is disposed while the SAF picker is open the callback is
   dropped, the ViewModel survives (same nav entry), and the user is left with a permanently
   disabled "Nyomtatás…" button and no error — while `DisposableEffect`'s `onDispose` has already
   called `disconnect()`, so the printer really is gone.
2. **`initialize`'s guard contradicts its call site.** `PrintSection` uses
   `LaunchedEffect(isCancel, receiptSerialNumber)` — "re-run when the receipt changes" — but
   `PrintSectionViewModel.initialize` opens with `if (initialized) return`, silently discarding it.
   With Task 05 rendering reprint and storno for different receipts, the second render prints the
   first receipt's text.
3. **Dev-printer detection sniffs null state instead of the address.**
   `BorealPrinter.printTaggedText()` treats `printer == null && printerSocket == null` as "this is
   the dev mock". But `connect()` calls `disconnect()` first, which nulls both — so any real-printer
   connect that fails after that point leaves the object in a state where the next print opens a
   Save-As dialog instead of reporting "not connected".
4. **`"00:00:00:00:00:00"` is a magic literal**, duplicated in `BluetoothHelper` and
   `BorealPrinter` (plus 12 test sites).
5. **Receipt text is built on the main thread.** `PrintSectionViewModel.initialize` calls
   `PrintReceiptTextBuilder.build(...)` inside a bare `viewModelScope.launch { }`, which is
   `Dispatchers.Main.immediate`. For a 3-copy, 40-item invoice that is `copies × items × ~10`
   intermediate `String` allocations plus an `n log n` `Collator.compare` sort, on the UI thread,
   at the moment Summary opens.

Plus four mutable ViewModel fields that duplicate or gate UI state (see *ViewModel state
discipline* below).

**Result:** a print state machine with no unreachable-recovery states, a `PrintSection` that
honors a receipt change, explicit dev-printer identification, off-main-thread text building, and
no ViewModel `var` that shadows UI state.

## Data requirements
No new fields, models, or DTOs. `PrintSectionUiState` and `PrinterConnectorUiState` gain/lose
fields as the state-discipline work requires, but carry no new information.

## Behavioral requirements

### 1. `PRINTING` must not be a terminal state
- Leave `PRINTING` at the moment the `SaveToFile` request is emitted, not when the picker result
  arrives. The button being re-enabled while the system picker is on screen is harmless — the
  picker is a separate activity and the user cannot reach the button — and it makes the state
  machine independent of a callback that can legitimately be dropped.
- `onSaveFileResult` then only performs the write and fires the success callback; it no longer
  carries sole responsibility for un-sticking the UI.
- Same reasoning applies to any future async print outcome: no phase may depend on a callback
  from a component that unregisters on dispose.

### 2. `PrintSection` must honor a receipt change
- Replace the `initialized` boolean with a guard keyed on the actual inputs, so re-invoking
  `initialize` with a *different* `(isCancel, receiptSerialNumber)` re-resolves, while re-invoking
  with the *same* pair stays a no-op (the `LaunchedEffect` will re-fire on recomposition and must
  not re-run the network/DB reads).
- While reworking this, `PrintSectionViewModel` must remain correct when its
  `receiptsStore.receipts` read returns the `initialValue` — see Task 03c, which changes that flow
  to `WhileSubscribed`. Prefer resolving the receipt by an explicit lookup rather than a one-shot
  `.value` read of a shared flow.

### 3. Identify the dev printer explicitly
- Extract a single `const val` for the dev printer address, shared by `BluetoothHelper` and
  `BorealPrinter` (and referenced by the tests rather than re-literalled).
- `BorealPrinter` must decide "this is the dev printer" from the address it connected to — record
  it on successful `connect()` and clear it on `disconnect()` — not from whether its socket fields
  happen to be null. A real printer that is not connected must produce the "not connected" failure,
  never a save-to-file outcome.

### 4. Build receipt text off the main thread
- Wrap the `PrintReceiptTextBuilder.build(...)` call in `withContext(Dispatchers.Default)`.
- While there, two cheap wins inside the builder: `oneReceiptBody` is currently re-run from
  scratch per copy even though only the copies label differs between copies, and `huCollator` is a
  shared field on an `object` — `java.text.Collator` is **not thread-safe**, which stops being
  theoretical as soon as two `build()` calls can overlap (Task 05's reprint alongside Summary, or
  simply moving the call to a background dispatcher). Either confine the collator per call or use
  `CollationKey`s.

### 5. ViewModel state discipline
Four fields duplicate or gate something already represented in UI state and must go:

| Field | Problem |
|---|---|
| `PrintSectionViewModel.hasMarkedPrinted` | Second copy of `receipt.isPrinted` — two sources of truth for "printed" |
| `PrintSectionViewModel.initialized` | Gates UiState population while the call site's `LaunchedEffect` key implies re-init (this is defect 2) |
| `PrintSectionViewModel.currentReceipt` | Shadows whatever produced `uiState.receiptToPrint` |
| `PrinterConnectorViewModel.pendingOnPrintSuccess` | One half of a request whose other half (`saveFileRequest`) lives in UiState — the two can diverge |

Two fields are **fine and must be left alone**: `SummaryViewModel.hasLeft` and
`ReviewItemsViewModel.isOrderSubmitted`. Both are pure one-shot idempotence latches that duplicate
nothing and are never read by the render path.

The rule this task establishes, to be recorded in `00-overview.md`'s shared conventions so the
next ViewModel does not re-introduce the pattern:

> A `var` in a ViewModel is permitted **only** when it is a one-shot latch that (a) duplicates no
> value present in the UiState, (b) is never derivable from UiState, and (c) is never read by the
> render path. Anything else belongs in the UiState data class.

For `pendingOnPrintSuccess` specifically: a callback cannot live in a UiState data class (it
breaks equality and is not a value). The fix is not to move it but to stop holding it — the
success signal should be modelled as something the caller observes, so the ViewModel does not own
a lambda across an async boundary.

## API requirements
None. Entirely client-local.

## UI/UX notes
No intended visual change. The one observable difference is that the print button is enabled
rather than disabled while the SAF picker is open — acceptable and, per requirement 1,
deliberate. No new Hungarian copy.

## Testing requirements
- Unit: after a `SaveToFile` outcome is emitted, the phase is **not** `PRINTING` — this test must
  fail against the current implementation.
- Unit: a `SaveToFile` request that never receives an `onSaveFileResult` still leaves the
  connector in a usable state (the dead-end regression test).
- Unit: `PrintSectionViewModel.initialize` with a *different* `(isCancel, receiptSerialNumber)`
  re-resolves and produces the new receipt's text; with the *same* pair it does not re-run its
  reads. Both directions matter — the first is the Task 05 bug, the second prevents a
  recomposition loop.
- Unit: `BorealPrinter.printTaggedText()` on a real (non-dev) address with no live socket returns
  the "not connected" **failure**, not a `SaveToFile` outcome.
- Unit: `PrintSectionViewModel` marks the receipt printed exactly once across repeated success
  callbacks, with `hasMarkedPrinted` gone — i.e. the idempotence survives removing the field.
- Existing `PrintReceiptTextBuilderTest` must pass unchanged: the output string is a contract and
  none of this task's changes may alter a single character of it. This is the guard rail for the
  builder refactor in requirement 4.
- Instrumented: `PrintSectionContentTest` / `PrinterConnectorContentTest` re-verified after the
  signature changes (single connected device only — see `CLAUDE.md` ADB safety).

## Non-goals
- No change to the receipt text itself. The `PrintReceiptTextBuilder` output is byte-for-byte
  frozen; requirement 4 is purely about *where* and *how many times* it is computed.
- No change to real Bluetooth/Datecs transport behavior, connect semantics, or the copy-count
  logic.
- No change to the dev-print save-to-file feature's user-visible behavior (Task 03b) beyond the
  phase timing in requirement 1.
- Not a rewrite of `PrinterConnectorViewModel`'s connect/error classification — that works.

## Depends on
Tasks 03 and 03b for the code under change. Interacts with Task 03c (`ReceiptsStore.receipts`
becoming `WhileSubscribed` affects `PrintSectionViewModel`'s one-shot `.value` read) — whichever
lands second must resolve that read.

**Should precede Task 05** (reprint & storno), which is where defects 1 and 2 stop being latent.
Task 08 (storage print) benefits automatically.

## RN reference paths
None for the defects — these are native-only regressions in native-only scaffolding. RN's
`PrinterConnector.tsx` reducer remains the reference for the *connect/print phase model* itself
(which is not changing), not for the SAF/save-to-file path, which RN never implemented.
