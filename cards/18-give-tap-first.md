# 18: Hand over, tap first

Wave: 4 (polish) · Devin mode: Normal · Depends on: the polish prep on `main` (stubs for `TapPulse`, `ParcelLabel` and `TapOffPanel`, the new `HandoverStepper(steps, …)`, `Haptics`, `TenetMotion`)

## Goal
Offline, the giver's offer screen is the tap target: one amber square that pulses until the phones touch, floods the screen, and waits calmly while the receiver signs. The QR code is a fallback behind a button. The Done screen is a parcel label with HANDED OVER stamped on it, and the match code shrinks to a small verify line.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/give/GiveScreen.kt
- app/src/main/java/com/nushhack26/app/ui/give/GiveComponents.kt
- app/src/main/java/com/nushhack26/app/ui/give/TapParts.kt (don't change the signatures of `rememberTapReady`, `TapLine` or `LinkedMark`: card 19 uses them)
- app/src/test/java/com/nushhack26/app/screenshots/GiveTapScreenshotTest.kt (new)

**Not** `GiveViewModel.kt`: the open PR for card 09 edits it. Everything here is UI state. Keep the "QR mode" switch in the screen (`rememberSaveable`), and pass it into `GiveContent` as a parameter.

## Build
Read `docs/spec.md` §3 "Motion", "The tap moment", "Done: the parcel label" and G1–G5. Use the exact text below.

- **Stepper.** Use `HandoverStepper(HandoverSteps.giverTap / giverOnline / giverQr, step)`:
  - tap mode (the default offline): CHOOSE · TAP. Step 1 covers G1 and G2, step 2 covers G3, and Done is 3.
  - online (G3-online): CHOOSE · SEND
  - QR mode: CHOOSE · SHOW OFFER · SCAN ACCEPTANCE, as today
- **Step changes.** Wrap the step body in `AnimatedContent`: forward slides left, back slides right, `TenetMotion.STEP_MS` with `TenetMotion.Emphasized`, no fade. Snap when `LocalReduceMotion.current` is true.
- **G2 line.** Offline: "Offline · hold your phone to Wei Ming's". Online: as now.
- **G3, tap mode** (`state.step == Offer` and QR mode off):
  - While `tapLink.available` is `Ready`, show `TapPulse` centred:
    - phase `Waiting`: title "HOLD TO WEI MING'S PHONE", subtitle "Back to back, until both buzz."
    - `state.tapped` turns true: phase `Contact`. In `onFlooded`, set a local `flooded = true`.
    - `flooded`: phase `Linked`, title "LINKED", subtitle "Wei Ming is scanning the seal and signing."
  - Under the pulse: "For Wei Ming only · single use · valid for 0:58", counting down. While waiting, also "KESTREL · C-12 → WEI MING", using `item.displayName`.
  - When tap isn't `Ready`, show `TapOffPanel(availability, onResume = { tapLink.refresh() })` instead of the pulse. With `NoNfc`, start in QR mode.
  - At the bottom: `SecondaryButton("Show QR code instead", icon = Icons.Sharp.QrCode2)` turns on QR mode. Below it, the text button CANCEL OFFER, or SIGN A NEW OFFER once expired.
  - Expired before a tap: the pulse is replaced by "Offer expired. If Wei Ming already accepted, scan their code. Otherwise sign a new offer."
  - `tapError`: show it in the existing `ErrorBox` under the pulse, and keep the QR button.
- **G3, QR mode.** Today's offer screen with the QR panel and **SCAN ACCEPTANCE**. Keep serving the tap underneath, with the existing "Or hold your phone to Wei Ming's" `TapLine`. Add the text button USE TAP INSTEAD to switch back.
- **G4.** `QrScanner(title = "Scanner · acceptance", hint = "Point at Wei Ming's code", rejectCount = …)`. Increase `rejectCount` whenever `scanError` changes to a non-null value.
- **G5, Done.** Replace the check line, the big match code box and the `Receipt` with:
  - `ParcelLabel`:
    - `itemName = item.displayName`, `itemCode = item.code`
    - fields: FROM giver name · TO receiver name · SEAL seal no · RETURN BY time (left out when there's none)
    - `stampWord = "Handed over"`, `stampTime` = acceptance time (HH:mm), `stampLine = "To Wei Ming"` (first name)
  - Then one line with the `SyncChip`: "Receipt saved · not synced yet" or "Receipt saved · synced".
  - Then the verify line: "VERIFY CODE 482 107 · same on both phones", in `recordLabel` and `inkMuted`, with the six digits grouped in threes.
  - Primary button DONE.
  - Delete the now unused `Receipt` composable from `GiveComponents.kt`.
- **Haptics** (`rememberHaptics()`):
  - `confirm()` when signing succeeds
  - `reject()` on a scan error
  - `TapPulse` and `ParcelLabel` fire their own haptics, so don't double them.

## Offline behaviour
- This is the offline path. Nothing new needs the network.
- With NFC or Bluetooth off, `TapOffPanel` says so and the QR flow works exactly as before.

## Acceptance criteria (checked on two phones)
- [ ] Airplane mode, NFC and Bluetooth on. Siti issues C-12 to Ravi → Sign. G3 shows the pulsing amber square, "HOLD TO RAVI'S PHONE", and no QR code.
- [ ] The stepper shows CHOOSE · TAP.
- [ ] Ravi opens RECEIVE and the phones touch. Siti's phone punches, floods amber, buzzes, then shows LINKED in cyan.
- [ ] Ravi accepts. Siti's phone shows the parcel label with KESTREL, the HANDED OVER stamp dropping in with a thud, and the small verify code. The digits match Ravi's.
- [ ] SHOW QR CODE INSTEAD shows today's QR flow (3 steps), and SCAN ACCEPTANCE still finishes the handover.
- [ ] NFC off: "NFC IS OFF" with TURN ON NFC. Turning it on and coming back shows the pulse again.
- [ ] Online: CHOOSE · SEND, and Done is the same parcel label.
- [ ] Remove animations on (Developer options): everything still works, nothing animates, and the haptics still fire.
- [ ] It fits at 360 dp wide and 1.3× font size.

## Test steps
1. Two phones offline, NFC and Bluetooth on. Issue C-12, touch the phones, accept on the other phone, and watch both screens.
2. Repeat with SHOW QR CODE INSTEAD.
3. Turn NFC off and open G3.

## Notes
- **Data:** none new. `item.displayName` is the camera's name (the `items.name` column), or its code if the name is empty.
- **Screenshots:** in `GiveTapScreenshotTest`, one for each of: G3 waiting, G3 linked, G3 with NFC off, G3 in QR mode, Done. `TapPulse` and `ParcelLabel` draw still in tests.
- **Design:** `design/brand.md`. Uppercase mono for titles and buttons, sentence case for sentences, no exclamation marks.
