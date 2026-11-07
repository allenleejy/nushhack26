# 31: Pick the return time on a wheel

Wave: 7 (motion) · Devin mode: Normal · Depends on: the prep on `main` (`DueChoice` in GiveViewModel, the PICK A TIME chip in G1, and the `ReturnTimePicker` stub with its final signature and `ReturnTimeDefaults`)

## Goal
When the store officer issues a camera, they pick the exact return time on a pair of drum wheels, hours and minutes, instead of choosing between two fixed clock times. The two short demo timers, +90 s and +5 min, stay as chips.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/ReturnTimePicker.kt
- app/src/test/java/com/nushhack26/app/screenshots/ReturnTimePickerScreenshotTest.kt (new)
- app/src/test/java/com/nushhack26/app/ui/components/ReturnTimeMathTest.kt (new)

Rules:
- Keep `ReturnTimePicker(initial, nowMs, onConfirm, onDismiss)` and `ReturnTimeDefaults.initial` exactly as they are on `main`. GiveScreen calls them, and card 29 edits GiveScreen in the same wave.
- Don't edit any `give/` file.

## Build
Everything square (`RectangleShape`), in the Tenet type and colour tokens. Space Mono for every number.

### The sheet
- **Container:** a `ModalBottomSheet` with `shape = RectangleShape`, `bg100`, and a 2 dp `lineStrong` line along its top edge. No drag handle; an 8 dp hazard band (`hazardStripes(onSignal, signal)`) across the top instead. It respects the navigation bar inset.
- **Header:** `RETURN BY` in `label`, `inkMuted`.
- **Readout:**
  - the chosen time large: `18:45` in Space Mono bold 48/56, `ink`, centred, updating live as the wheels turn
  - under it in `body`, `inkMuted`: `Today · in 3 h 20 min`, or `Tomorrow 08.11.2026 · in 21 h 5 min` when the time has passed today or is this very minute (the `DueChoice.At` rule)
  - under 1 hour reads `in 45 min`
- **The wheels**, side by side and centred, each 96 dp wide, with a `:` in Space Mono bold 32 sp between them:
  - **Hours:** 00–23. **Minutes:** 00–59, one-minute steps.
  - **Size:** five rows visible, each 56 dp tall, so the wheels are 280 dp.
  - **Scrolling:** each wheel is a `LazyColumn` with `rememberSnapFlingBehavior`, and it loops: use a large item count and start in the middle, so 23 runs on to 00 and 59 to 00 without a hard stop.
  - **Selection band:** behind the centre row, across both wheels and the colon, a `signalBg` band with 2 dp `signal` lines at its top and bottom.
  - **Drum look:** each row's distance d from the centre row, in rows (fractional while scrolling), sets:
    - text size: the centre row is 32 sp bold `ink`; others are regular `inkMuted`
    - scale 1 − 0.12·|d|, at least 0.7
    - alpha 1 − 0.3·|d|, at least 0.25
    - `rotationX` = −18°·d, with a camera distance of 8·density

    Read the scroll offset only inside `graphicsLayer`. A 40 dp fade from `bg100` to transparent sits over the top and bottom of the wheels.
  - **Haptics:** `haptics.tick()` each time a new row crosses the centre, on either wheel.
- **Quick jumps:** one row of four square chips under the wheels: `+30 MIN`, `+1 H`, `+2 H`, `+4 H`.
  - Each sets both wheels to now plus that much, rounded up to the next 5 minutes.
  - The wheels animate there with `animateScrollToItem`, taking the shortest way round.
- **Buttons:** `PrimaryButton("Set return time")`, which calls `onConfirm(LocalTime.of(h, m))`, then `TextLinkButton("Cancel")`, which calls `onDismiss`. Dismissing the sheet calls `onDismiss`.
- **Start:** the wheels open on `initial`.

### Pure helpers, in the same file, `internal`
- `fun readout(time: LocalTime, nowMs: Long, zone: ZoneId): String`: the "Today · in 3 h 20 min" line.
- `fun quickJump(nowMs: Long, plusMinutes: Int, zone: ZoneId): LocalTime`: now plus that, rounded up to 5 minutes.
- `fun wheelIndexFor(value: Int, size: Int, current: Int): Int`: the nearest list index showing `value`, from where the wheel is now, for the shortest scroll.

### Reduce motion, accessibility and screenshots
- **Reduce motion:** quick jumps snap instead of animating, and there's no `rotationX`. Scale and alpha stay, and haptics still fire.
- **TalkBack:** each wheel is one focusable element, "Hour, 18" or "Minute, 45", with custom actions "Increase" and "Decrease" (one step), and it answers swipe up/down. The readout is a live region.
- **Inspection mode** (screenshots): render still. LazyColumn renders in Robolectric, so no special path should be needed beyond a fixed `nowMs`.

## Offline behaviour
- Pure UI. The due time is stored in the signed offer exactly as before (`dueMs`).

## Acceptance criteria (checked on a phone)
- [ ] Store → Issue Kestrel → G1 shows the chips +90 S, +5 MIN and PICK A TIME.
- [ ] PICK A TIME opens the sheet on two hours from now (rounded to the quarter hour). The wheels spin smoothly, snap to rows, loop past 23 and 59, and tick on every row.
- [ ] The readout updates as you turn: "Today · in 2 h 15 min". Pick a time earlier than now and it says "Tomorrow 08.11.2026 · …".
- [ ] +1 H moves both wheels to now + 1 h, rounded up to 5 minutes.
- [ ] SET RETURN TIME: the chip reads `18:45`, or `18:45 TOMORROW`, in amber. G2 and the receipt show RETURN BY 18:45, and the camera goes overdue at that time.
- [ ] CANCEL or swiping the sheet away leaves the previous choice unchanged.
- [ ] +90 S still works for the demo.
- [ ] It fits at 360 dp wide and at 1.3× font size: the wheels and buttons stay on screen, and the readout wraps.

## Test steps
1. Issue Kestrel with a picked time 2 minutes from now. Wait for it to go overdue.
2. Turn both wheels quickly and use each quick jump.

## Notes
- **Tests:**
  - `ReturnTimeMathTest`: the readout today and tomorrow (including the same minute), under an hour, quickJump rounding across midnight, and wheelIndexFor wrapping.
  - `ReturnTimePickerScreenshotTest`: open on 18:45 with now 14:30, in both themes, and at 360 dp with 1.3× font.
- **Data:** `handovers.due_ms`, unchanged. `DueChoice.At` already turns the clock time into the next occurrence.
