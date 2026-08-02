# Task 08b · Errand: print end-errand summary & close the round

## Goal

Replace the `PrintEndErrandScreen` placeholder — today a literal `"TODO: Implement Print End
Errand Screen"` label — with the real terminal screen of the end-errand flow: print the round's
invoice summary ("körelszámolás") to the Datecs printer, then reset the device's round-scoped
state and log the user out.

## Business context

Ending a round ("kör zárása") is the accounting handover: the salesperson finishes the day,
prints a one-page summary of every invoice issued during the round, and physically hands the
invoices and the cash to the office against that printout (it carries "átvettem" / "átadtam"
signature lines). Until that printout exists there is no paper trail for the handover, so the
screen is not optional polish — it is the artifact the round exists to produce.

The current native app already **closes the round on the server** (`EndErrandViewModel` posts
`rounds/finish` with the receipts and last serial number) and then navigates to the stub screen.
So today a user can complete a round and be left with no printout and no cleanup: the local
receipts, the sell-flow draft and the sell-flow store snapshot all survive into the next round,
and the session is never logged out.

## Starting state

- `end_errand` → `print_end_errand` navigation is wired and working.
- `PrintEndErrandScreen.kt` renders a hardcoded TODO string plus a "Vissza a főképernyőre"
  button that navigates home without resetting anything.
- `PrinterConnector` (Task 03) and the dev-mode save-to-file printer (Task 03b) are built and
  reused by the sell Summary (Task 02) and the storage Summary (Task 08).
- `TextPadding` (Task 08) holds the shared fixed-width column semantics ported from RN's
  `printUtilities.ts`.
- `ReceiptsStore.resetReceipts()`, `StoreSessionStore.resetSelectedStoreCurrentState()` and
  `UserRepository.logout()` all exist and are unused by this flow.

## Resulting state

- A `PrintEndErrandViewModel` + rewritten `PrintEndErrandScreen` that renders the RN screen:
  success header, print section, "no more printing after this" warning, and a return button
  gated on having printed (or having nothing to print).
- A `PrintEndErrandTextBuilder` producing the round-summary printout.
- Exiting the screen resets round-scoped local state and logs out.

## Data requirements

Everything needed is already on the device after `rounds/finish` + `refreshCurrentUser()`:

| Printed field | Source                                                                               |
| ------------- | ------------------------------------------------------------------------------------ |
| Üzletkötő     | `User.userName`, `User.name`                                                         |
| Raktár        | `Round.store.code` / `.name` (fall back to `Store` looked up by `User.storeInUseId`) |
| Kör           | `User.lastRound.id`, `User.lastRound.partnerList.name`                               |
| Kördátum      | `User.lastRound.roundStarted` .. `roundFinished` (or today if null)                  |
| Invoice rows  | `ReceiptsStore.receipts`                                                             |

`Round.store` is preferred over the `stores` lookup RN uses: it is the store the round was
actually run with, is always present on a finished round, and needs no second query.

No new persisted state, no new API calls.

## Behavioral requirements

- **Print enabled** when a current user and a store identity are both resolvable. Otherwise show
  the RN "missing data object" `ErrorCard` and render no print section.
- **`canLeaveWithoutPrint`** when `ReceiptsStore.receipts` is empty — the round produced no
  invoice, so there is nothing to hand over. Show "Mivel nem készült számla, ezért nincs
  szükség nyomtatásra." instead of the print instructions.
