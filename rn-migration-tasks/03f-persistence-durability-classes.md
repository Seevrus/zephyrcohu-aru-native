# Task 03f · Persistence: separate cache from record, and survive shape changes

## Goal
Give the native app the two properties the RN app gets for free from its persisted TanStack Query
cache — **a shape mismatch must degrade into a refetch, never a crash**, and **cached server data
must be discardable on upgrade** — while separating the one table that is *not* a cache
(`receipts`) so it can never be wiped or crashed by either mechanism.

## Business context

The native persistence layer stores API response DTOs as Room column types. Every entity does it,
and it predates this backlog: `ReceiptEntity` (commit c6668ac) says so in its own message —
*"mirroring the existing ItemEntity/ItemDao/ItemConverters pattern."*

| Entity | DTO-typed columns |
|---|---|
| `ItemEntity` | `List<ExpirationDto>`, `List<DiscountDto>` |
| `PartnerEntity` | `List<PartnerLocationDto>`, `BasePriceListDto?` |
| `RoundEntity` | `RoundUserDto`, `RoundStoreDto`, `RoundPartnerListDto`, `List<RoundReceiptDto>` |
| `StoreEntity` | `StoreUserDto?` ×2 |
| `UserEntity` | `CompanyDto`, `RoundResponseDataDto?` |
| `ReceiptEntity` | `ReceiptVendorDto`, `ReceiptBuyerDto`, `ReceiptUserDto?`, `List<ReceiptItemDto>`, `List<ReceiptOtherItemDto>`, `List<ReceiptVatAmountDto>` |

This originates in the RN app's approach and is **not** in itself the defect. RN persists
server-shaped data too — `App.tsx` wraps the app in `PersistQueryClientProvider` with an
AsyncStorage persister, `gcTime: Infinity` and `staleTime: Infinity`, so query results are stored
indefinitely in whatever shape `queryFn` returned (raw `response.data.data` for `useItems` /
`useOtherItems`; mapped output for `usePartners` / `useRounds` / `useCheckToken`). Editing a
Response Type in TS and carrying on works because **TypeScript is erased at runtime**: the
persisted blob is plain JSON, nothing validates it against the type, and a missing field simply
reads as `undefined`. RN adds a second safety valve — `persistOptions` sets only `persister`, so
TanStack's default `maxAge` (24h) discards the whole persisted cache daily. No `buster` is set, so
there is no version-keyed invalidation.

Kotlin has neither property, and the asymmetry runs the wrong way:

| Backend change | RN | Native today |
|---|---|---|
| Field **added** | `undefined` until refetch | fine — `ignoreUnknownKeys = true` |
| Field **renamed/removed**, DTO updated to match | old blob loads, reads `undefined` | `MissingFieldException` inside a Room `TypeConverter` → **crash on read** |

`ignoreUnknownKeys` covers only the additive direction. A non-nullable Kotlin property absent from
a stored row throws on decode, surfacing as a crash when the table is read — typically at startup
for Items/Partners. The backend is mature and such changes should be rare, but "rare crash on a
field device mid-round, after an app update" is the wrong failure mode to leave in place.

Native already has the *staleness* half of RN's design and a stricter version of it:
`BaseRepository.networkBoundResource` plus `CacheMetadataEntity(queryKey, fetchedAt)` with
`DEFAULT_CACHE_TIMEOUT_MS = 300_000` is a per-query `staleTime` at 5 minutes, versus RN's
effectively-infinite one. That part needs no change.

### The distinction that resolves it: cache vs. record

Not "DTO vs. domain" — **refetchable vs. not**. Evidence for which native table is which:

- **There is no `GET /receipts`.** `ReceiptApiService` exposes only `POST receipts`,
  `POST receipts/cancel`, `POST orders`. Receipts are not recoverable from the backend.
- **`isPrinted` is client-only.** RN sets it on its zustand slice (`usePrintSection.ts:67`) and
  never transmits it. It exists nowhere on the server.
