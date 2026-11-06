# 05: Store, My custody, the timeline and the overdue alarm

Wave: 1 · Devin mode: Normal · Depends on: the plan job (card 01 for `deriveCustody`)

## Goal
A store officer sees every camera and who holds it. A radiographer sees what they hold, the countdown and their receipts. Anyone can open a camera's custody chain. The holder's phone alarms when a return is overdue, with no network.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/store/
- app/src/main/java/com/nushhack26/app/ui/mycustody/
- app/src/main/java/com/nushhack26/app/ui/timeline/
- app/src/main/java/com/nushhack26/app/alarm/
- app/src/main/java/com/nushhack26/app/data/repo/CustodyRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/StoreScreenshotTest.kt, MyCustodyScreenshotTest.kt, TimelineScreenshotTest.kt

## Build
Follow `docs/spec.md` §3 for S1, P1 and T, with that exact text.
- **`CustodyRepository`:** Flows of the items with their `CustodyState` (from `deriveCustody`, re-computed every second while a screen is visible so countdowns and overdue update), one item's chain for the timeline, my receipts (records where I'm the giver), and my pending offers.
- **S1 Store** (`StoreScreen(readOnly)`):
  - rows with status chips: In store, Out, Overdue (red), Disputed (striped red)
  - the holder line, the due line, and "Not synced yet"
  - **Issue** on in-store rows (→ `GiveRoute`), **Receive return** at the top (→ `ReceiveRoute`), row → `TimelineRoute`
  - `readOnly = true` (the safety officer's ITEMS tab) hides the buttons and uses `serverView`, captioned "As the server knows it".
- **P1 My custody:**
  - one large card per camera I hold, with a countdown and **Hand over**
  - when overdue, a full-width red banner with **Hand back to store**
  - a "Handover not confirmed" banner for each pending offer (→ `GiveRoute(itemId)`)
  - the Receipts list and the main **Receive** button
- **T Timeline:** entries oldest first, with signature checks (`Signatures.verify` through card 01), sync marks, a fork shown side by side with a red marker, overruled entries greyed, rejected entries in red, and resolution entries.
- **`alarm/OverdueAlarm`:**
  - **While the app is open:** when any camera I hold becomes overdue, post a notification (channel "Overdue returns", high importance), vibrate, and play the default alarm sound once.
  - **For the background:** schedule `AlarmManager.setAndAllowWhileIdle` at each due time I hold, and post the same notification from a receiver.
  - Call `OverdueAlarm.reschedule()` whenever my custody changes.
  - Ask for the notification permission on Android 13+ the first time a camera is mine.

## Offline behaviour
- Everything works offline from Room. The countdown and the alarm need no network.
- The Store's read-only view says the data is "as the server knows it".

## Acceptance criteria (checked on a phone)
- [ ] Siti's Store lists C-12, C-07 and C-15 "In store" after the first sync.
- [ ] After C-12 is issued to Ravi with +90 s, Ravi's My custody shows the card counting down. At zero, in airplane mode, his phone shows the red banner, vibrates, plays the sound and posts a notification.
- [ ] The timeline for C-12 shows Registered → H1 Siti → Ravi with "both signed ✓", and "Not synced yet" until it syncs.
- [ ] After a fork syncs, C-12 shows Disputed on Store and My custody, and the timeline shows two branches.
- [ ] It works in airplane mode, and the changes sync once the network is back, with nothing doubled.
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Phone A (Siti): Store, C-12 → timeline.
2. Issue C-12 to Ravi with +90 s (cards 03 and 04). Phone B: airplane mode on, wait for the alarm.
3. Lock phone B before the due time: the notification still arrives (it may be late, which is fine).

## Notes
- **Data:** `items`, `handovers`, `resolutions`, `alerts`, `people`, `pending_offers`.
- **Permissions:** `POST_NOTIFICATIONS` and `VIBRATE` are already in the manifest.
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`:
    `CustodyCard` (the hero of My custody and the timeline header: amber IN CUSTODY side tab for the holder), `CountdownBar` (due time: solid, then stripes in the last hour, then orange when overdue), `TimelineRow` (chain entries: voided and overruled ones hatched with a VOID strip) and `Receipt` (the receipts list). Use `ListRow` with `RowLeading.SealedSource` for Store rows, `HazardDiamond` (Alert when overdue) and `SyncChip` for per-record sync marks.
