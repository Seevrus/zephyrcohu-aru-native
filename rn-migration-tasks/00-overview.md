# RN → Native Migration: Remaining Task Backlog

This directory holds the **remaining business-functionality gaps** between the legacy
React Native app (`d:\zephyrcohu-aru`, source under `resources/frontend`) and the native
Kotlin/Jetpack Compose app (`d:\zephyrcohu-aru-native`).

Each task is a **deliverable increment**: it starts from a building, runnable native app and
leaves it building and runnable. Tasks are ordered so that dependencies come first. They
describe **business, data, behavioral, API and testing requirements** — *what* the app must
do, not *how* to code it. The goal is to **rebuild** each behavior idiomatically in Kotlin,
not to transliterate the RN implementation. Use the RN source as the behavioral reference of
record when a requirement is ambiguous.

## How to use a task file

A downstream agent should take one file, enter plan mode, read the referenced RN source for
exact edge cases, then produce an implementation plan. Files are self-contained but assume the
shared conventions below rather than repeating them.

## Current native frontier (as of writing)

Already built and working in the native app:

- **Auth / account**: login, token/password expiry handling, app-locked screen, change
  password, settings, printer settings + Datecs printer module (`BorealPrinter`, Bluetooth).
- **Main screen**: tiles with per-state enable/disable, round info, connectivity handling,
  garbage collection of stale cached data.
- **Errand (round) admin**: start errand, end errand. `PrintEndErrandScreen.kt` is currently a
  TODO stub (no Datecs call) — don't treat it as a working printer-UI pattern to copy from;
  `PrintSettingsViewModel.kt` is the only screen that actually drives `BorealPrinter.connect`
  today.
- **Sell flow through finalization, summary, and printing**: select partner (stored + tax-number
  search + add partner), select items (barcode, quantities), select other items, review items
  (including per-item discounts, Task 01), receipt + order creation to the API
  (`ReviewItemsViewModel` already builds totals, VAT breakdown, rounding, serial numbers,
  vendor/buyer, and calls `receiptsRepository.createReceipt` / `createOrders`), a terminal
  **Summary screen** (Task 02: success confirmation, offline notice, reset-on-exit), and a
  reusable **print section** (Task 03: `PrintSection`/`PrinterConnector` composables, Datecs
  connect-then-print flow, `PrintReceiptTextBuilder` receipt-text formatter, `Receipt.isPrinted`
  tracking) hosted on Summary and ready for reuse by Task 05's reprint/storno flow.
- **Data layer**: Retrofit API service + repositories (items, partners, receipts, rounds,
  stores, user), Room persistence (including Room-backed local receipts list, Task 01b),
  DataStore session/app-state (including the persisted sell-flow draft, Task 01c),
  connectivity observer.

## What is still missing (this backlog)

| # | Task | Area | Depends on | Status |
|---|------|------|-----------|--------|
| 01 | Sell · per-item discounts | Sell | frontier | ✅ Done |
| 01b | Sell · persist local receipts across process death | Sell / Data | 01 | ✅ Done |
| 01c | Sell · persist in-progress draft state across process death | Sell / Data | 01b | ✅ Done |
| 02 | Sell · summary screen & flow completion | Sell | 01 | ✅ Done |
| 03 | Sell · print sales receipt (shared print section) | Sell / Print | 02 | ✅ Done |
| 03b | Print · dev-mode printer (save print string for review) | Print | 03 | ✅ Done |
| 03c | Receipts store · ordering, scaling & draft-write correctness | Sell / Data | 01b, 01c | ✅ Done |
| 03d | Print path · state-machine hardening & ViewModel state discipline | Print | 03, 03b | Not started |
| 03e | Domain logic · de-duplication & Kotlin pattern cleanup | Sell / Infra | 01 | Not started |
| 03f | Persistence · separate cache from record, survive shape changes | Data | frontier | Not started |
| 04 | Receipts · list of receipts | Receipts | 03, 03c | Not started |
| 05 | Receipts · details, reprint & storno (cancel) | Receipts | 04 | Not started |
| 06 | Storage · select store (lock to user) | Storage | frontier | Not started |
| 07 | Storage · select items from store (batches, barcode, quantities) | Storage | 06 | Not started |
| 08 | Storage · review, save & summary (load + unlock + print) | Storage / Print | 07, 03 | Not started |
| 09 | Errand history · list rounds & round details | Errand history | frontier | Not started |
| 10 | Cross-cutting · startup data sync & stale-data refresh parity | Infra | frontier | Not started |
| 11 | Cross-cutting · app-update / version gate | Infra | frontier | Not started |