- **Return button** is `OK` when `canLeaveWithoutPrint || isPrinted`, else `DISABLED`.
- **Back press** with an unprinted, non-empty round opens a `BorealAlert` ("Megerősítés
  szükséges" / "Biztosan ki szeretne lépni? A későbbiekben a Lezárt körök menüpontból lesz
  lehetőség nyomtatásra." / confirm "Igen", warning). Otherwise back press exits directly.
- **Exit** (button or confirmed back press), guarded by a one-shot latch so the two paths can't
  both run: `ReceiptsStore.resetReceipts()` (wipes the receipts table, the sell-flow draft and
  the order draft) → `StoreSessionStore.resetSelectedStoreCurrentState()` →
  `UserRepository.logout()` → navigate to `main` clearing the back stack (the main screen
  redirects to login on a missing session, matching how Settings' logout already behaves).

### Printout format

Port RN's `createPrintEndErrand.ts` (its live-round branch — the historical-round branch is
Task 09's concern) field for field. Row width is 67 characters:

```
{b}KÖRELSZÁMOLÁS | yyyy.MM.dd.{/b}
---------------------------                      (27 dashes)
Üzletkötő: <userName:7> <name:48>
Raktár   : <code:7> <storeName:48>
Kör      : <roundId:7> <partnerListName:48>
Kördátum : yyyy.MM.dd .. yyyy.MM.dd

Számlaszám  F Adószám       Bolt                          Végösszeg
-------------------------------------------------------------------
<serial:>11 <F> <vatNumber:13> <buyerName:29> <roundedAmount:>9
...
-------------------------------------------------------------------
<'Összesen (N db számla)':57> <totalAmount:>9
<'Készpénzes számlák összesen':57> <totalCashAmount:>9
===================================================================
A számlákat és a készpénzt:
(3 blank lines)
<'------------------':>47  ------------------
<'átvettem':>37 <'átadtam':>18
-------------------------------------------------------------------
Számlázott készlet összesen: <totalQuantity:>15 karton, <totalAmount:>11 Ft
-------------------------------------------------------------------
```

- `F` (invoice format) is `Á` for `InvoiceType.ELECTRONIC`, else `K`. Missing VAT number prints
  `-`.
- A canceled receipt contributes **two** rows: the original, then a row keyed on
  `cancelSerialNumber` with the amount negated and sign-prefixed.
- Totals **skip canceled receipts entirely** (invoice + storno net to zero). `totalCashAmount`
  only accumulates receipts with `paymentDays == 0`. `numberOfReceipts` counts a canceled
  receipt as 2.
- Amounts render with the same JS-like number formatting the receipt printout uses (integral
  doubles print without a `.0`), so this must reuse `PrintReceiptTextBuilder`'s formatter rather
  than re-derive it.

**Deliberate deviation from RN:** RN sorts the invoice rows by building
`{ [serialNumber]: line }` objects and sorting on `keys(obj)[0]`, i.e. **lexicographically on
the stringified serial number** — so serial 10 prints before serial 9. The column exists to be a
numerically ordered list of invoice numbers, so this port sorts numerically. This is the one
place the port does not reproduce RN byte for byte; everything else, including the totals'
canceled-receipt handling, is faithful.

**Deferred to Task 10 — see its "Post-print reset cycle" bullet, which now spells this out:** RN
carries an `isPendingResetAfterPrint` flag (set on print success, consumed by `useInitializeApp`)
so that a crash between "printed" and "exited" still triggers the reset and logout on next launch.
Native has no startup hook to hang that on until Task 10 exists, so **this task adds neither the
flag nor a write to it** — Task 10 adds the persisted flag, has this ViewModel set it, and adds
the startup consumer, so the write and the read land together instead of leaving a flag nobody
reads. Do not add a half of it here.

The consequence of the gap in the meantime is bounded but silent: a crash in that window leaves
the user logged in with a finished round and a stale local receipts table, which the _next_
round's end-errand printout would then include. Recoverable only by an explicit manual logout.

## API requirements

None — this task adds no endpoint calls beyond the already-implemented `POST logout`.

## UI/UX notes

Reuse `BorealTopAppBar`, `SuccessCard`-style header text, `ErrorCard`, `LoadingIndicator`,
`PrinterConnector`, `BorealButton`, `BorealAlert`. Hungarian copy comes from RN verbatim via
`strings.xml`; the printout body text is printer output, not UI copy, so it stays in the builder
(same call as Task 03/08).

The dev-mode print flow needs a suggested filename with an identifier and a date, like the sell
and storage flows: extend `DevPrintFileNameBuilder` with a round variant.

- end errand landing (probably already implemented, but as a reference):
  `./screenshots/08b-01-end-errand-landing.png`
- print end errand: `./screenshots/08b-02-print-end-errand.png`
- leave without print alert: `./screenshots/08b-03-leave-without-print-alert.png`

## Testing requirements

- `PrintEndErrandTextBuilderTest`: fixture-based exact-column assertions (built programmatically
  via `padStart`/`padEnd`, not hand-counted spaces), canceled-receipt double row + sign, totals
  excluding canceled receipts, cash total gated on `paymentDays`, `numberOfReceipts` counting,
  numeric serial-number ordering, truncation of an over-long buyer name.
- `PrintEndErrandViewModelTest`: print-enabled gating, `canLeaveWithoutPrint`, `isPrinted`
  transition on print success, back-press confirm gating, exit running the reset+logout sequence
  exactly once across both exit paths.
- Instrumented: TODO stub is gone; return button disabled until printed when receipts exist;
  enabled immediately when there are none.

## Non-goals

- Reprinting a historical round (Task 09).
- The startup `pendingResetAfterPrint` recovery (Task 10).
- Any change to how the round is finished on the server.

## Depends on

Task 03 (print section / `PrinterConnector`), Task 03b (dev printer), Task 08 (`TextPadding`).

**Blocks Task 09**, which reuses this printout builder for historical rounds. **Blocks the
post-print reset half of Task 10**, which adds crash recovery on top of the inline reset/logout
path this task builds.

## RN reference paths

- `resources/frontend/screens/errand-administration/print-end-errand/PrintEndErrand.tsx`
- `resources/frontend/screens/errand-administration/print-end-errand/usePrintEndErrandData.ts`
- `resources/frontend/screens/errand-administration/print-end-errand/createPrintEndErrand.ts`
- `resources/frontend/utils/printUtilities.ts`
- `resources/frontend/hooks/useInitializeApp.ts` (the deferred pending-reset recovery)
