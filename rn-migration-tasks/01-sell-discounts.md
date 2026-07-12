# Task 01 · Sell: per-item discounts

## Goal
Let the salesperson apply discounts to an individual regular item line during review, before
the receipt is finalized, and have those discounts flow into all money totals.

## Business context
On the sell **Review** screen each regular (non-"other") item line can carry discounts defined
for that item on the server. A line can have up to three discount types available
simultaneously, each applied to a chosen quantity of that line:

- **absolute** — a fixed Ft amount subtracted from the net unit price.
- **percentage** — a percentage off the net unit price (rounded to whole forints).
- **freeForm** — an arbitrary replacement net unit price, entered as a **negative** number
  (it is added to the price, so a discount is negative).

The discounted quantities across the three types may not exceed the line's total quantity;
any remaining, undiscounted quantity keeps the normal price. Discounts change the line's net,
VAT and gross amounts, and therefore the receipt's grand total and per-VAT-rate breakdown.

## Starting / resulting state
- **Start**: Review screen lists regular + other items with per-line gross and a grand total,
  and finalizes the receipt/order (already built). Item lines expose available discounts in the
  domain model (`Item.discounts`, `DraftReceipt`/`SelectedDiscount` already exist) but there is
  **no UI to select them** and totals ignore them.
- **Result**: from a regular item line the user opens a Discounts screen, enters discounted
  quantities (and a free-form price), applies them, returns to Review; the line and grand total
  reflect the discount; finalization sends the discounted amounts and each item's discount name.

## Data requirements
- Reuse `Item.discounts` (available discounts: id, name, type, amount) and the existing
  `SelectedDiscount` domain model (id, name, type, quantity, amount?, price?).
- A regular review-item must retain its **selected discounts** in the sell-flow draft state so
  they survive navigating away from and back to Review, and so finalization can read them.
- Each finalized receipt line carries a `discountName` (RN concatenates selected discount names)
  — already a field on `DraftReceiptItem`/`CreateReceiptItemDto`; populate it.

## Behavioral requirements
- Entry point: selecting a regular line on Review (or a dedicated control on the line) opens the
  Discounts screen for that specific item + expiration batch. Other-item lines have no discounts.
- Show line identity (name + expiry month `yyyyMM`), the line quantity/unit, and one input group
  per available discount type, prefilled with any previously chosen quantities/price.
- Free-form price defaults to the negative of the item's net price when none chosen yet.
- **Validation** (block apply, show Hungarian error, mark offending fields): all inputs numeric;
  sum of discounted quantities ≤ line quantity ("Túl nagy megadott mennyiség."); free-form price
  must be ≤ 0 ("A kedvezményt negatív számként lehetséges megadni.").
- Applying with all-zero quantities clears the line's discounts.
- After apply, recompute the line amounts and the review grand total. The money math must match
  the RN utilities exactly: percentage price = `round(netPrice * (100-amount)/100)`; absolute
  price = `netPrice - amount`; free-form price = entered price; undiscounted remainder priced
  normally; VAT per amount = `round(netAmount * vatRate/100)`; totals grouped per VAT rate.
- Discounts must be reflected in the **receipt totals used at finalization** (net, VAT, gross,
  per-VAT-rate `vatAmounts`, cash rounding). Verify `calculateReceiptTotals` applies selected
  discounts; extend it to the discounted calculation if it currently ignores them.

## API requirements
- No new endpoint. Discounts are computed client-side and included in the existing
  `POST /receipts` (`create_receipts`) payload: per-line discounted `netAmount`/`vatAmount`/
  `grossAmount`, aggregated `vatAmounts`, and each line's `discountName`.

## UI/UX notes
- Reuse `BorealTextInput`, `BorealButton`, `ErrorCard`, `LoadingIndicator`. One scrollable form,
  keyboard-aware. Hungarian labels: "Mennyiség", "Elérhető kedvezmények", "Típus"
  (abszolút/százalékos/tetszőleges), "Mértéke", "Ár", "Kedvezmények érvényesítése".

## Testing requirements
- ViewModel unit tests: prefill from existing selection; validation branches (non-numeric, over
  quantity, positive free-form price); clearing discounts; building `SelectedDiscount` lists.
- Unit tests on the discounted-amount + receipt-totals calculation for each type and mixed
  (partly discounted) lines, including VAT rounding and cash 5-Ft rounding parity with RN.
- Instrumented test: open discounts from a review line, apply, see the line + grand total update.

## Non-goals
- No new discount types; no server-side discount editing; other-items keep no discounts.

## Depends on
Native sell flow through Review + finalization (frontier).

## RN reference paths
- `resources/frontend/screens/sell/review/Discounts.tsx`, `useDiscountsData.ts`
- `resources/frontend/screens/sell/review/Review.tsx`, `useReviewData.ts`
- `resources/frontend/utils/calculateDiscountedItemAmounts.ts`, `calculateReceiptTotals.ts`,
  `calculateAmounts.ts`
- `resources/frontend/store/receiptsSlice.ts` (`SelectedDiscount`), `store/sellFlowSlice.ts`
