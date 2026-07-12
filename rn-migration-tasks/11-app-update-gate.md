# Task 11 · Cross-cutting: app-update / version gate

## Goal
Notify the user, once a day, when a newer app version is available in the Play Store, so field
devices stay current.

## Business context
The RN app checks Google Play for an available update at most once per 24 hours (only in
production, only when online) and, if an update exists, shows a dismissible alert asking the user
to update via the Play Store. The native app has no equivalent — this closes that gap using the
platform-native mechanism.

## Starting / resulting state
- **Start**: no update check exists in the native app.
- **Result**: on the main screen, a once-per-day online check surfaces a dismissible
  "update available" alert when a newer version is published; dismissed with "Értem".

## Data / behavioral requirements
- Use the Android **in-app updates** mechanism (Play Core / Play in-app update API) as the native
  equivalent of the RN in-app-updates library — do not roll a custom version endpoint.
- Trigger only when: not a debug/dev build, connectivity is available, and at least ~24 h have
  passed since the last successful check. Persist "last checked" so it survives process restarts
  (DataStore).
- When an update is available, show a dismissible alert — title "Frissítés érhető el", message
  "Új programverzió érhető el. Kérem, frissítse a programot a Play Áruházban.", button "Értem".
  Dismissing simply closes it (RN does not force-update; keep that behavior unless the product
  owner asks for a flexible/immediate in-app update flow).
- Must not block app usage or interfere with the login / app-locked / connectivity banners on the
  main screen (coexist with the existing alerts; match RN precedence where an update alert is
  shown before the tile alert).

## API requirements
- None (Play services handle version discovery).

## Testing requirements
- ViewModel unit tests with a fake update-checker + clock: check runs only when online + prod +
  interval elapsed; "last checked" persisted and honored; alert shown only when an update is
  reported; dismiss clears it.
- Instrumented test (checker stubbed): alert appears when an update is available and dismisses on
  "Értem"; no alert otherwise.

## Non-goals
- No forced/immediate update flow unless requested. No custom in-house version API. No changes to
  CI/release versioning.

## Depends on
Frontier. Independent of all feature tasks; can be scheduled whenever convenient.

## RN reference paths
- `resources/frontend/hooks/useAppUpdates.tsx` (uses `sp-react-native-in-app-updates`)
- `resources/frontend/screens/start-page/StartPage.tsx` (alert precedence with tile alert)
