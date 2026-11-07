# Tenet

Our NUSH Hack 2026 app: a native Android chain-of-custody app for industrial radiography cameras. Every time a camera changes hands, the giver and the receiver both sign the handover on their phones, by NFC tap or QR code, with or without a network. When phones reconnect, the server rebuilds one checked chain. The product is specified in `docs/spec.md` (the demo script is §2), and the record formats and sync rules are in `docs/sync.md`.

- Kotlin, Jetpack Compose and Material 3, in one `app` module
- Room holds the phone's copy of the data; every change goes through an outbox
- WorkManager runs the sync in the background
- Supabase (Postgres and sign-in) is the server, through supabase-kt

How we work, the stack and the coding rules are in `hackathon-kit/` (start with `hackathon-kit/README.md` and `hackathon-kit/stack.md`).

## Build and install

You need JDK 17 or newer (Android Studio's `jbr` works) and the Android SDK with platform 37.

1. Create `local.properties` in the repo root:
   ```
   sdk.dir=C:/Users/you/AppData/Local/Android/Sdk
   SUPABASE_URL=...
   SUPABASE_PUBLISHABLE_KEY=...
   ```
   The two Supabase values can also come from environment variables. Without them the app still builds and runs, shows "Server not configured" on Me, and doesn't sync.
2. Start an emulator or plug in a phone with USB debugging on.
3. `./gradlew :app:installDebug` (`.\gradlew.bat` in PowerShell).

The debug build is `com.nushhack26.app.debug` ("Tenet dev"), so it installs next to the release APK (`com.nushhack26.app`). Every build is signed with the committed `app/hack.keystore`, so new APKs install over old ones and keep the phone's data.

Release APKs for the phones are on the repo's Releases page (`r01`, `r02`, …). The final demo build is `r15`.

## Configuration

| Name | Where |
|---|---|
| `SUPABASE_URL` | `local.properties` or an environment variable |
| `SUPABASE_PUBLISHABLE_KEY` | `local.properties` or an environment variable |

Only the publishable key goes in the app. Row-level security protects the data.

## Commands

| Command | What it does |
|---|---|
| `./gradlew quickCheck` | Builds the debug app and runs the JVM unit tests. Must pass before any pull request. |
| `./gradlew :app:recordRoborazziDebug` | Renders the screenshot tests to `app/build/outputs/roborazzi/`. Add `--tests "*<Feature>ScreenshotTest"` for one feature. |
| `./gradlew :app:installDebug` | Installs the debug build on the running emulator or USB phone. |
| `bash scripts/emulator.sh` | Starts the emulator (`$AVD` or the first one) if none is running and waits for it to boot. |
| `maestro test .maestro/smoke.yaml` | The app opens: Enrol, Lock or the role's first tab, whichever applies. |
| `maestro test .maestro/offline.yaml` | Airplane mode on shows the offline banner; off hides it. |
| `maestro test .maestro/enrol-wrong-code.yaml` | A wrong enrolment code shows "doesn't match" (needs a sync-configured debug build). |
| `reset-demo.bat [reenrol]` | Resets the server to the start of the demo; `reenrol` also withdraws every phone. |
| `./gradlew :app:assembleRelease -PversionCode=NN` | The release APK for round NN (build account only). |

## Demo

- **Reset the server** to the start of the demo script (`docs/spec.md` §2): double-click `reset-demo.bat` in the repo folder (or run it in a terminal). It keeps people, cameras and enrolled phones. Afterwards each phone: Debug → **Clear local data**, then **Sync now**.
  - It asks whether to also withdraw every phone (`reset-demo.bat reenrol` skips the question). Yes withdraws every enrolled phone, so each one wipes itself on its next sync and shows Enrol. Use it to give the phones new people.
  - It needs `DEMO_RESET_TOKEN` in `local.properties`, next to the Supabase values. The server keeps only its SHA-256, in `demo_secrets`. Without the laptop, run `supabase/reset_demo.sql` in the SQL editor.
- **Check the phones** in seconds: `bash scripts/demo-prep.sh` prints one line per connected phone (model, Android, Tenet version, airplane-mode radios, NFC, Bluetooth, battery). `bash scripts/demo-prep.sh --install <apk>` also installs that APK on each phone.
- **Airplane mode must leave Bluetooth and NFC on** for the tap handover (`docs/sync.md` A.11): on each phone, once, run `adb shell settings put global airplane_mode_radios cell,wifi,wimax`. `demo-prep.sh` flags a phone that still cuts them.

## Debug screen

Tap the version number on **Me** 5 times. It shows the build, the user id, the outbox, the last sync time and error, and has **Sync now** and **Clear local data**. Include a screenshot of it in bug reports.

## Layout

```
app/src/main/java/com/nushhack26/app/
  App.kt, AppContainer.kt   start-up and wiring (no DI framework)
  auth/                     app lock and offline unlock
  crypto/                   signing keys, canonical records, offer checks, roster
  nearby/                   NFC tap and the phone-to-phone link
  alarm/                    the overdue alarm
  data/local/               Room database, entities, DAOs (outbox, sync_state)
  data/remote/              Supabase client, session and the server row classes
  data/repo/                one repository per feature
  sync/                     sync engine, worker and scheduler, on-screen auto-sync, network monitor
  ui/                       theme, shared components, navigation, one package per screen
app/src/test/               Robolectric tests and Roborazzi screenshot tests
.maestro/                   phone test flows
supabase/                   migrations, seed data and the demo reset
design/                     the design export (read-only)
cards/                      task cards for Devin
docs/                       spec, sync protocol, concept, setting, demo seal stickers
```
