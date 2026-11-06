# 10: Demo tools: a demo reset, a phone prep script, and Maestro flows that match the app

Wave: overnight · Devin mode: Lite · Depends on: cards 03–07

## Goal
Rehearsing the 3-minute demo many times needs three things:
- a clean server state at the press of a button
- the phones checked in seconds
- smoke tests that still match the app, now that it opens on Enrol and Lock

## Files owned
- supabase/reset_demo.sql (new; the build account runs it, it's not a migration)
- scripts/demo-prep.sh (new)
- .maestro/
- README.md (the "Demo" section only)

## Build
- **`supabase/reset_demo.sql`:** one transaction that puts the server back to the start of `docs/spec.md` §2.
  - **Delete:**
    - every handover except the three `register` records
    - every `alerts`, `resolutions`, `missing_reports`, `offer_relay` and `enrol_attempts` row
    - the `applied_ops` rows for the deleted records
  - **Keep** `people`, `items` and `devices`, so enrolled phones stay enrolled. Never drop or alter a table.
  - **Clear** any `devices.revoked_at`, so a phone withdrawn in rehearsal works again.
  - **An optional second section**, commented out, deletes all `devices` rows for a full re-enrol.
  - **Comments** at the top say what each phone must do after a reset: Debug → **Clear local data**, then **Sync now**.
- **`scripts/demo-prep.sh`** (bash, Git Bash on the laptop). For every phone in `adb devices`, print one line:
  - model
  - Android version
  - installed Tenet `versionName`
  - whether airplane mode leaves Bluetooth and NFC on (`settings get global airplane_mode_radios` must not contain `bluetooth` or `nfc`)
  - NFC on
  - Bluetooth on
  - battery %

  Then, with `--install <apk>`, install that APK on each phone (`adb install -r`) and print the new version.
- **Maestro** (debug app id `com.nushhack26.app.debug`):
  - fix `smoke.yaml` and `offline.yaml` for today's app:
    - they start on Enrol (not enrolled) or the role's home (enrolled)
    - each step checks for either, using `runFlow` with `when`
    - assert the offline banner as before
  - add `enrol-wrong-code.yaml`: Enrol → staff number `R-2210` → code `000000` → the "doesn't match" text appears. Use one wrong code only, to stay under the 5-try limit.
- **README.md, "Demo" section:** how to reset (ask the build account to run `reset_demo.sql`), how to run `demo-prep.sh`, and the airplane-mode setting command from `docs/sync.md` A.11.

## Offline behaviour
- Not applicable: these are tools. The `offline.yaml` flow still checks the offline banner.

## Acceptance criteria
- [ ] `bash scripts/demo-prep.sh` prints one correct line per connected phone, and `--install` installs and reports the version.
- [ ] `reset_demo.sql` runs twice in a row without errors (the build account runs it through the Supabase MCP), and afterwards C-12, C-07 and C-15 are "In store" with Siti after a phone clears its local data and syncs.
- [ ] `maestro test .maestro/smoke.yaml` and `.maestro/offline.yaml` pass on the emulator with the debug app, enrolled or not.
- [ ] `./gradlew quickCheck` passes.

## Test steps
1. Run `demo-prep.sh` with the phones plugged in.
2. Run the three Maestro flows on the emulator.
3. Paste the SQL in the PR description so the build account can check it before running it.

## Notes
- Don't change `supabase/migrations/` or `supabase/seed.sql`.
- Don't touch the app code.
