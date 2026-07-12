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
- **Start**: Summary screen (Task 02) has a print slot but no printing; the Datecs printer module
  (`printer/BorealPrinter.kt`, `BluetoothHelper.kt`) and print settings already exist and are used
  by "print end errand".
- **Result**: a reusable **PrintSection** composable + supporting logic that, given a receipt
  serial number and a cancel/normal flag, connects to the printer, prints the correctly-formatted
  receipt N times, and records the printed state. Summary hosts it for the just-created receipt.

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
