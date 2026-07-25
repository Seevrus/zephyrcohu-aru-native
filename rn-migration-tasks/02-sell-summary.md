# Task 02 · Sell: summary screen & flow completion

## Goal
After a receipt is successfully finalized, take the user to a dedicated **Summary** screen that
confirms the sale, offers printing, and cleanly ends the sell session by returning to the main
screen with all sell-flow state reset.

## Business context
Finalization (create receipt + order, update local store quantities) already happens on the
Review screen. In the RN app, once sending succeeds the user taps "Továbblépés" and lands on a
Summary screen ("Számla mentése sikeres!") that hosts the receipt print action and a "return to
home" button. Leaving the sell flow must discard the in-progress draft so the next sale starts
clean. This task builds that terminal screen and the navigation/reset semantics; **printing
itself is Task 03** (this screen provides the slot for it).

## Starting / resulting state
- **Start**: Review finalizes and shows an inline success message; there is no dedicated summary
  screen and no post-sale reset/return contract. Verify that receipt sent in is currently handler
  correctly with the expected feedback messages given the API responses (success and error cards
  from `Review.tsx` are replicated with the correct messages.)
- **Result**: successful finalization navigates (reset stack) to Summary, which shows the success
  confirmation, the finalized receipt's serial/year identity, a placeholder/slot for the print
  section (filled in Task 03), and a "return to home" button. Any exit from Summary resets the
  sell-flow draft and lands on the main screen.

## Data requirements
- Read the just-finalized receipt from the receipts store (the last/current receipt, including
  `serialNumber`, `yearCode`, buyer, totals).
- Reset the sell-flow draft, selected partner, selected items/other items, and current
  receipt/order draft when the session ends (reuse existing reset actions in the sell/receipt
  stores).

## Behavioral requirements
- Transition to Summary only after a fully successful finalization (receipt **and** order steps
  succeeded); on partial failure the user stays on Review to retry (already handled there).
- Summary is a **terminal** screen: back-navigation / system back and the return button both go
  to the main screen via a stack reset, not back into the sell flow.
- On leaving Summary (any path), reset the sell-flow state exactly once and show a brief loading
  state during reset (mirrors RN `resetSellFlow` + `navigation.reset` on `beforeRemove`).
- Show an offline notice if connectivity is lost (informational; the receipt was already sent).
- Header title "Összegzés"; success text "Számla mentése sikeres!"; return button
  "Visszatérés a kezdőképernyőre".

## API requirements
- None. This screen consumes already-persisted local state.

## UI/UX notes
- Reuse `SuccessCard`/`InfoCard`, `BorealButton`, `LoadingIndicator`, `BorealTopAppBar`. Leave a
  clearly-marked composable slot where the print section (Task 03) will render, so Task 03 is a
  drop-in.
- layout reference: `screenshots/02-01-summary-screen-layout.png`

## Testing requirements
- ViewModel unit tests: session reset happens on exit; terminal navigation contract; reads the
  correct current receipt.
- Instrumented tests: finalize → Summary shown with success + return button; pressing return (and
  system back) lands on main screen and clears sell state (next sell session starts empty).

## Non-goals
- No printing logic (Task 03). No changes to the finalization/receipt-creation logic itself.

## Depends on
Task 01 (discounts feed final totals) and the existing finalization.

## RN reference paths
- `resources/frontend/screens/sell/summary/Summary.tsx`
- `resources/frontend/screens/sell/review/useReviewData.ts` (`finishReviewHandler`,
  `resetSellFlow`, `navigation.reset` semantics)
- `resources/frontend/store/hooks/sellFlow.ts`, `store/hooks/receipts.ts`

## Status: ✅ Done

Implemented `SummaryViewModel.kt` + `SummaryScreen.kt` (new), `ReviewItemsScreen.kt` /
`MainActivity.kt` wiring (`"summary"` route, `popUpTo(0){inclusive=true}` both into and out of
Summary — Android's terminal-nav idiom used everywhere else in this app, simpler than RN's
two-entry `[StartPage, Summary]` stack), and `ReceiptsStore.resetSellFlowDraft()`.

**Deliberate deviations from the literal RN screen**, both confirmed against the RN source and
the layout screenshot:
- The success message and offline notice use `SuccessCard`/`InfoCard` (colored cards), not RN's
  plain `Text`/`TextCard` — matches this task's own UI/UX notes and how `ReviewItemsScreen`
  already surfaces the same states, so the native app stays visually consistent with itself
  rather than mirroring RN 1:1.
- Content is a scrollable `Column` with the return button flowing naturally below content
  (mirrors `EndErrandScreen.kt`'s pattern), not pinned to the screen bottom — the first draft
  copied `PrintEndErrandScreen.kt`'s bottom-pinned button, which was wrong since that screen is
  an unrelated TODO stub, not an established layout pattern.
- No serial/year text is rendered on Summary itself (RN doesn't show it either — it's only
  passed as a prop into `PrintSection`). The receipt is still read and available for Task 03.

**Known gap to flag, not fixed here (out of scope for Task 02):** `ReceiptsStore.resetReceipts()`
(existing, used by Review's cancel flow) clears **all** persisted receipt history via
`receiptDao.clearReceipts()`, unlike RN's `resetSellFlow` which only clears the draft. This task
added `resetSellFlowDraft()` as a narrower alternative for Summary's exit path specifically to
avoid that trap; `resetReceipts()` itself was left untouched.

The print section is a marked empty slot: `SummaryPrintSectionSlot(receipt: Receipt?)`, a private
composable near the bottom of `SummaryScreen.kt`, called from `SummaryScreenContent` right after
`SuccessCard`. Task 03 replaces its body.
