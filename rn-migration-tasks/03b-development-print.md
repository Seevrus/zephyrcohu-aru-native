# Task 03b · Print: dev-mode printer (save print string for review)

## Goal
Let a **dev-mode user** (per the logged-in user's `isDev` flag) select a virtual "Dev Test"
printer in Print Settings that, instead of sending anything over Bluetooth, prompts the user to
save the generated receipt print-string to a file on the device — so the exact text a real
Datecs printer would receive can be reviewed without physical hardware.

## Business context
This is a **greenfield feature for the native app**, not a port of working RN behavior: the RN
app only ever gated its "Dev Test" pseudo-device on a build-time env var
(`EXPO_PUBLIC_IS_DEV === 'yes'`) and never actually implemented anything for it beyond adding it
to the paired-device list — selecting it and printing had no defined behavior. That's the tech
debt this task fixes properly: gate on the *logged-in user's* dev-mode flag (so it works the same
regardless of build type), and give the option a real, useful behavior (save-to-file for review)
instead of a no-op.

## Starting / resulting state
- **Start**: the native app already has unfinished scaffolding for this, left over from earlier
  work, that this task must correct rather than build from scratch:
  - `BluetoothHelper.getPairedDevices()` (`printer/BluetoothHelper.kt`) appends a "Dev Test"
    device (address `"00:00:00:00:00:00"`) to the paired-device list whenever
    `BuildConfig.DEBUG` is true — **wrong gate**, unrelated to the logged-in user.
  - `BorealPrinter.connect()` already short-circuits to `Result.success(Unit)` for address
    `"00:00:00:00:00:00"` (keep this as-is; connecting to the dev printer should keep succeeding
    instantly).
  - `BorealPrinter.printTaggedText()` already special-cases "no real socket" by just
    `Log.d`-ing the text and returning success — **wrong behavior**, must become a real
    save-to-file prompt instead of a log line.
  - `PrintSettingsViewModel` has no notion of the logged-in user at all.
- **Result**: `BluetoothHelper.getPairedDevices()` (or its caller) shows the Dev Test device when
  `BuildConfig.DEBUG` **or** the current user's `isDev` is `true` (logical OR — either condition
  is sufficient, kept for local-dev convenience so a debug build always has the option regardless
  of the logged-in user); printing to it (from `PrinterConnector`/`PrintSection`, Task 03) opens
  the system "Save As" file picker with the receipt text as content and a suggested filename, for
  any print flow that reaches a connected dev printer (sell receipts today; storage and
  reprint/storno flows automatically once they reuse `PrintSection`).

## Data requirements
- Current user's `isDev: Boolean` (`domain/model/User.kt`, already present, sourced from
  `UserRepository.getCurrentUser()` — the check-token response). No new DTO/model field needed.
- Receipt metadata needed to name the file: partner name, receipt/invoice date, and the receipt's
  serial number — all already available wherever `PrintSection`/`PrintReceiptTextBuilder` are
  invoked (Task 03).

## Behavioral requirements
- **Gating**: extend the `BuildConfig.DEBUG` check in `BluetoothHelper.getDevTestDevice()`'s
  caller with an OR against the current user's `isDev` flag — pass it in as a parameter (from
  `PrintSettingsViewModel`, which reads `userRepository.getCurrentUser()` following the existing
  pattern used by `SettingsViewModel`/`SelectPartnerViewModel`/etc.). Keep `BuildConfig.DEBUG` as
  one of the two conditions (convenience for local development, independent of which user is
  logged in); a dev-mode user must additionally see the option in **release** builds too.
- **Connect**: no behavior change — address `"00:00:00:00:00:00"` keeps succeeding immediately
  without a real Bluetooth socket.
- **Print → save flow**: when `BorealPrinter.printTaggedText()` is called while connected to the
  dev address, instead of logging the text it must produce a result the UI layer can act on to
  launch Android's `ACTION_CREATE_DOCUMENT` system file picker (SAF), pre-filled with the
  suggested filename below and `text/plain` MIME type, writing the exact tagged text as UTF-8
  content once the user confirms a location. If the user cancels the picker, treat it as neither
  success nor failure requiring a retry-blocking error — `PrinterConnector` should return to the
  `CONNECTED` phase, not surface a printer error.
