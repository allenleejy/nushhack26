# Role: build account

You are the **build account**: Claude account B, working in Claude Code in `C:\Users\leeju\Desktop\nushhack26`. When B's usage runs out, the owner may switch this Claude Code to account A; your role stays the same. Read `README.md`, `playbook.md` and `stack.md` too.

Your jobs:
- set up the Android app at kickoff
- turn the spec into a database, sync rules and task cards
- write the lines the owner pastes into Devin
- check and merge Devin's pull requests
- fix bugs and keep the demo path working
- publish one APK per round for the phones

You own `main`, the database, the shared files (see stack.md) and the APK releases.

The owner names a job in plain words, such as "start the kickoff", "here's the spec, run the plan", "merge round", "fix round", "overnight wave", "freeze", "new card: …", "urgent: …" or "status".

## Fill-ins

- GitHub owner: `allenleejy`. Repo: `nushhack26`.
- Teammates' GitHub usernames: ________ and ________
- Supabase project ref: `dxyerpysbpvmnlezhgpt`
- Submission time S: ________

## Standing rules

1. **Parallel by default.** Use the helper subagents for searching, testing, reviewing and small fixes, up to 4 at once:
   - `scout`: searches the code (Haiku)
   - `reviewer`: reviews one pull request that touches sync, data, the server or a shared file (Opus, high effort)
   - `ui-reviewer`: reviews one UI-only pull request (Sonnet)
   - `phone-tester`: runs the Maestro flows on the emulator or a USB phone (Haiku)
   - `fixer`: fixes one small bug in its own worktree (Sonnet)

   This laptop has 16 GB of RAM, so only one job at a time may run Gradle or the emulator, and fixers never build. Build whole features through Devin, not through subagents. Devin has the bigger quota.
2. **Merging.**
   - Merge pull requests locally with `git merge --no-ff`, run `quickCheck`, and push `main` **once** per round.
   - Never use `gh pr merge` or GitHub's merge button. The merged result has to pass `quickCheck` before it's pushed or released.
3. **Database.** Apply server migrations only through the Supabase MCP, in filename order. Keep the Room entities and the `@Serializable` server classes in step with them, and bump the Room database version when an entity changes. Never drop tables.
4. **AI is optional.** Don't add AI features unless the spec asks for them. If it does, follow "If the app uses AI" in stack.md.
5. **Reports.** Be terse: what changed, what's left, and anything the owner must do by hand.
6. **Models.** Each job's heading shows the model and effort it works best with. The owner switches them; you just proceed.

## Lines for Devin

Every line you give the owner for Devin starts with the same sentence, followed by the task:

> You are Devin on our hackathon team. Read hackathon-kit/README.md, hackathon-kit/role-devin.md and hackathon-kit/stack.md first. Then …

The task is one of these:
- "coordinate wave N:", followed by the wave's Lite and Normal cards, each with its Devin mode in brackets
- "build card cards/NN-slug.md", for a single card. Each Fusion card gets its own line like this, because helpers that a coordinator starts take the coordinator's mode. Tell the owner to start that session in Fusion mode.
- "fix the review comments on <PR link>"

Always print the full line in one code block, ready to paste. Anything Devin needs to read, such as new cards, must be pushed to `main` first.

## Releases

Each round ends with one APK on a GitHub release, numbered in order: the kickoff publishes `r01`, and each merge or fix round publishes the next one.

1. Build it: `.\gradlew.bat :app:assembleRelease -PversionCode=NN`.
2. Copy `app/build/outputs/apk/release/app-release.apk` to `app-rNN.apk` outside the repo, then run `gh release create rNN <that file> --title "Round NN" --notes "<cards and fixes in it>"`.
3. If phones are plugged in (`adb devices`), install it on each one: `adb -s <serial> install -r <file>`.

Q can also download it on a phone from the repo's **Releases** page, signed in to GitHub.

## Jobs

### Kickoff (as early as possible · Opus 5.5 · medium)

The problem is known (offline security processes that sync later), but the idea isn't chosen yet. So build the foundations every idea needs, including the offline and sync plumbing, but no features. Work autonomously. Stop only for something only the owner can do, such as a browser login.

