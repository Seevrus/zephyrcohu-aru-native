# Task 09 · Errand history: list rounds & round details

## Goal
Let the user browse past rounds ("körök") and open one to see its summary and reprint the
end-of-round report.

## Business context
The errand-administration area lets the user review historical rounds. A list shows each round
with its store, start and finish timestamps; opening a round shows its store, start/finish,
partner list, and offers printing the same end-errand summary that the live end-errand flow
prints — but for that historical round.

## Starting / resulting state
- **Start**: start-errand, end-errand and print-end-errand for the **current** round are built;
  `RoundsRepository` and the round domain model exist; there is no history list/detail.
- **Result**: a list-rounds screen and a round-details screen (with reprint) reachable from the
  errands hub.

## Data requirements
- Rounds from `GET /rounds` (`view_all`), cached; each round: id, store (name), `roundStarted`,
  `roundFinished`, partner list (name), and the data needed to reproduce the end-errand printout.
- Round-details reuses the end-errand print data builder for the selected round id.

## Behavioral requirements
- **List**: fetch rounds (show a "Körök betöltése folyamatban..." loading state); render rows with
  store name, start and finish; tap → details for that round id. Wire the errands hub to open the
  list.
- **Details**: show store, start, finish, partner list; if the printable end-errand data is
  available, offer printing via the printer connector (reuse the Task 03 / print-end-errand
  pattern). Loading state "Kör betöltése folyamatban...".
- Read-only: no editing, starting, or deleting rounds from these screens.
- Handle rounds with a missing finish (in-progress) gracefully (blank finish).

## API requirements
- `GET /rounds` (`RoundController@view_all`), Sanctum + `X-Device-Id`. (Start/finish/delete
  endpoints already used elsewhere; not needed here.)

## UI/UX notes
- Reuse list/card components, `Container`, `LoadingIndicator`, printer connector. Hungarian
  labels: "Raktár", "Kör kezdete", "Kör vége", "Partnerlista". Match date/time formatting used
  by the existing round screens.

## Testing requirements
- ViewModel unit tests: rounds exposed/sorted as in RN; details lookup by id; printable-data
  availability gating.
- Instrumented tests: list renders rounds and navigates to details; details render fields and
  show the print action when data is present.

## Non-goals
- No round CRUD. No per-receipt drill-down beyond the end-errand summary.

## Depends on
Frontier (rounds data layer, end-errand print). Independent of the sell/storage/receipt tasks.

## RN reference paths
- `resources/frontend/screens/errand-administration/list-errands/ListErrands.tsx`,
  `ErrandsListItem.tsx`
- `resources/frontend/screens/errand-administration/errand-details/ErrandDetails.tsx`,
  `useErrandDetailsData.ts`
- `resources/frontend/api/queries/useRounds.ts`; backend `RoundController@view_all`
