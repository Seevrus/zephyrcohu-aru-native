# Task 05 · Receipts: details, reprint & storno (cancel)

## Goal
Open a single receipt to reprint it and, when eligible, cancel it (storno) — which creates a
cancel receipt on the server, marks the local receipt as canceled, restores the sold quantities
back into the round store, and lets the user print the storno document.

## Business context
From the receipts list, a receipt opens to a details screen titled `serialNumber/yearCode`. The
user can reprint it (reusing the shared print section from Task 03). If the receipt is not
already canceled and the device is online, the user can **storno** it: this is an irreversible
action that produces a new cancel receipt (with the next available serial number and the store's
year code), reverses the inventory movement (adds the sold quantities back to the round store),
and thereafter the receipt prints as a **storno** document. Ad-hoc and listed partners both
supported.

## Starting / resulting state
- **Start**: receipts list (Task 04) exists; shared print section (Task 03) exists; storno
  endpoint is unused by the native app.
- **Result**: a details screen with reprint + conditional storno; after storno the receipt shows
  as canceled, the round store quantities are corrected, and the print switches to the storno
  layout.

## Data requirements
- Look up the receipt by `serialNumber` from the receipts store.
- "Canceled" = `cancelSerialNumber` and `cancelYearCode` both present.
- Next available serial number: `max(serialNumber, cancelSerialNumber) + 1` across all local
  receipts (reuse the same rule as finalization). Year code comes from the current round store's
  `yearCode`.
- On storno success, update the local receipt with the returned `cancelSerialNumber`/
  `cancelYearCode` and clear any "needs update" flag; add each canceled line's quantity back to
  the matching item+expiration in the current round store state.

## Behavioral requirements
- Header shows `serialNumber/yearCode`. Subtitle "Számla nyomtatása" or, if canceled,
  "Stornó számla nyomtatása".
- Reprint via the shared PrintSection with `isCancel = receipt is canceled`.
- Storno button visible only when: not already canceled, online, and the receipt has a server id.
  Confirmation dialog ("Számla sztornózása" / irreversible warning) before proceeding.
- Offline: hide/disable storno and show "Internetkapcsolat hiányában a számla nem stornózható."
- If the receipt was canceled in a previous session show "Ezt a számlát korábban stornózták.";
  right after a successful storno show "Sikeres stornózás!".
- Error handling: dev users may see raw error detail; normal users see a generic Hungarian
  message ("Váratlan hiba lépett fel a számla sztornózása során.").
- Missing round-store state blocks storno with a clear message.

## API requirements
- `POST /receipts/cancel` (`ReceiptController@update_receipts`), Sanctum bearer +
  `X-Device-Id`, `{ "data": [ <cancel-receipt-request> ] }`. The cancel request is built from
  the original receipt plus the new `cancelSerialNumber` and `cancelYearCode` (see RN
  `mapCreateCancelReceiptsRequest`). Response is the standard receipts response; match rows by
  `serialNumber` + `yearCode` to update local state.

## UI/UX notes
- Reuse `BorealButton` (warning variant for storno), `BorealAlert`, `ErrorCard`,
  `InfoCard`/`TextCard`, and the Task 03 PrintSection. Keyboard-aware scroll container. Header
  title set dynamically to the receipt number.

## Testing requirements
- ViewModel unit tests: canceled derivation; storno eligibility (canceled / offline / missing id
  / missing store); next-serial computation; local receipt update + quantity restoration mapping;
  dev vs. non-dev error messaging.
- Instrumented tests: reprint shows correct (normal/storno) subtitle; storno gated by
  connectivity; confirm dialog; post-storno state (canceled label, storno print).

## Non-goals
- No hard-delete of receipts (`DELETE /receipts/{id}` is admin-only). No editing of receipt
  contents.

## Depends on
Task 04 (list entry point) and Task 03 (shared print section).

## RN reference paths
- `resources/frontend/screens/receipts/ReceiptDetails.tsx`, `useReceiptDetails.ts`
- `resources/frontend/api/mutations/useCancelReceipt.ts`,
  `api/request-mappers/mapCreateCancelReceiptsRequest.ts`
- backend `app/Http/Controllers/ReceiptController.php` (`update_receipts`)