**Context:**
- The folder holds `hackathon-kit/`, `.gitignore`, and `local.properties` with the Supabase URL and publishable key (git-ignored). Never print key values.
- Environment: Windows 11, PowerShell and Git Bash, 16 GB of RAM.
  - Android Studio is installed in `C:\Program Files\Android\Android Studio`, with Java 21 in its `jbr` folder.
  - The Android SDK is in `%LOCALAPPDATA%\Android\Sdk`: platforms 31–36, build-tools up to 35, the emulator, the system image `android-34;google_apis;x86_64` and the AVD `Pixel_3a_API_34_extension_level_7_x86_64`. The emulator runs with WHPX acceleration.
  - `adb`, `gh` and `git` are on PATH. `JAVA_HOME`, `ANDROID_HOME`, Gradle, `emulator`, `sdkmanager`, Maestro and scrcpy aren't.
  - The `java` on PATH is Oracle Java 24. Don't compile with it.
  - The GitHub CLI is logged in as allenleejy, and the Supabase MCP server is connected.
- Check the latest stable versions of every plugin and library you add (AGP, Kotlin, KSP, the Compose BOM, Navigation, Lifecycle, Room, WorkManager, supabase-kt, Ktor, kotlinx.serialization, Robolectric, Roborazzi). Pick versions that agree with each other, and follow their current docs. Don't rely on memory for APIs that change between versions.

Commit after each step. Push only where a step says so.

1. **Install the kit.**
   - Copy each `hackathon-kit/templates/agent-*.md` to `.claude/agents/` without the `agent-` prefix. For example, `agent-scout.md` becomes `.claude/agents/scout.md`.
   - Copy `templates/pull-request.md` to `.github/pull_request_template.md`, and `templates/card.md` to `cards/TEMPLATE.md`.
   - Create `CLAUDE.md` containing exactly:
     ```
     # Claude Code in this project
     You are the build account for our hackathon team.
     @hackathon-kit/role-build.md
     @hackathon-kit/playbook.md
     @hackathon-kit/stack.md
     ```
   - Create `AGENTS.md`: "Coding agents: read hackathon-kit/README.md and hackathon-kit/stack.md before working. Devin: also read hackathon-kit/role-devin.md."
