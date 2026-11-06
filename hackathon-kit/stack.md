# Stack and coding rules

The build account and Devin read this before writing code.

## The app

The app is a native Android app that keeps working with no network and syncs correctly when the network returns. Judges see it on our phones, mirrored to the projector.

- **Kotlin** with **Jetpack Compose** and Material 3, in one `app` module.
- **Room** (the phone's database) holds the main copy of the data. The app reads from it and writes to it, online or not.
- **WorkManager** runs the sync in the background whenever there's a network.
- **Supabase** is the server: Postgres, sign-in and file storage. The app talks to it through supabase-kt. Supabase can also run on an organisation's own servers, which matters to DSO.
- **No web app and no iPhone version.**
- **AI:** optional, and most features won't need it. See "If the app uses AI" at the end of this file.

The package is `com.nushhack26.app`. Debug builds add `.debug` to it, so a debug build and the APK Q tests can sit side by side on one phone. minSdk is 26.

Where things live:
- `docs/spec.md`: the product spec and demo script.
- `docs/sync.md`: the exact sync protocol and conflict rules, written in the plan job.
- `cards/`: the task cards.
- `design/`: the design export. It's read-only.

## Commands

Run Gradle as `./gradlew` (Devin, and Git Bash on the laptop) or `.\gradlew.bat` (PowerShell).

- `./gradlew quickCheck`: builds the debug app and runs the JVM unit tests. It must pass before any pull request. It needs no phone or emulator.
- `./gradlew :app:recordRoborazziDebug --tests "*<Feature>ScreenshotTest"`: renders that feature's screens to PNG files in `app/build/outputs/roborazzi/`, with no phone or emulator. Leave out `--tests` to render every screen.
- `./gradlew :app:installDebug`: installs the debug build on the running emulator or a USB phone.
- `bash scripts/emulator.sh`: starts the emulator if none is running and waits until it has booted.
- `maestro test .maestro/<flow>.yaml`: a scripted test on the running emulator or phone. Maestro can switch airplane mode on and off, so flows can test offline-then-sync.
- `./gradlew :app:assembleRelease -PversionCode=NN`: the APK for the phones. Only the build account runs it, once per round.

If the kickoff had to use a different tool or path, it updates this list.

## Where code goes

All app code is under `app/src/main/java/com/nushhack26/app/`:

- `App.kt` and `AppContainer.kt`: start-up, and the one place that creates the database, the Supabase client, the sync scheduler and the repositories. No Hilt.
- `MainActivity.kt`: hosts the Compose UI.
- `data/local/`: `AppDatabase.kt`, plus the Room entities and DAOs.
- `data/remote/`: the Supabase client, and the `@Serializable` classes that match the server tables.
- `data/repo/`: one repository per feature. Screens get and change data only through these.
- `sync/`: the outbox, the sync worker and scheduler, the sync engine and the network monitor.
- `ui/theme/`: colours, type and shapes from the design.
- `ui/components/`: shared pieces, such as the sync status bar and the empty, loading and error states.
- `ui/navigation/`: `Routes.kt` (the list of screens) and `AppNavHost.kt`.
- `ui/<feature>/`: `<Feature>Screen.kt` and `<Feature>ViewModel.kt`.

Tests are under `app/src/test/java/com/nushhack26/app/`: `sync/` holds the sync rule tests, and `screenshots/` holds one `<Feature>ScreenshotTest.kt` per feature.

Elsewhere:
- `.maestro/`: phone test flows.
- `scripts/`: helper scripts, such as `emulator.sh`.
- `supabase/migrations/`: one new SQL file per server change. Never edit a migration that has been applied.
- `supabase/seed.sql`: demo data on the server.

## Shared files

Every feature would want to edit these, so only the build account does, in the kickoff and the plan job:

- `gradle/libs.versions.toml`, `app/build.gradle.kts` and the other Gradle files
- `app/src/main/AndroidManifest.xml`
- `AppContainer.kt`
- `data/local/AppDatabase.kt` and every Room entity
- `ui/navigation/Routes.kt` and `AppNavHost.kt`
- `ui/theme/`

The plan job adds every dependency, permission, table and screen the cards need up front, with a placeholder screen for each, so cards only fill in their own files. If a card truly needs to change a shared file, the change stays minimal and the pull request lists it under "Shared files changed".

## Offline and sync rules

Working offline and syncing correctly is the whole point of the app, and it's where the judges will dig. `docs/sync.md` has the exact protocol. These rules always hold:

1. **The phone's database comes first.** Screens show only what's in Room (as Flows), and every action saves to Room straight away, with or without a network. Nothing on screen waits for the server, and no screen or ViewModel calls Supabase.
2. **Every change goes through the outbox.** A repository saves the record and adds an entry to the `outbox` table in one Room transaction. Only the sync engine reads the outbox and sends data to the server.
3. **IDs are made on the phone** (UUIDs), so records created offline never need a new ID later.
4. **Prefer new records over edits.** A checklist tick, a patrol scan or an incident note is a new row, not a change to an existing one. Most new rows never conflict. **The exception is a table with a rule that new rows can break,** such as "one holder at a time" (two checkouts of the same key) or "a permit can't close while someone is inside". The server checks that rule as each row arrives, and a row that breaks it becomes a conflict, never accepted silently. `docs/sync.md` lists these rules for each table.
5. **Edits carry the version they started from.** The server applies an edit only if the record hasn't changed since. Otherwise it reports a conflict, and the rule for that table in `docs/sync.md` decides what happens. Never silently drop an officer's change.
6. **Deletes are soft** (`deleted_at`), so they sync like edits.
7. **Sending is safe to repeat.** Each outbox entry has its own ID, and the server skips one it has already applied, so a retry after a dropped connection never duplicates anything.
8. **The server's counter orders changes, not phone clocks.** Every server row gets an increasing `server_seq`, and a phone fetches "everything after the last `server_seq` I saw". Keep the phone's time an action happened (`performed_at`) for the audit trail, and the server's time it arrived (`received_at`).
9. **Sync runs by itself.** WorkManager runs it when a network appears, after each local change, every 15 minutes, and from a "Sync now" action. It retries with backoff.
10. **Sync status is always visible:** an offline banner, the number of changes waiting, the last sync time, a "not synced yet" mark on records, and a list of conflicts to resolve.
11. **Sign-in never blocks work.** The phone signs in once while online and keeps the session. If the session expires while offline, local work carries on and sync waits until it can sign in again.

## Coding rules

1. **Phone first.**
   - Build for a 412 dp-wide phone and check at 360 dp. Portrait only.
   - Touch targets at least 48 dp. Text still fits at 1.3× system font size.
   - Draw edge to edge and respect the system bars (use the Scaffold's insets).
   - Every screen has loading, empty and error states, and shows sync status wherever it shows records.
2. **Screens.** Each screen is a `<Feature>Screen` that collects its ViewModel's state, plus a stateless `<Feature>Content(state, onAction)` that only draws. Screenshot tests render the Content with sample state, so they need no database.
3. **Text.** Write UI text directly in Kotlin, not in `strings.xml`, so cards don't collide on one file.
4. **Data.**
   - Every server table has row-level security (RLS) enabled, with policies.
   - The app holds only the publishable key. The secret key never goes into the app, the repo or Devin.
   - Database and network work stays off the main thread (suspend DAOs and Flows).
5. **Don't add** Hilt or Dagger, RxJava, Retrofit, XML layouts or Fragments, or another database library. Ask in the pull request before adding any dependency; the build account adds it to the shared files.
6. **Don't use `FLAG_SECURE`.** It blacks out the screen mirror on the projector. Mention it in the pitch as a production setting instead.
7. **Releases.** Only the build account pushes to `main` and publishes APKs.
8. **Stay in scope.** Edit only the files your card owns.
9. **Branches:**
   - Devin: `devin/NN-slug`
   - Claude Code: `claude/NN-slug`, and `claude/fix-<issue>-<slug>` for fixes
10. **When unclear,** choose the simplest reasonable option and list it under "Assumptions" in the pull request.

## Configuration

Names only. Gradle reads them from `local.properties` (git-ignored) or, failing that, from environment variables, and builds them into `BuildConfig`. If they're missing, the app still builds and runs; it just can't sync.

| Name | `local.properties` on the laptop | Devin's secrets |
|---|---|---|
| `SUPABASE_URL` | yes | yes |
| `SUPABASE_PUBLISHABLE_KEY` | yes | yes |

- **The publishable key ends up inside the APK.** That's expected: row-level security protects the data. The Supabase secret key stays in the Supabase dashboard and the owner's password manager.
- **The signing key `app/hack.keystore` is committed on purpose,** with its passwords in `app/build.gradle.kts`. Every build is signed with it, so a new APK always installs over the old one and keeps the phone's offline data.
- **Local database changes:** until the freeze, a change to the Room tables bumps the database version and wipes the phone's local data (destructive migration). That's fine as long as the phone synced first, so Q syncs before installing each new APK. After the freeze, no table changes.

## Free-plan limits that matter

- **Supabase:**
  - 500 MB database, 1 GB of files, 200 realtime connections.
  - Its built-in email only reaches the team, so phones sign in anonymously unless the spec says otherwise.
  - Anonymous sign-ins are raised to 300 per hour per IP address.
- **This laptop** has 16 GB of RAM. Run one Gradle build or emulator job at a time, and keep Android Studio closed unless you need it.

## Security extras (only if the spec asks)

DSO will ask what happens to the data on a lost phone. Good answers, cheapest first:
- **App lock** with fingerprint or PIN (`BiometricPrompt`), after the first sign-in.
- **An encrypted local database:** SQLCipher for Room, with its key in the Android Keystore.
- **Remote wipe:** the server marks a device as revoked, and the app clears its local data on the next sync.

## If the app uses AI (optional)

Most of this app won't need AI, and AI can never be on the offline path, because it needs the network. Add an AI feature only when the spec asks for one, and keep it small.

- **Where:** one Supabase Edge Function holds the key and calls the provider. The app calls that function only when it's online. Never put an AI key in the app.
- **Provider:** the owner's free Gemini key, set as an Edge Function secret. Use a stable Flash model.
- **Always have a fallback.** Give every AI call a canned answer. Use it whenever:
  - the phone is offline
  - the call fails or takes longer than about 12 seconds
  - the free quota runs out

  The app must never break because of AI.
- **Limits:** Gemini's free quotas are small and reset at midnight US Pacific time.
