# Task 03e · Domain logic: de-duplication & Kotlin pattern cleanup

## Goal
Collapse duplicated business rules introduced across Tasks 01–03b — chiefly the **discount
split-and-total rule, which is currently implemented twice** and can silently make the displayed
and printed receipt total disagree with the rows actually submitted to the backend. Plus a set of
small, mechanical Kotlin cleanups in the same code.

## Business context
Task 01 (per-item discounts) introduced two independent implementations of one rule: how a
discounted line splits into per-discount portions plus an undiscounted remainder. One drives what
the user *sees and prints*; the other drives what the backend *stores*. They agree today. Nothing
enforces that they keep agreeing, and the failure mode is a receipt whose printed total does not
equal the sum of its own line items — a financial-document defect, discovered by an accountant
rather than by a test.

Everything else in this task is cheap and mechanical, batched here because it lives in the same
files.

## Starting / resulting state

**Start:**
- **The duplicated rule.** `DiscountCalculator.calculateDiscountedLineAmounts`
  (`domain/utils/DiscountCalculator.kt`) loops the selected discounts, accumulates net/VAT/gross
  via `AmountCalculator.calculateAmounts`, then adds the undiscounted remainder — producing the
  **totals shown** on Review Items, Summary, and the printed receipt.
  `DraftReceiptItem.toDtos` + `toDiscountedDto` + `toRemainderDto` (`data/mapper/ReceiptMapper.kt`)
  run the same loop with the same remainder rule and the same `AmountCalculator` calls to emit the
  **rows submitted** to the API. The two already differ cosmetically — the remainder guard is
  `remainingQuantity <= 0` in one and `remainingQuantity > 0` in the other (equivalent today) —
  which is precisely the kind of near-miss that drifts.
- `Json { ignoreUnknownKeys = true }` is constructed **six** times: the five Room converter classes
  (`ItemConverters`, `PartnerConverters`, `ReceiptConverters`, `RoundConverters`, `UserConverters`)
  and `ReceiptsStore`. A seventh, differently configured, lives in `NetworkModule`. Six live
  instances is cheap; six independent configurations that must stay in sync is not.
- `PERCENTAGE_DIVISOR = 100.0` is declared in both `AmountCalculator` and `DiscountCalculator`.
- `ReviewItemsViewModel.onToggleExpanded` and `onToggleOtherItemExpanded` are the same
  set-toggle written twice over different fields.
- `DraftReceiptItem.toDtos` mutates an accumulator (`discountedQuantity`) from inside a `map { }` —
  a side effect in a pure-looking transform.
- `BorealPrinter.connect` / `initializePrinter` use `!!` chains (`printerSocket!!` ×3,
  `protocolAdapter!!` ×5) on fields they just assigned.
- `PrintSectionViewModel` computes a `canPrint` boolean that proves `user` and `receipt` non-null,
  then needs `requireNotNull(user)` / `requireNotNull(receipt)` two lines later because the boolean
  destroyed the smart cast.

**Result:** one implementation of the discount rule, one `Json` configuration, and the mechanical
cleanups applied.

## Data requirements
None. No model, DTO, entity, or schema changes. This task must be **behavior-preserving by
construction** — every number it touches must come out identical.

## Behavioral requirements

### 1. One discount rule (the substantive change)
- Establish a **single producer** of the per-discount rows. `toDtos()` already produces exactly the
  right decomposition — per-discount rows plus remainder — so make it (or a domain-level
  equivalent that it delegates to) the one implementation.
- Redefine `calculateDiscountedLineAmounts` as a **fold over those same rows**: sum the net, VAT
  and gross of the rows that would be submitted. The displayed total then cannot diverge from the
  submitted rows, because it is computed *from* them.
- Watch the layering: `toDtos()` currently lives in `data/mapper` and returns an API DTO, while
  `calculateDiscountedLineAmounts` lives in `domain/utils`. Domain must not depend on the API DTO
  type. The clean shape is a domain-level "split this line into its charged portions" function in
  `domain/utils`, which both the mapper (to build DTOs) and the calculator (to sum) consume.
