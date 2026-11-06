# 06: Outbox, and the safety officer's Alerts and Fork screens

Wave: 1 · Devin mode: Normal · Depends on: the plan job (cards 01 and 02 to work end to end)

## Goal
Anyone can see what their phone hasn't delivered yet. The radiation safety officer sees every alert, opens a fork to see both branches side by side with every signature checked on the phone, and settles it with a signed decision. (Enrol and Me moved to card 07.)

## Files owned
- app/src/main/java/com/nushhack26/app/ui/outbox/
- app/src/main/java/com/nushhack26/app/ui/alerts/
- app/src/main/java/com/nushhack26/app/ui/fork/
- app/src/main/java/com/nushhack26/app/data/repo/SafetyRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/AlertsScreenshotTest.kt, ForkScreenshotTest.kt, OutboxScreenshotTest.kt

## Build
Follow `docs/spec.md` §3 for E4, O1 and O2, with that exact text.
- **E4 Outbox** (`OutboxScreen`, opened by the top app bar's SyncChip and the OUTBOX tab): follow the spec's E4. Each `outbox` row shows what record it carries (read the record from Room by `recordId`), its tries and last error, with **SYNC NOW** as the primary action.
- **O1 Alerts:**
  - cards sorted fork, overdue, seal mismatch, then refused, using `SafetyRepository`
  - **fork card:** names `alerts.person_id`, and says "both signatures valid on this phone" only if `Signatures.verify` passes for every record in `handover_ids`; otherwise the warning
  - **overdue cards:** from `deriveCustody(serverView = true)` for every item, with the spec's text, including "newer handovers may not have synced"
  - **refused cards:** show the server's reason in words, including the access refusals "not authorised: lapsed" ("Refused: C-12 to Jun Lim, whose authorisation had lapsed") and "devices are added by enrol_device only"
  - **behaviour:** while the screen is visible and online, call `syncNow()` every 10 seconds
  - the empty state, and "Resolved by Dr Tan" collapsed cards
- **O2 Fork:**
  - the two branches side by side: giver → receiver, times, "delivered by <device owner's name>'s phone" (the person whose `devices.created_by` equals the record's `created_by`), and the key fingerprints with ✓ or ✗
  - one choice per branch, a note field, and **Sign decision**
  - **Sign decision** shows only when `AccessRules.maySignDecision(me)` (docs/sync.md A.8), and is disabled with `AccessRules.LEASE_EXPIRED_TEXT` when `container.identity.leaseNow().expired` (A.9)
  - signing calls `SignPrompt`, then `Canonical` resolution string, then `DeviceKeys.sign`, then `SafetyRepository.saveResolution` (record and outbox in one transaction), then `syncNow()`

## Offline behaviour
- **Alerts offline** shows what last synced with "Can't reach the server. Showing what synced at 14:02."
- **A decision made offline** is queued: "Decision signed · not synced yet".
- **The outbox** works the same online or off; offline it shows "Can't reach the server. Records stay on this phone."

## Acceptance criteria (checked on a phone)
- [ ] With records waiting, the Outbox lists each one with what it carries, and empties after **SYNC NOW** online.
- [ ] On the emulator as Dr Tan, after the demo's fork syncs: a red fork card naming Ravi Kumar, both branches on O2 with ✓ signatures, and a signed decision that closes the alert on the server.
- [ ] Before reconnecting, the overdue card names the last holder the server knows. After reconnecting, it names the new holder.
- [ ] A radiographer opening a fork (from a timeline) sees the branches but no Sign decision.
- [ ] It works in airplane mode (Alerts shows the last sync; a decision is queued and syncs later, with nothing doubled).
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Enrol as Dr Tan on the emulator (card 07, or `EnrolRepository.enrol` with staff number O-3001 and code 904372).
2. Run the demo's fork (cards 03 and 04), reconnect, and watch Alerts.
3. Open the fork → choose Wei Ming → Sign decision → the alert collapses and the server's alert has `closed_at`.

## Notes
- **Data:** `people`, `devices`, `items`, `handovers`, `alerts`, `resolutions`, `outbox`, `sync_state`.
- **Uses:** `crypto/` (card 01), `auth/AccessRules.kt`, `SignPrompt.kt`, `IdentityRepository`, `container.syncScheduler`.
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`:
    `ForkAlert` (the O1 fork card and the O2 header), `LoadingSkeleton` (Alerts while syncing), `BottomSheet` (confirming the decision) and `TextField` (the decision note). Use `ListRow` (the outbox rows), `SyncChip` (FORK, WAITING), `PrimaryButton` (SYNC NOW, DECIDE WHO HOLDS C-12) and `EmptyState`.
