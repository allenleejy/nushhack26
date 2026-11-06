# 13: Withdraw a lost phone: the Devices tab, and wiping a withdrawn phone

Wave: overnight · Devin mode: Normal · Depends on: card 07; the revocation server, Room fields, API and stubs on `main` (`docs/sync.md` A.12 and D.6)

## Goal
DSO will ask: "What if a phone is lost?" The safety officer opens **Devices**, taps **Withdraw** on the lost phone, and from then on:
- the server rejects anything that phone signed after the withdrawal
- other phones refuse its signatures once they sync (card 09 does those checks)
- the lost phone itself wipes its data and key the next time it reaches the server

## Files owned
- app/src/main/java/com/nushhack26/app/ui/devices/
- app/src/main/java/com/nushhack26/app/data/repo/RevocationRepository.kt (fill in the stub; keep its constructor and method names)
- app/src/main/java/com/nushhack26/app/ui/enrol/ (the withdrawn notice only)
- app/src/main/java/com/nushhack26/app/ui/alerts/AlertsModel.kt (the "device revoked" reason text only)
- app/src/main/java/com/nushhack26/app/crypto/DeviceKeys.kt (add `deleteKey()` only)
- app/src/main/java/com/nushhack26/app/data/repo/IdentityRepository.kt (add what the wipe needs only)
- app/src/test/java/com/nushhack26/app/screenshots/DevicesScreenshotTest.kt, app/src/test/java/com/nushhack26/app/data/RevocationRepositoryTest.kt

## Build
Follow `docs/sync.md` A.12 and D.6, and `docs/spec.md` §3 O3 and E1, with that exact text.
- **On `main` already, don't change them:**
  - the migration (applied)
  - `DeviceEntity.revokedAtMs` and the `DeviceRow.revoked_at` mapping
  - `DeviceAdminApi` / `SupabaseDeviceAdminApi`
  - `container.revocation`
  - `DevicesRoute`, and the safety officer's DEVICES tab (it replaced RECORDS)
  - the `SyncWorker` hook that calls `checkWithdrawn()` on "not enrolled"
- **`RevocationRepository`:**
  - **`revoke(deviceId)`** returns `RelayOutcome`, which is already used for online-only calls.
    - Call `api.revoke` with a 6 s timeout, offline → `Unavailable`.
    - On `revoked` or `duplicate`, upsert `result.device.toEntity()` into Room, then request a sync.
    - On `rejected`, return `Refused(reason)`.
  - **`checkWithdrawn()`:** call `api.status()`. If `revoked`, in this order:
    1. `deviceKeys().deleteKey()`
    2. `database.clearAllTables()`, which clears `sync_state` and with it the identity, so the app shows Enrol
    3. save the notice in `sync_state` (key `withdrawn_notice`)
    4. return true

    On any error, return false, so the worker retries as now.
  - **`withdrawnNotice`:** a Flow of that `sync_state` key.
- **`DeviceKeys.deleteKey()`:** delete the Keystore entry (and the software key file in debug), so a new enrolment makes a fresh key.
- **O3 Devices** (`DevicesScreen(container)`):
  - rows from Room: devices joined with people, newest first, each with name, role, key line, enrolled time and an **In use** or **Withdrawn 14:40** chip
  - this phone's own row says "This phone" and has no action
  - **Withdraw** → the bottom sheet → `revocation.revoke(id)` → a snackbar with the result
  - offline: **Withdraw** disabled with the spec's text
  - loading, empty and error states
- **E1 Enrol:** when `withdrawnNotice` isn't null, show the red notice above the form. Clear it once enrolment succeeds.
- **Alerts:** map the refusal reason `device revoked` to "Refused: a C-12 record signed by <name>'s withdrawn phone".

## Offline behaviour
- Devices shows the last synced list offline, and Withdraw needs the network.
- A withdrawn phone that never comes online keeps its local copy until its 72-hour lease runs out (A.9). Other phones refuse its new signatures as soon as they've synced (card 09).

## Acceptance criteria (checked on a phone and the emulator)
- [ ] Emulator enrolled as Dr Tan (O-3001 / 904372): the DEVICES tab lists Siti's and Ravi's phones and "This phone".
- [ ] Withdraw Ravi's phone: the bottom sheet → **Withdraw phone** → the chip turns "Withdrawn". On Supabase, `devices.revoked_at` is set.
- [ ] Ravi's phone, online: within a sync (or **Sync now**), it shows Enrol with "This phone was withdrawn by your safety officer…", and its local data is gone.
- [ ] A handover Ravi's phone signed offline after the withdrawal is rejected when another phone syncs it. Dr Tan's Alerts shows "Refused: … withdrawn phone".
- [ ] Siti's phone (a store officer) has no DEVICES tab, and `revoke_device` refuses her anyway.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Emulator: enrol as Dr Tan. Phones: Siti and Ravi, enrolled and synced.
2. Dr Tan: DEVICES → Withdraw Ravi's phone. Ravi's phone: Sync now.
3. Ask the build account to reset Ravi's enrolment code afterwards, so he can enrol again.

## Notes
- **Data:** `devices`, `people`, `sync_state`; the server through `container.revocation` only.
- **Shared files changed:** list the `DeviceKeys.kt` and `IdentityRepository.kt` additions in the PR. Keep them minimal.
- **Don't touch** `crypto/OfferChecker.kt`, `crypto/Roster.kt` or `ui/timeline/` (card 09 is there).
- **Design:** rows use `ListRow`, the chip uses `SyncChip` styles, and Withdraw uses `DangerButton`. Read `design/brand.md`.
