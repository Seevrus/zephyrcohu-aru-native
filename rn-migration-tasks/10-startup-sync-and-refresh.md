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
    in-use store's details are present/refreshed. **Concretely**: `StoreSessionStore` (which
    every sell/storage screen reads via `selectedStoreCurrentState`) is in-memory-only and does
    not survive process death — after a relaunch mid-round it comes back `null` until this
    refresh populates it via `StoreSessionStore.setStore(...)`, hiding functionality that gates on
    a live store (e.g. the Receipt Details storno button, Task 05). Confirmed the backend keeps
    stock in sync symmetrically — `ReceiptController` decrements `ExpirationStore.quantity` on
    receipt creation and increments it back on cancel — so a plain `GET stores/{id}` refetch after
    relaunch is sufficient to restore correct quantities for any already-synced receipt; no local
    reconstruction from `ReceiptsStore` is needed. The one residual gap is receipts created while
    offline and not yet synced to the server, which this refresh alone cannot reflect — treat as a
    pre-existing offline-sync limitation, not new scope for this task.
  - Re-validate the current user (`check-token`) when the user object is missing but a token
    exists.
- **Staleness**: refetch cached data whose last update exceeds a max age (RN uses ~10 min
  default, and a short 30 s age when re-entering store selection). Native's garbage collection
  should cover this — verify coverage for the sets above and the store list on relevant screen
  entries.
- **Post-print reset cycle** (concretized by Task 08b — read its "Deferred" note first): Task 08b
  runs the reset+logout inline when the user leaves the print-end-errand screen, but deliberately
  does **not** port RN's crash-recovery half, because native has no startup hook to hang it on
  until this task exists. That half is:
  - A **persisted** boolean (RN's `isPendingResetAfterPrint`, a **record** per the durability
    convention — it exists only on the device and nothing server-side recreates it) that Task 08b's
    ViewModel sets to `true` on print success and back to `false` once the exit sequence completes.
    Task 08b does not add it; adding it and having 08b set it is this task's job, so the write and
    the read land together rather than leaving a flag nobody consumes.
  - On startup, if the flag is set: `ReceiptsStore.resetReceipts()`,
    `StoreSessionStore.resetSelectedStoreCurrentState()`, clear the flag, then
    `UserRepository.logout()` **only if the token is still valid** (RN skips the network logout on
    an expired token — see `useInitializeApp`).

  The window this closes is narrow but the failure is silent: a crash between "printed" and
  "exited" otherwise leaves the user logged in with a finished round and a stale local receipts
  table that the next round's end-errand printout would then include. Recoverable only by an
  explicit manual logout, which the user has no reason to know to do.
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
Frontier, plus **Task 08b** for the post-print reset cycle — 08b builds the screen and the inline
reset/logout path this task adds crash recovery to. The rest of the task is independent of 08b and
can proceed without it. Best done alongside or after the feature tasks so the exact "must be
fresh" data sets (receipts, store details) are all present.

## RN reference paths
- `resources/frontend/hooks/useInitializeApp.tsx`
- `resources/frontend/api/invalidateOldQueries.ts`
- `resources/frontend/hooks/useTiles.tsx`, `hooks/useTileStates.tsx`
- native `ui/screens/MainViewModel.kt` (`performGarbageCollection`, tile builders)