- Reconcile the `<= 0` / `> 0` remainder guards into one expression as part of this.
- Floating-point ordering matters: summing per-row results is not necessarily bit-identical to the
  current accumulation if the order of additions changes. Keep the summation order identical to
  today's, and let the tests below prove it.

### 2. One `Json` configuration
- Provide a single `@Singleton Json` (or a shared `object`) with the `ignoreUnknownKeys = true`
  persistence configuration, and have all five converter classes plus `ReceiptsStore` use it.
- Leave `NetworkModule`'s instance separate and clearly named — it configures the *wire* format,
  which is legitimately a different concern from the *persistence* format, and conflating them is
  how a network-driven config change silently alters stored data.

### 3. Mechanical cleanups
- Single `PERCENTAGE_DIVISOR`, referenced by both calculators.
- A `Set<T>.toggle(element)` extension replacing the two hand-written toggles in
  `ReviewItemsViewModel`.
- `toDtos`: compute the discounted quantity with `sumOf { it.quantity }` and map separately, so no
  transform carries a hidden accumulator.
- `BorealPrinter`: assign to local `val`s instead of `!!`-ing just-assigned fields. **Coordinate
  with Task 03d**, which also changes this file — do 03d first and fold this in, or skip it here.
- `PrintSectionViewModel`: restructure so the non-null checks smart-cast, removing both
  `requireNotNull` calls. **Coordinate with Task 03d**, which rewrites this class.

## API requirements
None. The request payload `toDtos()` produces must be **byte-identical** to today's for every
input — that is the acceptance criterion, not a side effect.

## UI/UX notes
No visual change whatsoever. Every displayed amount must be identical.

## Testing requirements
- Unit: for a line with each combination of absolute / percentage / free-form discounts (and
  combinations thereof, with and without a remainder), assert that
  `calculateDiscountedLineAmounts` equals the **sum of the rows** `toDtos()` produces, for net,
  VAT and gross independently. This is the invariant the whole task exists to establish, and it
  should be written **before** the refactor — it must pass against the current code (proving the
  refactor is behavior-preserving) and keep passing after.
- Unit: existing `DiscountCalculatorTest` / `AmountCalculator` / receipt-mapper tests pass
  unchanged. Do not adjust an expected value to accommodate the refactor — a changed expectation
  means the refactor changed behavior and is wrong.
- Unit: an edge case worth pinning while here — discounts whose quantities sum to *exactly* the
  line quantity (zero remainder) and to *more* than it (negative remainder), asserting both
  implementations agree on the degenerate cases where the `<= 0` / `> 0` guards differ in shape.
- Unit: round-trip a `ReceiptEntity` through the shared `Json` and confirm it decodes identically
  to before, guarding the converter consolidation.

## Non-goals
- No change to the discount **rules** themselves — the RN `calculateDiscountedItemAmounts.ts` /
  `createUniqueDiscountedItems.ts` semantics, including the `Math.round`-per-portion VAT behavior,
  are frozen. This task changes only how many places implement them.
- No change to `ReceiptEntity` column types — that is Task 12.
- No new abstractions beyond the single split function; this is de-duplication, not a redesign of
  the calculation layer.

## Depends on
Task 01 (per-item discounts) for the duplicated code. Independent of Tasks 03c/03d except for the
two file-overlap notes in requirement 3.

Lowest-risk of the three pre-04 tasks — it is fully covered by an equality invariant and can land
in any order.

## RN reference paths
- `resources/frontend/utils/calculateDiscountedItemAmounts.ts` — the displayed-total rule.
- `resources/frontend/utils/createUniqueDiscountedItems.ts` — the submitted-row rule.
  These two RN files are themselves the origin of the duplication; the native app inherited the
  split. RN is the reference for *what the numbers must be*, not for keeping two implementations.
