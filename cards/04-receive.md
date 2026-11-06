# 04: Receive: scan the offer, check it offline, scan the seal, accept

Wave: 1 · Devin mode: Normal · Depends on: the plan job (card 01 for real checks)

## Goal
A receiver scans the giver's offer, sees every offline check pass or fail, scans the camera's seal, accepts liability with a fingerprint, and shows the acceptance QR. A copied or replayed code is refused, with the reasons on screen.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/receive/
- app/src/main/java/com/nushhack26/app/data/repo/ReceiveRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveScreenshotTest.kt

## Build
The `ReceiveRoute` screen, with steps R1–R5 from `docs/spec.md` §3 and that exact text.
- **R1:** `QrScanner` with a torch toggle, then `QrPayloads.parse`. Anything other than an offer: "Not a handover code".
- **R2:**
  - "Checking offline…", then `OfferChecker.check(offer, me, roster, localHandovers, now)`. Show its lines one by one (each ✓ or ✗).
  - Then line 6, the authorisation check: `AccessRules.checkReceiver(giver, me, now)`, where `giver` is the offer's `from_person_id` from the roster and `me` is this phone's person (docs/sync.md A.4 and A.8).
  - All pass → **Scan the seal**.
  - Any ✗ → the refused state. Its header is **"This is a copy. It can't be used."** when `looksLikeCopy` and line 6 passed, else "This handover can't be accepted." Show the failed lines in red and "Nothing was signed." **Nothing is stored.**
- **R3:** `QrScanner` for the seal, then `OfferChecker.checkSeal`. A mismatch blocks with the spec's text.
- **R4:**
  - The who-and-what card, plus "This is sealed source IR-0457 (iridium-192). You're liable for it the moment you sign." (from the item) and the due time.
  - **Accept** calls `SignPrompt`, then `HandoverBuilder` (the receiver's half) and `DeviceKeys.sign`. If `container.identity.leaseNow().expired` (A.9), Accept is disabled with `AccessRules.LEASE_EXPIRED_TEXT`.
  - Then `ReceiveRepository.saveAccepted(proof, record)` saves the proof record (origin `received_in_offer`) and the new record (origin `mine`), each with its outbox entry, in one transaction.
- **R5:** `QrCode(QrPayloads.acceptance(record))`, "Show this to <giver's first name>", the match code. On **Done**: "You hold C-12. Return by 14:31." and the sync state.
- **Errors:** camera permission, fingerprint cancelled ("Nothing was signed. Ravi still holds C-12.").

## Offline behaviour
- All of it works in airplane mode, using the roster and records already on the phone.
- An unknown signer means this phone hasn't synced since that person enrolled: "Unknown signer. Sync this phone before accepting."

## Acceptance criteria (checked on a phone)
- [ ] Ravi scans Siti's offer: five green lines, the seal scan matches, the fingerprint, the acceptance QR, and the same match code as Siti.
- [ ] Siti scans a photo of an offer made out to Wei Ming: the red "This is a copy" screen with "Made out to Tan Wei Ming, not you". After 75 seconds it also shows "Expired".
- [ ] Wei Ming scans the same offer again after accepting it: "Already used on this phone".
- [ ] Ravi's six lines include "Authorised · radiographer · valid to 30 Jun 2027". On Jun Lim's phone, any offer is refused with "You aren't authorised to hold sealed sources (lapsed 3 Nov)", offline as well.
- [ ] A seal sticker for another camera gives the mismatch screen and can't be accepted.
- [ ] It works in airplane mode, and the handover syncs once the network is back, with nothing doubled (the proof record is a harmless duplicate).
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Phone B (Ravi): My custody → Receive → scan Siti's offer → Scan the seal → Accept → fingerprint.
2. Phone A scans B's acceptance (card 03). Compare the match codes.
3. Phone A: Receive return → scan a photo of an old offer → the refused screen.

## Notes
- **Data:** `items`, `people`, `devices`, `handovers`, `sync_state`.
- **Uses:** `crypto/` (card 01), `auth/AccessRules.kt`, `ui/components/QrCode.kt`, `QrScanner.kt`, `SignPrompt.kt`, `IdentityRepository` (including `leaseNow()`).
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`:
    `LiabilityLine` (above the Accept button: THIS IS SEALED SOURCE IR-0457, then the sentence) and the refused screen as an alert label. Use `HandoverStepper(Receiver)`, `SealCheckRow`, `QrPanel` (caption ACCEPTANCE · C-12), `HazardDiamond(Filled, diamondLg)` beside the liability line, and `PrimaryButton` (SCAN SEAL, then SIGN AND ACCEPT C-12; hatched out with "Scan seal first" until the seal matches).
