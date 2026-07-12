# Task 10 · Cross-cutting: startup data sync & stale-data refresh parity

## Goal
Ensure the app deterministically refreshes exactly the reference data each user state needs when
the main screen becomes active, prunes stale cached data, and performs the post-print
reset/logout cycle — matching the RN initialization behavior so every downstream feature reads
fresh, correct data.

## Business context
The RN app centralizes startup logic (`useInitializeApp`) that, when the user is authenticated
and online, refreshes different data sets depending on the user's server **state**, redirects to
the app-locked screen when the account cannot use the app, and runs a "pending reset after print"
cycle that clears session state and logs out after certain print actions. A staleness helper
(`invalidateOldQueries`) refetches cached data older than a threshold on screen focus. The native
app already has a main-screen ViewModel with tiles and a garbage-collection step; this task
audits and closes the gaps so the behavior is at parity.

## Starting / resulting state
- **Start**: native `MainViewModel` builds tiles, handles `canUseApp`, and runs a
  `performGarbageCollection`; repositories can fetch each data set; DataStore holds session/app
  state.
- **Result**: a verified, tested initialization contract covering state-driven refresh, staleness
  invalidation, app-locked redirect, and the post-print reset/logout — implemented only where
  currently missing. **First step for the implementer: diff current native behavior against the
  RN reference and scope down to real gaps.**

## Data / behavioral requirements (parity targets)
- **Auth/lock**: when authenticated, not token/password-expired, and the account's `canUseApp`
  is false → route to the app-locked screen. (Native already does this — confirm.)
- **State-driven refresh** (only when online, token present, password not expired):
  - Loading state (store lock in progress) → ensure items and stores are present/refreshed.
  - On-round state → ensure items, other items, partner lists, partners, price lists, and the
    in-use store's details are present/refreshed.
  - Re-validate the current user (`check-token`) when the user object is missing but a token
    exists.
- **Staleness**: refetch cached data whose last update exceeds a max age (RN uses ~10 min
  default, and a short 30 s age when re-entering store selection). Native's garbage collection
  should cover this — verify coverage for the sets above and the store list on relevant screen
  entries.
- **Post-print reset cycle**: after the print-driven reset trigger (RN `pendingResetAfterPrint`),
  clear sell/receipt/order/selected-store session state and, if the token is still valid, log the
  user out. Confirm whether an equivalent exists in native; add if missing (no
  `pendingReset`/`afterPrint` handling currently found).
- Refreshes must not thrash: only refetch when data is absent or stale, guarding against loops
  (mirror RN's "not already loading/pending" guards).

## API requirements
- No new endpoints. Uses existing: `check-token`, `items`, `other_items`, `partner_lists`,
  `partners`, `price_lists`, `stores`, `stores/{id}` (details), `logout`.

## Testing requirements
- ViewModel unit tests with a fake clock / repositories: correct data sets refreshed per user
  state; no refetch when fresh; staleness triggers refetch past threshold; app-locked redirect on
  `canUseApp == false`; post-print reset clears the right stores and logs out only when the token
  is valid.
- Regression: existing main-screen/tile tests still pass.

## Non-goals
- No UI redesign of the main screen. No change to how individual endpoints are called at the
  repository level beyond what parity requires.

## Depends on
Frontier. Best done alongside or after the feature tasks so the exact "must be fresh" data sets
(receipts, store details) are all present.

## RN reference paths
- `resources/frontend/hooks/useInitializeApp.tsx`
- `resources/frontend/api/invalidateOldQueries.ts`
- `resources/frontend/hooks/useTiles.tsx`, `hooks/useTileStates.tsx`
- native `ui/screens/MainViewModel.kt` (`performGarbageCollection`, tile builders)