- Receipts are round-scoped: `resetReceipts()` at `StartErrand.tsx:115` and
  `usePrintEndErrandData.ts:80`.
- The **session token lives in DataStore** (`UserSessionStore`), not Room. `UserEntity.token` is a
  secondary copy the auth path does not read — so wiping Room does not log anyone out.

So in Room today exactly **one** table is an irreplaceable record: `receipts`. Every other table
is a cache that a refetch reconstitutes.

### The aggravating factor

`DatabaseModule` builds with `.fallbackToDestructiveMigration(true)` across the whole database and
`BorealDatabase` sets `exportSchema = false`. The current answer to *any* schema change is "wipe
everything" — receipts included. Today that is harmless (no installed apps, version bumps happen
in development). From Task 05 onward, a version bump mid-round silently destroys that round's
receipts and their print state, with no endpoint to recover them from. It is currently the only
thing protecting the app from the crash scenario above, and it protects the receipts table by
destroying it.

## Starting / resulting state

- **Start**: one Room database at version 19, all tables under `fallbackToDestructiveMigration`,
  `exportSchema = false`, six entities with DTO-typed columns, five converter classes whose
  decodes propagate `SerializationException`, and a `CacheMetadataEntity` tracking only
  `fetchedAt`.
- **Result**: cache tables tolerate shape changes by refetching instead of crashing and are
  explicitly invalidated on upgrade; `receipts` is a durable record that neither mechanism can
  destroy, with non-DTO types, an exported schema and real migrations.

## Data requirements
- `CacheMetadataEntity` gains a cache-generation stamp (see behavioral requirement 2). No other
  new information is stored.
- The `receipts` table's *contents* are unchanged — only the Kotlin types describing them, and the
  stored JSON must be verified byte-compatible or migrated (requirement 3).

## Behavioral requirements

### 1. Cache decodes degrade, never crash
- Converter decodes for **cache** tables must yield null / empty rather than propagating a
  `SerializationException`. A malformed or shape-mismatched row becomes a cache miss, which
  `networkBoundResource` already handles by fetching.
- Verify the resulting flow end-to-end: a decode failure must lead to a refetch and a repaired
  row, not to an empty screen that silently stays empty. If `query()` returning empty is not
  currently enough to trigger a fetch on its own (it is gated on `fetchedAt` staleness, not on
  emptiness), the miss must also invalidate that query's cache metadata.
- This is the direct analogue of RN reading `undefined` off a stale persisted blob, and it is what
  makes DTO-typed cache columns acceptable rather than merely tolerated.

### 2. Cache buster
- Add an app-version or cache-schema stamp to `CacheMetadataEntity`. On mismatch, treat every
  cached query as stale regardless of `fetchedAt`, forcing a refetch.
- This is TanStack's `buster` option, which the RN app does not use but which replaces the
  behavior currently provided accidentally by whole-database destructive migration. It makes
  post-upgrade cache state deterministic instead of dependent on a version bump happening to occur.
- Bumping the stamp then becomes the documented, one-line response to changing a DTO used by a
  cache table — cheaper and more precise than a schema version bump.

### 3. `receipts` becomes a durable record
- Reshape `ReceiptEntity`'s columns off the `api/dto` types. Prefer dedicated `@Serializable`
  persistence types over the domain models directly, so a later domain refactor cannot invalidate
  stored data; `Receipt.toEntity()` currently round-trips domain → DTO → JSON purely to satisfy
  the converters, so this removes a conversion rather than adding one.
- Its converters must **not** adopt requirement 1's degrade-to-null behavior — for a record, a
  decode failure is a bug to surface, not a cache miss to paper over.
- Export the schema and write real migrations. Destructive migration must be unreachable for this
  table.

