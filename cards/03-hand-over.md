# 03: Hand over: choose, sign, show the offer, scan the acceptance

Wave: 1 · Devin mode: Normal · Depends on: the plan job (card 01 for real signing; until then the skeleton throws)

## Goal
The holder of a camera (a store officer issuing, or a radiographer passing it on or returning it) can hand it over offline: pick the receiver, sign with a fingerprint, show an offer QR that is plainly "for one person only", scan the receiver's acceptance, and see the match code and the saved receipt.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/give/
- app/src/main/java/com/nushhack26/app/data/repo/GiveRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/GiveScreenshotTest.kt

## Build
The `GiveRoute(itemId)` screen, with steps G1–G5 from `docs/spec.md` §3 and that exact text.
- **G1 Choose:**
  - Read the item and its custody (`deriveCustody`). If I'm not the holder, show the empty state.
  - **Receivers:** store officer → radiographers ("Issue to"); radiographer → "Hand to a colleague" (radiographers) and "Return to store" (store officers). The kind follows from the receiver's role.
  - **Due chips** (issue only): **+90 s**, **+5 min**, **17:00**, **19:00**. A clock time already passed today means tomorrow.
  - **Authorisation** (docs/sync.md A.8): run `AccessRules.checkPick(me, receiver, now)` for every receiver. Those that fail stay in the list, greyed out and not selectable, showing its text ("Jun Lim · authorisation lapsed 3 Nov"). The kind comes from `AccessRules.kindFor`.
- **G2 Review and sign:** the who-and-what card. If `container.identity.leaseNow().expired` (A.9), the Sign button is disabled with `AccessRules.LEASE_EXPIRED_TEXT`. **Sign** calls `SignPrompt`, then `HandoverBuilder` and `DeviceKeys.sign`. Save the offer with `GiveRepository.savePendingOffer` before showing the QR.
- **G3 Offer QR:**
  - `QrCode(QrPayloads.offer(record, proof))` inside the card, where proof is the head record unless it's a register record
  - "For <first name> only · single use · valid for m:ss", ticking every second
  - **Scan acceptance** and **Cancel offer**
  - full brightness and screen on while shown (the `QrCode` component does both)
  - Debug builds: **Copy code**
- **G4 Scan acceptance:** `QrScanner`, then `QrPayloads.parse`, then `OfferChecker.checkAcceptance`. If it passes, `GiveRepository.completeHandover(record)` saves the record and its outbox entry in one transaction (the repository handles the tampered phone, `docs/sync.md` A.5) and deletes the pending offer.
- **G5 Done:** "Signed by both · checked offline · 2 of 2 signatures valid", `Canonical.matchCode` in large digits, and the sync state of the record.
- **A pending offer** left unscanned can be reopened at G4 (My custody links to `GiveRoute(itemId)`, which resumes the pending offer for that item).
- **Errors:** fingerprint cancelled, wrong acceptance, not a handover code, camera permission, and expired (spec text).

## Offline behaviour
- All of it works in airplane mode. The receipt shows "Not synced yet" until sync.

## Acceptance criteria (checked on a phone)
- [ ] Siti issues C-12 to Ravi with **+90 s**: the fingerprint prompt, then the offer card with Ravi's name, the countdown, and a QR another phone scans in under 2 seconds.
- [ ] After scanning Ravi's acceptance, both phones show the same match code.
- [ ] Cancelling the fingerprint leaves C-12 with Siti, and nothing is saved.
- [ ] In Ravi's picker, Jun Lim is greyed out with "Jun Lim · authorisation lapsed 3 Nov", offline as well as online.
- [ ] With the Debug **Tampered phone** switch on, after Ravi hands C-12 to Wei Ming, Ravi's My custody still shows C-12 and he can sign a new offer for it.
- [ ] It works in airplane mode, and the handover syncs once the network is back, with nothing doubled.
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Phone A (Siti): Store → Issue on C-12 → Ravi → +90 s → Sign → fingerprint.
2. Phone B (Ravi): Receive → scan (card 04). Phone A: Scan acceptance → scan B. Compare the match codes.
3. Repeat in airplane mode, then reconnect and check Supabase has one `handovers` row.

## Notes
- **Data:** `items`, `people`, `devices`, `handovers`, `pending_offers`, `sync_state` (`tampered`).
- **Uses:** `crypto/` (card 01), `auth/AccessRules.kt`, `ui/components/QrCode.kt`, `QrScanner.kt`, `SignPrompt.kt`, `IdentityRepository` (including `leaseNow()`).
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`:
    `BottomSheet` (the receiver picker), `Receipt` (G5, with the countersigned stamp) and `Snackbar`. Use `HandoverStepper(Giver)`, `QrPanel` (caption OFFER · C-12) and `PrimaryButton` (SIGN AND SHOW QR, SCAN ACCEPTANCE). The design's giver stepper starts with "Scan seal"; ours starts with CHOOSE, because only the receiver scans the seal.
