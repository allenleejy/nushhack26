# 06: Enrol, Me, Outbox, and the safety officer's Alerts and Fork screens

Wave: 1 · Devin mode: Normal · Depends on: the plan job (cards 01 and 02 to work end to end)

## Goal
A new phone enrols as one person and gets its signing key. Me shows who the phone is. The radiation safety officer sees every alert, opens a fork to see both branches side by side with every signature checked on the phone, and settles it with a signed decision.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/enrol/
- app/src/main/java/com/nushhack26/app/ui/me/
- app/src/main/java/com/nushhack26/app/ui/outbox/
- app/src/main/java/com/nushhack26/app/ui/alerts/
- app/src/main/java/com/nushhack26/app/ui/fork/
- app/src/main/java/com/nushhack26/app/data/repo/EnrolRepository.kt
- app/src/main/java/com/nushhack26/app/data/repo/SafetyRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/EnrolScreenshotTest.kt, AlertsScreenshotTest.kt, ForkScreenshotTest.kt, MeScreenshotTest.kt, OutboxScreenshotTest.kt

## Build
Follow `docs/spec.md` §3 for E1, E2, O1 and O2, with that exact text.
- **E1 Enrol:**
  - the roster (`people`, active), with **Try again** calling `syncScheduler.syncNow()`
  - after a name is picked: `DeviceKeys.createKey()`, then `SignPrompt` (to prove the key works)
  - `EnrolRepository.enrol(person, keyInfo)` saves the `devices` row and its outbox entry, and sets the identity (`IdentityRepository.setIdentity`)
  - then `syncNow()`, and the "Enrolled as …" card with the waiting state
  - release builds with no secure lock screen: the spec's error
  - the app shows Enrol until an identity exists (`AppScaffold` already does this)
- **E4 Outbox** (`OutboxScreen`, opened by the top app bar's SyncChip and the OUTBOX tab): follow the spec's E4. Each `outbox` row shows what record it carries (read the record from Room by `recordId`), its tries and last error, with **SYNC NOW** as the primary action.
- **E2 Me:** name, role, staff number; "Device key 3F:A9:12:7C · hardware-backed (TEE)"; the sync card with **Sync now**; the release version (5 taps → Debug, as now).
- **O1 Alerts:**
  - cards sorted fork, overdue, seal mismatch, then refused, using `SafetyRepository`
  - **fork card:** names `alerts.person_id`, and says "both signatures valid on this phone" only if `Signatures.verify` passes for every record in `handover_ids`; otherwise the warning
  - **overdue cards:** from `deriveCustody(serverView = true)` for every item, with the spec's text, including "newer handovers may not have synced"
  - **behaviour:** while the screen is visible and online, call `syncNow()` every 10 seconds
  - the empty state, and "Resolved by Dr Tan" collapsed cards
- **O2 Fork:**
  - the two branches side by side: giver → receiver, times, "delivered by <device owner's name>'s phone" (the person whose `devices.created_by` equals the record's `created_by`), and the key fingerprints with ✓ or ✗
  - one choice per branch, a note field, and **Sign decision**
  - signing calls `SignPrompt`, then `Canonical` resolution string, then `DeviceKeys.sign`, then `SafetyRepository.saveResolution` (record and outbox in one transaction), then `syncNow()`

## Offline behaviour
- **Enrol needs the network once.** Offline, it shows the empty or error state and never creates a half identity.
- **Alerts offline** shows what last synced with "Can't reach the server. Showing what synced at 14:02."
- **A decision made offline** is queued: "Decision signed · not synced yet".

## Acceptance criteria (checked on a phone)
- [ ] A fresh install online: pick "Ravi Kumar" → fingerprint → "Enrolled as Ravi Kumar · key …". The `devices` row is on Supabase within 10 seconds, and the app opens on My custody.
- [ ] Me shows the name, role, key fingerprint and security level.
- [ ] On the emulator as Dr Tan, after the demo's fork syncs: a red fork card naming Ravi Kumar, both branches on O2 with ✓ signatures, and a signed decision that closes the alert on the server.
- [ ] Before reconnecting, the overdue card names the last holder the server knows. After reconnecting, it names the new holder.
- [ ] It works in airplane mode (Alerts shows the last sync; a decision is queued and syncs later, with nothing doubled).
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Fresh install, online: Enrol as Dr Tan on the emulator.
2. Run the demo's fork (cards 03 and 04), reconnect, and watch Alerts.
3. Open the fork → choose Wei Ming → Sign decision → the alert collapses and the server's alert has `closed_at`.

## Notes
- **Data:** `people`, `devices`, `items`, `handovers`, `alerts`, `resolutions`, `sync_state`.
- **Uses:** `crypto/` (card 01), `SignPrompt.kt`, `IdentityRepository`, `container.syncScheduler`.
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`:
    `ForkAlert` (the O1 fork card and the O2 header), `LoadingSkeleton` (Alerts while syncing), `BottomSheet` (confirming the decision) and `TextField` (the decision note). Use `ListRow` (the roster on Enrol with `RowLeading.Initials`, the outbox rows), `SyncChip` (FORK, WAITING), `PrimaryButton` (CREATE MY SIGNING KEY, SYNC NOW, DECIDE WHO HOLDS C-12) and `EmptyState`.