### 4. Make the durability class structural
- Recommended: move `receipts` into its **own Room database**, so the two migration policies are
  enforced by construction rather than by a convention someone must remember. The alternative —
  one database with real migrations throughout and trivial drop-recreate migrations for cache
  tables — keeps a single DB but leaves the policy as discipline. Confirm the choice in the
  implementation plan; the two-database split is the recommendation.
- `exportSchema` must be `true` for whichever database holds `receipts` (required by
  `MigrationTestHelper`).
- Cache tables may keep `fallbackToDestructiveMigration`; with requirements 1 and 2 in place it is
  no longer load-bearing, but it is still the correct policy for a cache.

## API requirements
None. The wire format is untouched. This task exists to stop the wire format from having a second,
undeclared job.

## UI/UX notes
No visible change on a fresh install. After an upgrade that busts the cache, the first launch
refetches — already an existing, handled state (`ApiResource.Loading`), not new UI. A user
upgrading mid-round must find their round, store session, draft sell flow and receipts intact;
that is the acceptance criterion.

## Testing requirements
- Unit: a cache converter given JSON missing a now-required field returns null/empty instead of
  throwing — the regression test for the crash scenario. Write it against a realistic stored blob,
  not a hand-minimised one.
- Unit: a decode failure on a cache table results in a refetch, and the row is repaired. This is
  the requirement-1 end-to-end check and the one most likely to reveal that emptiness alone does
  not trigger a fetch.
- Unit: `networkBoundResource` treats every query as stale when the cache stamp does not match,
  and as fresh when it does and `fetchedAt` is recent.
- Unit: the `receipts` converters **do** propagate a decode failure — the inverse of the first
  test, pinning the deliberate asymmetry so a later refactor does not "helpfully" unify them.
- Instrumented Room migration test for `receipts`: build the database at the pre-change version
  with a rich row (items, other items, VAT amounts, null `user`, `isPrinted = true`), migrate,
  assert every field survives. If the JSON proves byte-compatible and the migration is a no-op
  version bump, the test must still exist and prove it.
- Unit: entity ↔ domain round-trip for the reshaped `ReceiptEntity`.
- Architecture test: no class in `data/local` imports from `api/dto` for the record database.
  (Cache entities intentionally still may — assert the boundary that is meant to hold, not a
  blanket ban that would fail by design.)

## Non-goals
- **No decoupling of the cache entities from DTOs.** Requirements 1 and 2 make DTO-typed cache
  columns safe; reshaping five more entities buys nothing and costs a large diff. This is a
  deliberate reversal of the earlier framing that treated all six entities alike.
- No change to `networkBoundResource`'s staleness model, the 5-minute default, or the retry logic
  beyond the buster check.
- No change to what is cached, to cache-invalidation *timing* (Task 10), or to any API DTO.
- Not an ORM change — Room stays.

## Depends on
Nothing blocks it. It should land **before Task 05** ships anything to real users, since that is
when `fallbackToDestructiveMigration` over the receipts table stops being harmless. Ordering
against Tasks 03c/03d/04 is free; note that Task 03c deliberately avoids schema changes so it can
precede Task 04, and this task is where the schema work belongs instead.

Doing it while there are **no installed apps** means requirement 3's migration is a no-op to
write and verify. That window closes at Task 05.

## RN reference paths
- `App.tsx` — `PersistQueryClientProvider`, `createAsyncStoragePersister`, and the
  `gcTime`/`staleTime: Infinity` query defaults. The origin of the persisted-server-shape approach
  and of the `maxAge`/`buster` mechanisms requirement 2 mirrors.
- `resources/frontend/store/store.ts` — the zustand + AsyncStorage slice persistence, RN's
  equivalent of the native record class (receipts, sell flow, printer, user).
- `resources/frontend/api/queries/useItems.ts` (raw `response.data.data` passthrough) vs.
  `usePartners.ts` (`mapPartnersResponse`) — the two persisted cache shapes.
- `resources/frontend/components/print-section/usePrintSection.ts:67` — `isPrinted` being set
  client-side and never transmitted, the evidence that `receipts` is a record and not a cache.