**Next up:** Tasks 03c–03f — a consolidation batch from the post-03b code review, scheduled ahead
of Task 04. They are independent of each other and none adds user-facing behavior:

- **03c (blocks Task 04)** — `ReceiptDao.getAllReceipts()` has no `ORDER BY` while two call sites
  depend on order; `ReceiptsStore.receipts` is `Eagerly`-shared and redecodes the whole table on
  every write; the sell-flow draft is persisted per-keystroke with a lost-update race. Task 04
  turns that flow into a list screen's backing data, so this is the one to do first.
- **03d (should precede Task 05)** — the print state machine can strand `PRINTING` permanently,
  `PrintSectionViewModel.initialize` silently discards a receipt change, and the dev printer is
  identified by sniffing null fields rather than by address. All latent while Summary is the only
  caller; all live once Task 05 reuses `PrintSection` for reprint **and** storno. Also carries the
  ViewModel state-discipline rule below.
- **03e (any time)** — the discount split-and-total rule is implemented twice (displayed/printed
  total vs. submitted rows) with nothing enforcing agreement; plus mechanical de-duplication.
  Fully covered by an equality invariant, lowest risk of the batch.
- **03f (must precede Task 05 shipping)** — Room stores API DTOs, and unlike RN's persisted query
  cache (where TypeScript is erased and a stale blob just reads `undefined`) a renamed field makes
  kotlinx.serialization **throw inside a TypeConverter**, i.e. crash on read. Meanwhile
  `fallbackToDestructiveMigration(true)` covers the whole database including `receipts` — the one
  table with no `GET` endpoint to recover from and a client-only `isPrinted`. Fixes the cache half
  by degrading to a refetch plus a cache buster (keeping the DTOs, which are the right shape for a
  cache), and separates `receipts` as a durable record. **Do this while there are no installed
  apps** — that makes the migration a no-op to write, and the window closes at Task 05.

See `## Status` in `03-sell-print-receipt.md` for what the print section actually looks like
(component/ViewModel shapes, `Receipt.isPrinted` plumbing) and `## Status` in
`03b-development-print.md` for the dev-print save-to-file flow before Task 04/05 build on it —
Task 05 in particular reuses `PrintSection(isCancel, receiptSerialNumber)` as-is for
reprints/storno, and Task 03b's dev-print behavior benefits Task 05/08 automatically once they
reuse `PrintSection`, with no changes needed on their part.

Excluded from scope (per decision): the RN debug screens (`BoundStore`, `Queries`).

## Shared conventions

**Language / UI**: Kotlin, Jetpack Compose, MVVM (`*Screen` + `*ViewModel`), Hilt DI,
Kotlin coroutines/Flow. Follow the existing package layout under
`app/src/main/java/com/zephyr/boreal` (`ui/screens`, `ui/components`, `domain/model`,
`data/{local,repository,mapper}`, `api/dto`, `store`). New screens register in
`MainActivity.kt`'s `NavHost` with a string route.

