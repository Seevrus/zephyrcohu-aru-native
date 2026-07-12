# Task 06 · Storage: select store (lock to user)

## Goal
Start the warehouse-loading ("Rakodás") flow by letting the user pick an available store,
locking it to themselves on the server, loading both the primary store and the selected store's
current stock, and entering the item-selection step.

## Business context
"Rakodás" moves stock between the company's **primary** store (`type == 'P'`, the central
warehouse) and a mobile/van store the user loads for their round. Before loading, the user must
select and **lock** one available store to themselves so no one else can modify it concurrently.
Only certain stores are eligible: not the primary store, currently idle (`state == 'I'`,
un-locked), and either owned by the user or the user has the "load any store" permission. After
locking, the app fetches the primary store's details and the selected store's details to seed the
loading screen.

## Starting / resulting state
- **Start**: the storage/"Rakodás" tile computes its enabled state (already present) but there is
  no select-store screen; `StoresRepository` + `Store`/`StoreDetails` domain models and
  `getStoreDetails` exist.
- **Result**: a store-selection screen (dropdown) with a forward action that locks the store,
  loads primary + selected store details into storage session state, and navigates to
  select-items (Task 07). This is a working increment even though item selection is a stub until
  Task 07 lands — deliver Tasks 06–08 in sequence so intermediate states build.

## Data requirements
- Store list from `GET /stores` (cached; refresh if stale on entry). Each store: id, name, type,
  `state` (`I` idle / `L` locked / `R` ...), owner, user (current locker).
- Eligibility filter for the dropdown: `type != 'P'` **and** `state == 'I'` **and**
  (`user.canLoadAnyStore` **or** store has no owner **or** `owner.id == user.id`); sorted by name
  (Hungarian collation). Default selection = the user's owned store if present.
- After locking, fetch and persist into storage session state: the **primary store details** and
  the **selected store details** (both as initial + current snapshots — the loading screen needs a
  mutable "current" copy and an immutable "initial" copy for the summary/print).

## Behavioral requirements
- Online-only: if connectivity drops, leave the screen (navigate back).
- Selecting a store that is already locked to another user must not be selectable/submittable.
- Forward action appears only once a store is selected; it: (1) calls lock-to-user, (2) fetches
  primary details, (3) fetches selected-store details, (4) stores initial+current state,
  (5) replaces the route with select-items (so back doesn't return to selection).
- Surface server refusals as a readable error (store already in use by the user, belongs to
  another user, already locked, cannot lock primary) — see the endpoint's distinct responses.
- Show a loading state across the lock+fetch sequence.

## API requirements
- `POST /storage/lock_to_user` (`StorageController@lock_to_user`), Sanctum + `X-Device-Id`,
  body `{ "data": { "storeId": <int> } }`. Success returns the updated **user** resource (now
  carrying `storeInUse`). Notable non-2xx: `507` already-has-store / already-locked, `403`
  belongs to another user, `422` cannot lock primary, `404` unknown store.
- `GET /stores` (`view_all`) and `GET /stores/{id}` (`view`) for list + details.

## UI/UX notes
- Reuse `BorealDropdown`, `ScrollContainer`, `ErrorCard`, `LoadingIndicator`, and a forward
  affordance in the app bar (mirror `ForwardIcon`). Header "Raktár kiválasztása", label "Raktár".

## Testing requirements
- ViewModel unit tests: eligibility filtering + default selection; lock→fetch→store sequence
  ordering; error mapping for each refusal; offline exit.
- Instrumented tests: dropdown lists only eligible stores; forward disabled until a selection;
  forward triggers lock + navigation.

## Non-goals
- No item quantity editing (Task 07). No load/unlock (Task 08). No creating/editing stores.

## Depends on
Frontier (stores data layer). Precedes Tasks 07–08.

## RN reference paths
- `resources/frontend/screens/storage/select-store/SelectStore.tsx`, `useSelectStoreData.ts`
- `resources/frontend/api/mutations/useSelectStore.ts`, `api/queries/useStores.ts`,
  `api/queries/useStoreDetails.ts`
- backend `app/Http/Controllers/StorageController.php` (`lock_to_user`)
