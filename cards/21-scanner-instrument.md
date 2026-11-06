# 21: The scanner as an instrument

Wave: 4 (polish) · Devin mode: Normal · Depends on: the polish prep on `main` (`QrScanner`'s new parameters `title`, `onToggleTorch`, `rejectCount`; `Haptics`; `TenetMotion`)

## Goal
Every QR scan (offer, seal, acceptance) looks like a lab instrument: a bezel with a readout, amber brackets, a sweeping scan line and hazard tape. It locks on in cyan with a tick, and flashes orange on a wrong code.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/QrScanner.kt
- app/src/test/java/com/nushhack26/app/screenshots/ScannerScreenshotTest.kt (new)

Keep `QrScanner`'s signature exactly as it is on `main`: cards 18 and 19 call it in the same wave.

## Build
Everything square (`RectangleShape`). Remove the `RoundedCornerShape(16.dp)` and the M3 `Button`s.

- **Bezel**, a `bg100` panel with a 2 dp `lineStrong` border:
  - **Header strip**, 48 dp, `bg200`:
    - left: `title` uppercase in `recordLabel` bold ("SCANNER · SEAL")
    - right: an 8 dp square status light, then the status word in `recordLabel`
    - then, when `onToggleTorch` isn't null, a 48 dp icon button (`FlashlightOn` / `FlashlightOff`, content description "Torch on" / "Torch off")
  - **Window:** the camera preview, square, full width
  - **Foot:** a static 8 dp hazard band (`hazardStripes(onSignal, signal)`), the one striped thing in the scanner
  - **Under the bezel:** `hint` in `body`, `ink`
- **Overlay on the window:**
  - four corner brackets, 28 dp long and 4 dp thick, in `signal`, inset 24 dp
  - a scan line: 2 dp `signal`, with a 24 dp tail fading upward (a vertical gradient from `signal` at 40 % to transparent), sweeping top to bottom inside the brackets in 1800 ms, `LinearEasing`, restarting from the top
- **States:**
  - **SEARCHING:** the light is `inkMuted`, blinking 600 ms on / 600 ms off; the status reads SEARCHING.
  - **On a decode:**
    - the brackets move 12 % inward and turn `ok` (`tween(TenetMotion.SMALL_MS, easing = FastOutSlowInEasing)`)
    - the scan line stops
    - the light turns solid `ok`, and the status reads LOCKED
    - `haptics.click()`
    - call `onCode` 250 ms later
    - after 1500 ms with no new code, return to SEARCHING
  - **Each increase of `rejectCount`:** the brackets and the light turn `alert` for 400 ms, the status reads REJECTED, `haptics.reject()`, then back to SEARCHING.
  - **No camera permission:** the window shows "Allow the camera to scan codes." with `SecondaryButton("Allow camera")`, or, once refused, `SecondaryButton("Open settings")`, as now.
  - **Debug builds:** keep **Paste code**, as a `TextLinkButton` under the hint.
- **Reduce motion:** no sweep and no blink. The light is solid, and the brackets change colour without moving. Haptics still fire.
- **Inspection mode** (screenshot tests): draw the bezel, brackets and a still scan line at 40 % over a `bg200` window, with no camera and no infinite animations.
- **Performance:** draw the brackets and the scan line in one `drawWithContent` over the preview, and read the animated values only in the draw lambda.

## Offline behaviour
- Pure UI. ML Kit's bundled model already reads codes with no network.

## Acceptance criteria (checked on a phone)
- [ ] R1 in QR mode, R3 and G4 all show the bezel with the right title: SCANNER · OFFER, SCANNER · SEAL, SCANNER · ACCEPTANCE.
- [ ] The scan line sweeps and the light blinks. The torch icon in the header switches the torch.
- [ ] Scanning the C-12 seal: the brackets close in cyan, LOCKED, a tick is felt, and the result appears.
- [ ] Scanning the wrong seal: orange flash, REJECTED, a reject buzz.
- [ ] Remove animations on: nothing moves, and the haptics still fire.
- [ ] It fits at 360 dp and 1.3× font size; the header text doesn't clip.

## Test steps
1. Receive → SCAN A QR CODE INSTEAD → scan an offer. Then scan a seal, and a wrong seal.
2. Toggle the torch in a dark room.

## Notes
- **Uses:** `rememberHaptics()`, `TenetMotion`, `LocalReduceMotion`, `hazardStripes`.
- **Screenshots:** `ScannerScreenshotTest`, one for each of: SEARCHING, LOCKED, REJECTED, and no permission, in both themes.
