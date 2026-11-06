# 20: The tap moment: pulse, punch and flood

Wave: 4 (polish) · Devin mode: Normal · Depends on: the polish prep on `main` (the `TapPulse` and `FloodHost` stubs with their final signatures, `Haptics`, `TenetMotion`, `LocalReduceMotion`)

## Goal
The tap is the moment judges remember. On both phones, an amber square breathes while it waits, punches and clicks when the phones touch, then floods the whole screen with a thud and shrinks up into the hazard band. The giver's then waits in calm cyan.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/TapPulse.kt
- app/src/main/java/com/nushhack26/app/ui/components/Flood.kt
- app/src/test/java/com/nushhack26/app/screenshots/TapPulseScreenshotTest.kt (new)

Keep every public signature in both files exactly as it is on `main`: cards 18 and 19 call them in the same wave. Fill in the bodies only.

## Build
- **The shape is square, never round** (`design/brand.md`: square, at 0° or 45°):
  - a 160 dp `signal` core with a 2 dp `signalEdge` border
  - `Icons.Sharp.Contactless` 48 dp in `onSignal` at its centre
  - square rings around it
- **Waiting:**
  - the core breathes: scale 1.00 ↔ 1.04, 1400 ms, `FastOutSlowInEasing`, `RepeatMode.Reverse`
  - every 1400 ms, a 2 dp `signal` ring leaves the core's edge and grows to 1.8× while fading to 0, 1400 ms, `LinearOutSlowInEasing`
  - two rings are in flight at any time, staggered by 700 ms
- **Contact**, on entering the phase:
  1. `haptics.click()`
  2. the core punches: scale 1.0 → 1.18 → 1.0 with `spring(dampingRatio = 0.45f, stiffness = 500f)`
  3. one fast ring, 300 ms
  4. all rings turn to 45° (`tween(150)`)
  5. the title cross-slides (no fade) to "LINKING…"
  6. 250 ms after entering the phase, `LocalFlood.current.play(core centre in root coordinates) { onFlooded() }`
- **Linked:**
  - the core and rings in `ok` (cyan) with `onSignal` replaced by `bg000` for the icon
  - breathing at 2000 ms, one ring at a time
  - title and subtitle as passed
- **Flood**, `FloodHost`'s real controller, drawn in an overlay over everything in `AppScaffold`:
  1. a `signal` square grows from `origin` until it covers the screen: 380 ms, `TenetMotion.EmphasizedAccelerate`
  2. on full cover: `haptics.thud()`, then `onCovered()`
  3. hold 100 ms
  4. the square's bottom edge rises to the bottom of the top bar's hazard band (status bar inset + 64 dp + 6 dp): 240 ms, `TenetMotion.EmphasizedDecelerate`
  5. then the overlay is gone

  The overlay is pointer-transparent except while covering, so it never eats taps. Only one flood plays at a time; a second `play` while one runs calls its `onCovered` at once.
- **Reduce motion** (`LocalReduceMotion.current`): no breathing, no rings moving (draw two still rings at 1.3× and 1.6×), no punch, no flood: `onCovered` at once. The haptics still fire (click on contact, thud at cover).
- **Screenshot tests:** when `LocalInspectionMode.current` is true, draw a still frame (the core at 1.0, rings at 1.3× and 1.6×) and never start infinite transitions, so Roborazzi captures settle.
- **Performance:**
  - read animated values only inside `drawBehind` or `graphicsLayer` lambdas, never in composition
  - no per-frame recomposition
  - rings are `drawRect` with `Stroke`
- **Accessibility:**
  - the core has `contentDescription` = the title
  - the title has `liveRegion = LiveRegionMode.Polite`, so TalkBack announces "LINKING…" and "LINKED"
  - the whole thing is at most 160 dp + text tall, so it fits at 360 dp and 1.3× font

## Offline behaviour
- Pure UI. Nothing touches the network.

## Acceptance criteria (checked on a phone)
- [ ] With card 18 or 19 merged (or a temporary Debug button that cycles the phases, removed before the PR): Waiting breathes and ripples square rings; Contact punches, clicks, turns the rings to 45°, then floods amber with a thud.
- [ ] The amber shrinks up into the hazard band under the top bar.
- [ ] Linked is cyan and calmer.
- [ ] Remove animations on: still rings, no flood, and the click and thud are still felt.
- [ ] No dropped frames that you can see on the Redmi Note 11 Pro+ (Android 13).
- [ ] TalkBack reads the title changes.

## Test steps
1. Build card 18 and 19's flow (or the debug harness): open G3 on one phone and R1 on the other, then tap.
2. Developer options → Remove animations → repeat.

## Notes
- **Uses:** `rememberHaptics()`, `TenetMotion`, `LocalReduceMotion`, `Tenet.colors`.
- **Screenshots:** `TapPulseScreenshotTest`, one for each phase, in both themes.
