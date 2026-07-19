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
- **Errand (round) admin**: start errand, end errand, print end errand.
- **Sell flow up to finalization**: select partner (stored + tax-number search + add partner),
  select items (barcode, quantities), select other items, review items (including per-item
  discounts, Task 01), **and receipt + order creation to the API** (`ReviewItemsViewModel`
  already builds totals, VAT breakdown, rounding, serial numbers, vendor/buyer, and calls
  `receiptsRepository.createReceipt` / `createOrders`).
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
| 02 | Sell · summary screen & flow completion | Sell | 01 | Not started |
| 03 | Sell · print sales receipt (shared print section) | Sell / Print | 02 | Not started |
| 04 | Receipts · list of receipts | Receipts | 03 | Not started |
| 05 | Receipts · details, reprint & storno (cancel) | Receipts | 04 | Not started |
| 06 | Storage · select store (lock to user) | Storage | frontier | Not started |
| 07 | Storage · select items from store (batches, barcode, quantities) | Storage | 06 | Not started |
| 08 | Storage · review, save & summary (load + unlock + print) | Storage / Print | 07, 03 | Not started |
| 09 | Errand history · list rounds & round details | Errand history | frontier | Not started |
| 10 | Cross-cutting · startup data sync & stale-data refresh parity | Infra | frontier | Not started |
| 11 | Cross-cutting · app-update / version gate | Infra | frontier | Not started |

**Next up:** Task 02 (Sell · summary screen & flow completion) is unblocked — Task 01 is done —
and not started yet.

Excluded from scope (per decision): the RN debug screens (`BoundStore`, `Queries`).

## Shared conventions

**Language / UI**: Kotlin, Jetpack Compose, MVVM (`*Screen` + `*ViewModel`), Hilt DI,
Kotlin coroutines/Flow. Follow the existing package layout under
`app/src/main/java/com/zephyr/boreal` (`ui/screens`, `ui/components`, `domain/model`,
`data/{local,repository,mapper}`, `api/dto`, `store`). New screens register in
`MainActivity.kt`'s `NavHost` with a string route.

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
