# nushhack26

Our NUSH Hack 2026 app: a native Android app that lets safety personnel keep completing security processes offline, and syncs their changes correctly when the network returns. The product itself is still being specified in `docs/spec.md`; for now the app is the shell plus the offline and sync plumbing.

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

The debug build is `com.nushhack26.app.debug` ("NUSH Hack dev"), so it installs next to the release APK (`com.nushhack26.app`). Every build is signed with the committed `app/hack.keystore`, so new APKs install over old ones and keep the phone's data.

Release APKs for the phones are on the repo's Releases page (`r01`, `r02`, …).

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
| `maestro test .maestro/smoke.yaml` | The app opens, the tabs show, Me shows the sign-in state. |
| `maestro test .maestro/offline.yaml` | Airplane mode on shows the offline banner; off hides it. |
| `./gradlew :app:assembleRelease -PversionCode=NN` | The release APK for round NN (build account only). |

## Debug screen

Tap the version number on **Me** 5 times. It shows the build, the user id, the outbox, the last sync time and error, and has **Sync now** and **Clear local data**. Include a screenshot of it in bug reports.

## Layout

```
app/src/main/java/com/nushhack26/app/
  App.kt, AppContainer.kt   start-up and wiring (no DI framework)
  data/local/               Room database, entities, DAOs (outbox, sync_state)
  data/remote/              Supabase client and session
  data/repo/                one repository per feature
  sync/                     network monitor, sync worker and scheduler, sync status
  ui/                       theme, shared components, navigation, one package per screen
app/src/test/               Robolectric tests and Roborazzi screenshot tests
.maestro/                   phone test flows
supabase/                   migrations and seed data
cards/                      task cards for Devin
docs/                       spec and sync protocol
```
