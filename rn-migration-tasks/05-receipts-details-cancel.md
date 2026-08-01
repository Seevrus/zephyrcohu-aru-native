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
- receipt details screen example: `./screenshots/05-01-receipt-details.png`
- receipt details - canceled receipt: `./screenshots/05-04-canceled-receipt-details.png`
- cancel receipt - confirm alert: `./screenshots/05-02-receipt-cancel-confirm.png`
- successful cancel - `./screenshots/cancel-success.png`
- UX should include the error paths as well

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

## Status

Implemented. `ReceiptDetailsScreen`/`ReceiptDetailsViewModel` (`ui/screens/`) add a
`receipt_details/{serialNumber}` route wired from `ReceiptsListScreen`'s row tap. Reprint reuses
`PrintSection(isCancel = receipt.isCanceled, receiptSerialNumber = receipt.serialNumber)` as-is,
matching Task 03's design intent. Storno calls the already-existing (previously unused)
`ReceiptsRepository.cancelReceipt` / `ReceiptApiService.cancelReceipt` /
`CreateCancelReceiptDataDto` plumbing, upserts the response via `ReceiptsStore.addReceipt`
(REPLACE-by-`id`, which also resets `isPrinted` — intentional, since the storno document is a new
print obligation), and restores canceled quantities via
`StoreSessionStore.updateExpirationQuantity`.

One correction from this task doc during implementation: the cancel response is reconciled by
`id` (Room `insertReceipt` REPLACE), not by `serialNumber`/`yearCode` — the backend's
`UpdateReceiptRequest` validates and matches by `id` (`CreateCancelReceiptDataDto.id`), matching
what the request already sends.

One gap resolved during implementation: `ReceiptItem` (the persisted line item) has no
`expirationId`, unlike the RN reference's `StoreDetailsResponseData.expirations` matching. Both
`ReceiptItem` and `StoreDetailsExpiration` carry `articleNumber` + `expiresAt`, so quantity
restoration groups canceled receipt items by that pair (summing duplicates) and matches against
`StoreSessionStore`'s current expirations on the same pair before calling
`updateExpirationQuantity(itemId, expirationId, +quantity)`.

`Receipt.isCanceled` (`cancelSerialNumber != null && cancelYearCode != null`) was added as a
derived property on the domain model, reused instead of `ReceiptsListViewModel`'s local copy.

Correction after initial implementation: the screen originally had a `BorealTopAppBar` back-arrow
icon button (`arrow_back` drawable + `IconButton`) wired to `navController.popBackStack()`. This
was removed — the app relies on Android's system back gesture/button for navigation, not in-app
back-arrow affordances (no other screen has one; screens that take an `onNavigateBack` callback,
e.g. `DiscountsScreen`, invoke it after completing an action, never from a back-icon tap). The
screen no longer takes any navigation callback; `ReceiptDetailsRoute` just hosts the ViewModel.
The `arrow_back.xml` drawable and its string resource were deleted as unused.

Unit test coverage (`ReceiptDetailsViewModelTest`) and instrumented coverage
(`ReceiptDetailsScreenContentTest`, 15 cases) are both complete and passing on-device;
`ktlintFormat` and `detekt` are clean.

Writing `ReceiptDetailsScreenContentTest` surfaced (and this task fixed) a pre-existing gap in
the instrumented test harness: any Compose test embedding the shared `PrintSection` (Task 03)
calls `hiltViewModel()`, which needs a Hilt-enabled host activity — but every `*ScreenContentTest`
used a plain `createComposeRule()`, so `SummaryScreenContentTest`'s receipt-bearing cases were
silently broken from Task 03 onward (confirmed by running it before this fix — same crash). Fixed
project-wide, not just locally:
- Added the `hilt-android-testing` dependency, `HiltTestRunner` (boots `HiltTestApplication`),
  and pointed `testInstrumentationRunner` at it (`build.gradle.kts`).
- Added `ComposeContentTestActivity`, a bare `@AndroidEntryPoint` activity with no `setContent`
  of its own, so `createAndroidComposeRule` can host arbitrary composables under test. It lives
  in `src/debug/`, not `src/androidTest/` — a self-instrumenting test's `ActivityScenario`
  resolves activities against the app-under-test's own package, and an activity declared only in
  the androidTest manifest sits in a different package (`<applicationId>.test`), which fails to
  resolve at runtime.
- Converted `SummaryScreenContentTest`, `ReceiptDetailsScreenContentTest`, and `MainActivityTest`
  (the three files using `createAndroidComposeRule`) to `@HiltAndroidTest` +
  `HiltAndroidRule`/`RuleChain`, required once `HiltTestApplication` is in play.
- Full instrumented suite (99 tests) and full unit suite verified green after the change.

Any future screen test embedding `PrintSection` (or any other `hiltViewModel()` consumer) should
follow the same pattern: `createAndroidComposeRule<ComposeContentTestActivity>()` + `@HiltAndroidTest`
+ `HiltAndroidRule` via `RuleChain`.
