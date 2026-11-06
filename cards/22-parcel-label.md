# 22: Done: the parcel label and its stamp

Wave: 4 (polish) · Devin mode: Normal · Depends on: the polish prep on `main` (the `ParcelLabel` stub with its final signature, `Haptics`, `TenetMotion`, `LocalReduceMotion`)

## Goal
The end of every handover feels finished: a label like a shipping box, wrapped in warning tape, and a stamp that falls onto it with a thud, sending a small shockwave through the label.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/ParcelLabel.kt
- app/src/test/java/com/nushhack26/app/screenshots/ParcelLabelScreenshotTest.kt (new)

Keep `ParcelField` and `ParcelLabel`'s signature exactly as they are on `main`: cards 18 and 19 call them in the same wave.

## Build
**Layout** (label anatomy from `design/brand.md`: a 2 dp outline, content in boxed cells divided by 2 dp rules):
- **Top tape:** a 12 dp band, `hazardStripes(onSignal, signal)`.
- **Header cell:**
  - `itemName` uppercase in `headline` (falls back to `itemCode` when blank)
  - under it, `CAMERA <itemCode>` in `record`, `inkMuted`
- **Stamp area:** an empty `bg100` cell, 140 dp tall. The stamp lands here, so it never covers a value.
  - The stamp: a 3 dp `ok` box (`strokeStamp`), rotated `TenetAngles.angleStamp` (−9°), padded 16/8 dp.
  - Inside, three lines in bold uppercase mono, `ok`: `stampWord` in `title` size, `stampTime` in `record`, `stampLine` in `recordLabel`.
- **Field cells:** two per row, each a boxed cell with the label (`recordLabel`, `inkMuted`) over the value (`record` bold, `ink`). An odd last field spans the row. Values wrap; they never clip.
- **Bottom tape:** as the top.

**The drop**, played once per label (`rememberSaveable`):
1. 200 ms after the label first appears, so the step's slide-in finishes first, the stamp appears at scale 2.2, already at −9°, at full opacity.
2. It falls to 1.0 in 260 ms with `FastOutLinearInEasing`, accelerating like a hand bringing it down.
3. On landing:
   - `haptics.thud()`
   - the stamp squashes to 0.94 and springs back (`spring(dampingRatio = 0.4f, stiffness = 400f)`)
   - **the wave:** each cell and each tape band moves down 6 dp and springs back (`spring(dampingRatio = 0.35f, stiffness = 600f)`), starting after a delay of 0.25 ms for every dp between its centre and the stamp's centre (about 0–120 ms)

Use `graphicsLayer { translationY = … }`, so nothing re-lays out.

- **Reduce motion:** the stamp is simply there, with no wave. `haptics.thud()` still fires once.
- **Inspection mode:** the final state, no animation.
- **Accessibility:** merge the label's semantics into one description, for example "Received Kestrel, camera C-12, at 13:02, from Ravi. From Ravi Kumar. To Tan Wei Ming. Seal S-40117."

## Offline behaviour
- Pure UI.

## Acceptance criteria (checked on a phone)
- [ ] After a handover (cards 18 and 19), both phones show the label: tape, KESTREL, CAMERA C-12, the field cells.
- [ ] The stamp falls into its empty cell with a thud, and the label visibly ripples outward from it.
- [ ] The receiver's stamp reads RECEIVED / 13:02 / FROM RAVI. The giver's reads HANDED OVER / 13:02 / TO WEI MING.
- [ ] Leaving and coming back to the same Done screen doesn't replay the drop.
- [ ] Remove animations on: the stamp is in place, and one thud is felt.
- [ ] It fits at 360 dp and 1.3× font size, with no clipped values.

## Test steps
1. Complete a handover by tap and watch both phones.
2. Repeat with Remove animations on.

## Notes
- **Uses:** `rememberHaptics()`, `TenetMotion`, `LocalReduceMotion`, `hazardStripes`, `TenetAngles`.
- **Design:** this replaces the Receipt component on the Done screens. The stamp is still the only tilted thing in the app.
- **Screenshots:** `ParcelLabelScreenshotTest`: received and handed over, in both themes, and one with a long name at 1.3× font.
