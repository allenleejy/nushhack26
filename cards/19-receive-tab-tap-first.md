# 19: Receive, as a tab, tap first

Wave: 4 (polish) · Devin mode: Normal · Depends on: the polish prep on `main` (the RECEIVE tab, `ReceiveScreen(asTab, active, session, onFlowChange)`, the stubs for `TapPulse`, `ParcelLabel` and `TapOffPanel`, `HandoverSteps`, `Haptics`)

## Goal
RECEIVE is a tab like the others. It opens on a pulsing tap target, with the QR scanner one button away. A tap floods the screen into the checks. Done is a parcel label stamped RECEIVED, with a small verify code instead of a match code to read aloud.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/receive/ (all files)
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveTapScreenshotTest.kt (new)

Don't edit `ReceiveScreenshotTest.kt`. Keep it compiling: if a function it calls changes, leave a thin wrapper with the old signature.

## Build
Read `docs/spec.md` §3 "Motion", "The tap moment", "Done: the parcel label" and R0–R5. Use the exact text below.

- **The tab.** `AppScaffold` already hosts `ReceiveScreen` as the RECEIVE tab:
  - `active` is true only while the pager has settled on the tab
  - a new `session` gives a fresh ViewModel
  - `onFlowChange(true)` (already sent when the step leaves R1) hides the bottom bar and stops the swipe
  - `onDone` resets the tab and returns to the first tab
  - With `asTab`, R1 has no Cancel. From R2 onwards, CANCEL and the top bar's close both call `onDone`.
  - Start the NFC reader, and the camera in QR mode, only while `active`.
- **Stepper.** `HandoverStepper(HandoverSteps.receiverTap / receiverQr / receiverOnline, step)`:
  - tap mode: TAP · SCAN SEAL · SIGN
  - QR mode: as today
  - an offer relayed through the server: OPEN · SCAN SEAL · SIGN
- **R1, tap mode** (the default):
  - Tap `Ready`: `TapPulse(Waiting, title = "HOLD TO THE GIVER'S PHONE", subtitle = "Back to back, until both buzz.")`. Otherwise `TapOffPanel(availability, onResume = { tapLink.refresh() })`, and with `NoNfc` start in QR mode.
  - At the bottom: `SecondaryButton("Scan a QR code instead", icon = Icons.Sharp.QrCodeScanner)`.
  - On a tap:
    1. set the pulse to `Contact` and feed the offer to the ViewModel as now
    2. keep showing the pulse, not the checks, until `onFlooded`
    3. then show R2
- **R1, QR mode:**
  - `QrScanner(title = "Scanner · offer", hint = "Point at the giver's code", torchOn, onToggleTorch = { … ToggleTorch }, rejectCount)`
  - the text button USE TAP INSTEAD
  - Remove the separate torch `SecondaryButton` everywhere: the torch now lives in the scanner's bezel.
- **R2.** As now. As each check line appears, `haptics.tick()`. On a refusal, `haptics.reject()`.
- **R3.** `QrScanner(title = "Scanner · seal", hint = "Scan the seal on Kestrel (C-12)", …)`.
  - On a mismatch: increase `rejectCount` and `haptics.reject()`.
  - On a match: `haptics.confirm()`.
- **R4.**
  - `WhoAndWhatCard` title: the camera's name ("KESTREL"), then a `record` line "CAMERA C-12 · SOURCE IR-0457 · SEAL S-40117".
  - Add `itemName: String = ""` to `HandoverSummary`, and fill it from the item in `ReceiveViewModel`. Keep the default: `GiveViewModel` also builds `HandoverSummary`, and card 09's open PR edits that file.
- **R5 and Done.**
  - The acceptance QR (QR mode) stays, but replace the big `MatchCode` with the verify line: "VERIFY CODE 482 107 · same on both phones" (`recordLabel`, `inkMuted`). Delete `MatchCode`.
  - Done, for every path (tapped, online, after the acceptance QR):
    - `ParcelLabel`:
      - `itemName`, `itemCode`
      - fields: FROM giver · TO you · SEAL · RETURN BY
      - `stampWord = "Received"`, `stampTime` = when you signed (HH:mm), `stampLine = "From Ravi"`
    - then the sync line with the `SyncChip` ("Not synced yet" or "Synced")
    - then the verify line
    - primary DONE
  - The tap fallback ("Couldn't reach Siti's phone. Show this instead.") and the online fallback stay as they are.
- **Step changes.** `AnimatedContent`, forward slides left, `TenetMotion.STEP_MS`, `TenetMotion.Emphasized`, no fade. Snap with `LocalReduceMotion`.

## Offline behaviour
- All of it works offline, as today. The tab needs no network.
- With NFC or Bluetooth off, `TapOffPanel` says so and the QR button scans as before.

## Acceptance criteria (checked on two phones)
- [ ] RECEIVE shows the bottom bar and the pulsing square, and no camera opens.
- [ ] Swipe left and right across RECEIVE: the NFC reader and the camera only start once it settles.
- [ ] A tap from the giver: punch, amber flood, buzz, then the checks. The bar is hidden and swiping is off until DONE.
- [ ] DONE returns to CUSTODY (or ITEMS) and shows the camera held. RECEIVE is fresh again.
- [ ] Each check line ticks. The seal scan buzzes on a match and flashes orange with a reject buzz on a mismatch.
- [ ] Done shows the RECEIVED stamp dropping onto the KESTREL label, and the verify code matches the giver's.
- [ ] SCAN A QR CODE INSTEAD scans the offer as before. The acceptance QR shows the verify line, not a big code.
- [ ] Replay: SCAN A QR CODE INSTEAD → scan a photo of an old offer → "This is a copy. It can't be used."
- [ ] An offer through the server (the banner) still opens full screen with OPEN · SCAN SEAL · SIGN.
- [ ] It fits at 360 dp and 1.3× font size. With remove animations on, it still works and the haptics still fire.

## Test steps
1. Two phones offline, NFC and Bluetooth on. Phone B: RECEIVE. Phone A: issue C-12 to B, then touch the phones. On B: seal → accept → DONE.
2. Repeat with SCAN A QR CODE INSTEAD.
3. Swipe CUSTODY ↔ RECEIVE ↔ OUTBOX a few times, and check the camera light stays off.

## Notes
- **Data:** none new. The camera's name comes from `ItemEntity.name` (`displayName` falls back to the code).
- **Screenshots:** in `ReceiveTapScreenshotTest`, one for each of: R1 tap, R1 with NFC off, R1 in QR mode, R2 passed, R4 with the name, Done.
- **Design:** `design/brand.md`.