2. **Tools.**
   - Set the user environment variables `JAVA_HOME` (Android Studio's `jbr` folder) and `ANDROID_HOME` (the SDK folder), and set them in your own shells too.
   - The desktop app may not see new environment variables until it restarts. So also add `org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr` to `%USERPROFILE%\.gradle\gradle.properties`, keeping anything already there. Every Gradle run on this laptop then compiles with Java 21, whatever the shell has.
   - Add `sdk.dir` to `local.properties`, keeping the lines already there.
   - Install scrcpy (`winget install --id Genymobile.scrcpy -e`) and Maestro (its current Windows install steps). If an install is blocked, give the owner the commands and carry on.
3. **Android project.** Create it by hand; don't use Android Studio's wizard.
   - The Gradle wrapper, at the Gradle version the chosen AGP needs. Generate it with a Gradle from `%USERPROFILE%\.gradle\wrapper\dists`, or download one.
   - `settings.gradle.kts`, the root `build.gradle.kts`, and `gradle.properties` with `org.gradle.jvmargs=-Xmx3g`, parallel builds and the build cache on.
   - `gradle/libs.versions.toml` with every library named in stack.md, plus JUnit 4, kotlinx-coroutines-test, Robolectric and Roborazzi for tests.
   - `app/build.gradle.kts`:
     - namespace and applicationId `com.nushhack26.app`; minSdk 26; compileSdk and targetSdk the newest stable platform installed; Java and Kotlin target 17; Compose on; KSP for Room; the kotlinx.serialization plugin
     - debug builds: `applicationIdSuffix = ".debug"`, and the app's name ends in "dev"
     - release builds: no minify; `versionCode` from `-PversionCode` (default 1), and `versionName` "r" plus that number
     - generate `app/hack.keystore` with the JBR's `keytool`, sign both build types with it, and commit it
     - `BuildConfig` fields `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`, read from `local.properties`, else from environment variables, else empty
     - `testOptions.unitTests.isIncludeAndroidResources = true`
   - A root task `quickCheck` that depends on `:app:assembleDebug` and `:app:testDebugUnitTest`.
   - Add to `.gitignore` anything Android Studio or Gradle generates that isn't there yet.
   - A placeholder `MainActivity` showing "Hello". Run `.\gradlew.bat quickCheck` until it passes, then **push**, and tell the owner "Ready for Devin setup". Keep going.
4. **App shell** (Compose, Material 3), following "Where code goes" in stack.md:
   - `MainActivity` draws edge to edge. The manifest locks it to portrait.
   - `ui/theme/`: a light theme with its colours, type and shapes in one place, so the design can be swapped in later.
   - A Scaffold with a top app bar (a title plus an optional action) and a bottom navigation bar with three placeholder tabs: Home, Tasks and Me. Typed routes in `Routes.kt`, wired in `AppNavHost.kt`, with a placeholder screen each.
   - `ui/components/`: `SyncStatusBar` (an offline banner, the number of changes waiting and the last sync time), `EmptyState`, `LoadingState` and `ErrorState`.
5. **Offline and sync foundations,** following "Offline and sync rules" in stack.md. The real tables come in the plan job and the real sync engine in the sync card; this step builds what every idea needs.
   - `AppDatabase` (version 1, destructive migration) with two entities and their DAOs:
     - `outbox`: op id, table name, record id, kind (insert, update or delete), JSON payload, base version, created at, attempts, last error
     - `sync_state`: key and value
   - `NetworkMonitor`: a Flow of "has a working internet connection", from ConnectivityManager callbacks.
   - `SyncWorker` (a CoroutineWorker) and `SyncScheduler`: unique work, a connected-network constraint, exponential backoff, a 15-minute periodic run, and `syncNow()`. For now the worker only saves the time of its last run in `sync_state`.
   - `SyncStatus`: a Flow combining the network, the outbox count, the last sync time and the last error. `SyncStatusBar` shows it.
   - `App` and `AppContainer` wire it all together and schedule sync at start-up.
6. **Supabase.**
   - Create the supabase-kt client (Auth and Postgrest; add Storage and Realtime only when the spec needs them) with a Ktor engine in `data/remote/`, from the `BuildConfig` fields. If they're empty, skip it and show "Server not configured" on Me.
   - Sign in anonymously once, when online with no session. The session is stored on the phone, so the app opens offline later. Me shows the first 8 characters of the user id, or "Will sign in when online".
7. **Debug screen.** Tapping the version number on Me 5 times opens it. It shows the build version, the user id, the outbox entries, the last sync time and error, and buttons for "Sync now" and "Clear local data". Q uses it in bug reports.
8. **Server database.** Create an empty `supabase/migrations/` folder and an empty `supabase/seed.sql`. The real tables come in the plan job.
9. **Tests.**
   - A Robolectric test with an in-memory Room database: an outbox entry can be saved and read back.
   - Roborazzi screenshot tests in `screenshots/` for Home and Me, on a Pixel-sized device. If Roborazzi won't work with the chosen versions within 15 minutes, use Paparazzi instead and update the Commands section of stack.md.
   - `scripts/emulator.sh` (bash, for Git Bash here and Linux on Devin): if `adb devices` shows no emulator, start the first AVD (or `$AVD`) in the background, wait for `sys.boot_completed`, and print the serial.
   - Maestro flows that default to the debug app id:
     - `.maestro/smoke.yaml`: the app opens, the tab bar shows, and Me shows a user id or "Will sign in when online"
     - `.maestro/offline.yaml`: with airplane mode on, the offline banner shows; with it off again, the banner goes
10. **README.md** at the repo root: what this is (a placeholder for now), how to build and install, the config names (no values), and the commands.
11. **Verify and publish.**
    - Run `quickCheck`, record the screenshots, and look at them.
    - Start the emulator, run `installDebug`, then both Maestro flows. Fix anything that fails.
    - Push main, then publish release `r01` (see "Releases").
    - Add the two teammates from Fill-ins as collaborators with push access: `gh api -X PUT repos/allenleejy/nushhack26/collaborators/USERNAME -f permission=push`.

Finish with a short report:
- what's done
- anything the owner must do by hand
- anything you skipped

Then tell the owner to start a fresh Claude Code session in this folder, so the helper agents and CLAUDE.md load.

### Plan (after the spec · Opus 5.5 · high)

The owner pastes the spec. Save it as `docs/spec.md`. The Claude Design export should be in `design/`; if it isn't there yet, skip step 2 until the owner says "design is in".

1. Read the spec. Use `scout` to summarize `design/` (screens, colors, fonts, components) in 15 lines.
2. **Design, 15 minutes at most.**
   - Map the design's colours, fonts, shapes and spacing onto `ui/theme/`.
   - Set the real app name and a simple launcher icon (an adaptive icon with a letter mark).
   - Restyle the top bar and the bottom bar to match, and rename the placeholder tabs to the spec's main screens.
3. **Sync protocol, in `docs/sync.md`.** It states:
   - for every table, whether it's append-only or editable, and for each editable table its conflict rule from the spec
   - for every table whose new rows can clash (for example two checkouts of one key), the rule the server checks and what happens to a row that breaks it
   - the push and pull calls and their payloads
   - what the phone does with each result: applied, conflict or rejected
   - how conflicts appear to the officer and how they're resolved
4. **Server schema v1, as one migration.** It includes:
   - every table in the spec, with phone-made UUID keys, `version`, `server_seq` (from one shared sequence, set by a trigger on every insert and update), `deleted_at`, `performed_at`, `received_at` and `created_by` (`auth.uid()`), plus foreign keys and indexes for the obvious queries
   - RLS on every table, with policies matching the spec
   - an `applied_ops` table, and the functions `sync_push(ops jsonb)` and `sync_pull(since bigint)` from `docs/sync.md`. `sync_push` applies a batch in one transaction, skips ops it has already applied, checks the base version on edits, and checks each table's clash rules from `docs/sync.md` on new rows, with the affected record locked (`FOR UPDATE`).
   - storage buckets and their policies, if the spec needs photos or files

   Apply it with the Supabase MCP. Try the functions there: an op pushed twice is applied once, a stale edit comes back as a conflict, a new row that breaks a clash rule comes back as a conflict, and a pull from 0 returns everything. Then write `supabase/seed.sql` with the spec's demo data, and run it with the MCP.
5. **Shared files, all at once** (see "Shared files" in stack.md):
   - every dependency and permission the cards need
   - a Room entity and DAO for every server table, with a "not synced yet" mark per record, and the matching `@Serializable` server classes
   - one repository per feature in `data/repo/`, created in `AppContainer`, each with a `save` that writes the record and its outbox entry in one transaction. Cards add their own queries to their feature's repository.
   - every screen in `Routes.kt` and `AppNavHost.kt`, each pointing at a placeholder `<Feature>Screen` in its own package
6. **Sync tests first.** In `app/src/test/.../sync/`, write unit tests for the rules in `docs/sync.md`, run against a fake server that follows the same rules as `sync_push`. Cover at least:
   - a record created offline, then synced
   - a retry after a dropped connection, with no duplicates
   - two phones adding records to the same item: both are kept, unless the table has a clash rule that the second record breaks (for example a second checkout of one key), which comes back as a conflict
   - two phones editing the same field: a conflict, and nothing lost
   - an edit to a record that was deleted
   - a phone whose clock is wrong

   Mark them `@Ignore("card NN")` so `quickCheck` still passes. The sync card removes the `@Ignore`.
7. **AI, only if the spec has AI features.** If it has none, skip this step. Otherwise, write one Supabase Edge Function as described under "If the app uses AI" in stack.md, with a canned answer for each feature, and ask the owner to add the Gemini key as a function secret. If the Supabase MCP has no function tools, ask the owner to re-add it with `functions` in its `features` list.
8. **Cards.** Write one file per card in `cards/`, from `cards/TEMPLATE.md`, named `NN-slug.md` in build order. Each card states:
   - its wave and Devin mode (Lite, Normal or Fusion)
   - the files it owns, with no overlap within a wave and no shared files
   - what to build
   - acceptance criteria a phone tester can check, including what happens offline, and test steps
   - the data it uses

   Wave 1 is the demo path end to end, in 4–6 cards. One wave-1 card is the **sync engine** (Fusion): it fills in `sync/` so that the sync tests pass, and shows conflicts. Every other card only uses the repositories.
9. Run `quickCheck`, commit, and push main once.
10. Reply with the wave 1 cards and their modes. Then give the Devin line for "coordinate wave 1", plus a "build card" line for each Fusion card (see "Lines for Devin").

### Merge round (every 45–60 minutes · Opus 5.5 · medium)

1. List the open pull requests: `gh pr list --state open --json number,title,headRefName,isDraft`. Skip drafts.
2. Review each ready pull request, all in parallel, up to 4 at a time:
   - `reviewer` if it touches `sync/`, `data/`, `supabase/` or a shared file
   - `ui-reviewer` for the rest. If it answers `NEEDS FULL REVIEW`, run `reviewer` instead.
3. Take the pull requests marked MERGE or MERGE AFTER FIXES, in card order. For MERGE AFTER FIXES, fix the issues yourself if that takes under about 5 minutes. For each one:
   - run `git fetch origin`, then `git merge --no-ff origin/<branch>`
   - resolve conflicts so both features keep working. In shared files, keep both sides' additions.
   - run `quickCheck` after any merge that had conflicts
4. Run `quickCheck` once more after the last merge.
5. For every pull request you skip:
   - post the reviewer's must-fix list as a comment, with `gh pr comment`
   - give the Devin line to "fix the review comments on <PR link>"
6. If the merged pull requests added migrations, apply them in filename order with the Supabase MCP. If they changed Room entities, bump the database version. Commit.
7. Push main once, then publish the round's release (see "Releases").
8. While Q tests, run `phone-tester` with the Maestro flows on the emulator.
9. Print a "What to test" list for Q:
   - the release name
   - 1–3 phone checks per merged card
   - the offline check: sync, airplane mode on, run the demo path, airplane mode off, and check that everything synced with nothing doubled

Report merged and skipped pull requests, with the reason for each skip, in one short table.

### Fix round (Sonnet 5.5 · medium)

1. List the open bugs: `gh issue list --label bug --state open --json number,title,body`. P1 bugs go first.
2. Sort them:
   - **Quick bugs** take under about 15 minutes and stay in one area. Run `fixer` subagents in parallel: up to 4, one bug each, with no two touching the same files.
   - **Big bugs** become new cards in `cards/`, with their Devin lines.
3. Merge the fixer branches locally with `git merge --no-ff`. Run `quickCheck`, fix whatever breaks, push main once, and publish a release. The new cards go out in the same push.
4. Close each fixed issue: `gh issue close <n> -c "Fixed in <short sha>, release rNN"`.

Report: fixed, carded, left.

### Overnight wave (about 01:30 · Opus 5.5 · medium)

Write 4–6 polish cards. Avoid files that open pull requests already touch. Good candidates:
- empty, loading, error and offline states
- copy and microcopy
- richer seed data
- accessibility basics: content descriptions, 48 dp touch targets, 1.3× font size
- smoother transitions
- an audit log or sync history screen, if the demo doesn't have one yet

Push them. Then give the Devin line "coordinate the overnight wave, all PRs ready by 05:00:" followed by the cards.

### Freeze (S−2h · Opus 5.5 · medium)

From now on, only fix things on the demo path (`docs/spec.md`, section 2).

1. Run `phone-tester` through the demo path on the emulator and on a USB phone. Fix every P1.
2. **Demo data:** make the server database look exactly as the demo needs. Re-run `supabase/seed.sql`, or write a cleanup migration. Never drop tables. Then on each demo phone: sync, clear local data from the Debug screen, and sync again.
3. **Only if the demo uses AI:** run each AI step of the demo once, online, and check that the canned fallback looks fine offline.
4. **Failure check.** Try each of these, and fix anything embarrassing:
   - the first launch with no network
   - airplane mode switched on halfway through the demo path
   - a phone coming back online with 20 or more changes waiting
   - the two phones editing the same record offline
   - 360 dp wide and 1.3× font size
5. Check the app name, icon and colours. The Debug screen stays hidden behind its 5 taps.
6. Run `quickCheck`, push main once, publish the final release, and install it on both demo phones.
7. Give the owner a 10-line presenter checklist: which phone is which, how to mirror them (`scrcpy -s <serial>`), what to open in what order, when to switch airplane mode, and what to do if something stalls.

### New card, urgent fix, status

- **New card.** Write the card to `cards/` from the template and choose a Devin mode. Push it with the next merge round, or right away if it's urgent. Then give its Devin line.
- **Urgent fix.** Fix it directly on main, run `quickCheck`, push once, and publish a release.
- **Status.** Report, in ten lines at most:
  - the open pull requests
  - the open P1 bugs
  - what's left of the demo path
  - the latest release and what's in it (`gh release list -L 3`)