- **Copy count is ignored for the save flow**: `PrintSection`'s copy-count logic (2 for
  electronic/reprint, or the partner's `invoiceCopies` otherwise, per Task 03) exists to model
  physical printouts and must still compute/display normally for the UI's copy-count messaging,
  but the actual save-file action happens **once** per print action regardless of that count —
  the dev flow is for reviewing text content, not simulating N physical copies.
- **Filename**: `{partnerName}_{invoiceDate}_{receiptSerialNumber}.txt`, where `invoiceDate` is
  the receipt's date formatted `yyyy-MM-dd` (matching the backend's own receipt date format, per
  the shared conventions in `00-overview.md`). Storno/cancel prints use the exact same template —
  a cancel receipt already has its own distinct serial number, so no special-casing is needed
  (no `(or cancel receipt nr)` branch beyond "it's a different receipt with a different serial
  number, same template applies").
- Ad-hoc partners (no partner record) should fall back to whatever partner display name
  `PrintReceiptTextBuilder`/the print section already resolves for the receipt body's buyer block,
  for consistency.

## API requirements
None. Purely local: reads the already-cached current user and already-built receipt text.

## UI/UX notes
- Reuse `PrinterConnector`/`PrintSection` (Task 03) unchanged from the caller's perspective — this
  task only changes what happens *inside* `BorealPrinter`/`BluetoothHelper` when the selected
  device is the dev one, plus the small gating addition in `PrintSettingsViewModel`.
- The system Save-As picker is Android's own UI; no custom dialog/screen needed. `PrintSettings`
  screen's device dropdown needs no visual change beyond the dev device conditionally appearing/
  disappearing based on the current user.
- No new Hungarian copy is required beyond what already exists for `PrinterConnector`'s
  connect/print states, unless the SAF launch itself needs to surface a transient error (e.g.
  no app available to handle `ACTION_CREATE_DOCUMENT`, which is effectively unreachable on
  stock Android) — don't add handling for that unreachable case.

## Testing requirements
- Unit test: `BluetoothHelper.getPairedDevices()` (or its refactored equivalent) includes the Dev
  Test device when `BuildConfig.DEBUG` is true regardless of `isDev`, when `isDev = true`
  regardless of `BuildConfig.DEBUG`, and excludes it only when both are false.
- Unit test: `PrintSettingsViewModel` surfaces the dev device in its device list when
  `userRepository.getCurrentUser()` reports `isDev = true`, even on a non-debug build.
- Unit test: `BorealPrinter.printTaggedText()` for the dev address returns a result carrying the
  text + suggested filename for the caller to act on, rather than a plain success with no payload.
- Unit test: filename generation from partner name / receipt date / serial number, including a
  storno receipt's own serial number producing a distinct, correctly-formatted filename.
- Instrumented test (may stub the SAF launcher): selecting the Dev Test printer and printing opens
  the save flow instead of attempting a Bluetooth connection/print.

## Non-goals
- No change to real Bluetooth/Datecs transport behavior for non-dev printers.
- No change to copy-count logic or messaging itself (Task 03) — only whether the save action
  repeats per copy (it doesn't).
- No server-side concept of "dev print" — this is 100% client-local behavior gated on the
  already-synced `User.isDev` field.

## Depends on
Task 03 (Sell · print sales receipt) for `PrinterConnector`/`PrintSection`/
`PrintReceiptTextBuilder` to exist and be the thing this task's save-flow hooks into. Benefits
Task 05 (reprint/storno) and Task 08 (storage print) automatically once they reuse `PrintSection`,
with no changes needed on their part.

## RN reference paths
- `resources/frontend/screens/start-page/PrintSettings.tsx` (the `EXPO_PUBLIC_IS_DEV` /
  `isDevelopment` gate and `Dev Test` / `127.0.0.1` pseudo-device — reference for *what RN did*,
  not a pattern to copy; RN never implemented real dev-print behavior beyond the list entry).
- `resources/frontend/api/response-types/common/UserType.ts` (`isDev` field on the user, source
  of truth for the correct native gate).
- native `printer/BluetoothHelper.kt` (`getPairedDevices`, `getDevTestDevice`),
  `printer/BorealPrinter.kt` (`connect`, `printTaggedText`, the `"00:00:00:00:00:00"` mock
  branches), `ui/screens/PrintSettingsViewModel.kt`.