**ViewModel state discipline**: a `var` in a ViewModel is permitted **only** when it is a one-shot
latch that (a) duplicates no value present in the UiState, (b) is never derivable from UiState, and
(c) is never read by the render path. Anything else belongs in the UiState data class. Examples of
the permitted kind: `SummaryViewModel.hasLeft` (double-navigation guard),
`ReviewItemsViewModel.isOrderSubmitted` (network-step idempotence). Examples of the forbidden kind,
all being removed in Task 03d: a `hasMarkedPrinted` flag beside a `receipt.isPrinted` value, an
`initialized` boolean gating UiState population, a cached `currentReceipt` shadowing what produced
`uiState.receiptToPrint`, and a held callback whose paired request lives in UiState. The failure
mode this rule exists to prevent is internal ViewModel state disagreeing with rendered UI state.

**Reuse the Boreal design system** — do not invent new visuals. Existing shared components:
`BorealButton`, `BorealTextInput`, `BorealDropdown`, `BorealSearchField`, `BorealTile`,
`BorealTopAppBar`, `BorealBottomTabBar`, `BorealAlert`, `ErrorCard`, `InfoCard`, `SuccessCard`,
`LoadingIndicator`, `QuantityStepper`, `BarcodeScanner`, `RoundInfo`. Colors live in
`ui/theme/BorealColors.kt`, type scale in `BorealFontSizes` / `BorealTypography`.

**Color palette** (from `constants/colors.ts`, already mirrored in `BorealColors`):
background `#504682`, neutral (surface/app bar) `#312a5f`, input `#5e58cb`, primary/ok
`#1fb381`, warning `#ffb000`, error `#d11533`, canceled/storno highlight `#a951f3`, text
`#ffffff`. Screens are dark-on-purple; buttons use variant colors (ok / warning / neutral /
disabled).

**Language**: all user-facing copy is **Hungarian**. Reuse the exact RN strings (they carry
domain meaning) via `strings.xml`.

**Money & dates**: money math must match the RN utilities exactly (integer-forint rounding of
VAT via `Math.round`, cash/"paper" invoices rounded to the nearest 5 Ft). Reuse the existing
native `calculateReceiptTotals` and date helpers in `ReviewItemsViewModel`. Backend dates for
receipts are `yyyy-MM-dd`.

**API**: Laravel backend under `d:\zephyrcohu-aru` (`routes/api.php`, `app/Http/Controllers`,
`app/Http/Resources`, `app/Http/Requests`). Sanctum bearer token + `X-Device-Id` header.
Requests/responses are wrapped in a `{ "data": ... }` envelope. Ability middleware gates
endpoints (e.g. `I` = internal/admin, `A`, `NP`). Add DTOs under `api/dto` and map to domain
models in `data/mapper`; never leak DTOs into the UI.

**Persistence durability classes**: every persisted table is either a **cache** (reconstructible by
refetching — items, partners, rounds, stores, price lists, user profile) or a **record** (exists
only on the device — `receipts`, including its client-only `isPrinted`, plus the DataStore-held
sell-flow draft, printer settings and session token). Caches may store API DTO shapes directly,
must degrade to a refetch rather than throw on a decode failure, and may be destructively migrated.
Records must not store DTO shapes, must surface decode failures, and require real migrations —
never destructive ones. There is no `GET /receipts`, so a wiped receipts table is unrecoverable.
When adding a table, state which class it is. See Task 03f.

**Offline-first**: most screens read from persisted local state and treat the network as an
enhancement. Mutations (create receipt, storno, storage load, lock/unlock store) require
connectivity and must surface a Hungarian "internet required" message when offline. Use the
existing `ConnectivityObserver`.

**Testing** (see `CLAUDE.md`): unit tests (JUnit + MockK/Mockito-Kotlin, Turbine for Flows)
for every ViewModel and mapper; Compose instrumented tests for screen behavior. Run
`./gradlew ktlintFormat`, `./gradlew detekt`, `./gradlew test` before completion. Instrumented
tests: **never** run with more than one device connected (see `CLAUDE.md` ADB safety).

## Task file template

Each `NN-*.md` contains: **Goal · Business context · Starting / resulting state ·
Data requirements · Behavioral requirements · API requirements · UI/UX notes ·
Testing requirements · Non-goals · Depends on · RN reference paths**.
