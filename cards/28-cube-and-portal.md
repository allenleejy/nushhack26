# 28: The tap moment as a sealed cube and a portal

Wave: 7 (motion) · Devin mode: Normal · Depends on: the prep on `main` (`TapRole` and `TapPulse`'s new `role` parameter; ReceiveScreen already passes `TapRole.Receiver`)

## Goal
The handover looks like the camera itself moving between phones. On the giver's phone, a sealed cube spins and shrinks into a glowing amber circle. On the receiver's, it comes out of a cyan one. The giver then waits on a dashed outline of the cube with an amber line tracing round it.

## Source of truth
The design canvas **https://claude.ai/artifact/2B5tMaPVPTC3Ajzdb2BHhH**, board "Hand over · the cube and the portal" (`project/Box3D.dc.html` and its stylesheet). This card copies every number from its CSS keyframes (`gbFly`, `gbRig`, `rbFly`, `rbRig`, `gPortal`, `rPortal`, `glow`, `turn`, `run3`, `.rig3.cube`, `.portal`, `.trace3`). Where this card and the board disagree, follow this card. If you can't open the link, this card is complete without it.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/TapPulse.kt
- app/src/main/java/com/nushhack26/app/ui/components/SealedCube.kt (new)
- app/src/test/java/com/nushhack26/app/screenshots/TapPulseScreenshotTest.kt
- app/src/test/java/com/nushhack26/app/ui/components/SealedCubeTest.kt (new)

Keep `TapPulse`'s signature (`phase, title, subtitle, modifier, onFlooded, role`) and `TapPhase` and `TapRole` exactly as they are on `main`. GiveScreen and ReceiveScreen call them, and card 29 edits those screens in the same wave. Don't edit `Flood.kt`. TapPulse simply stops using `LocalFlood`.

## Build

### 1. `SealedCube.kt`: the cube, drawn in a Canvas
Compose has no 3D transforms for children, so project it yourself. Use no libraries.

- **Units:** design units, where the cube's edge E = 120. On screen, E = **137 dp** (the circle is 240 dp, and the design's is 210 units).
- **Geometry:** a cube centred on the origin, x right, y down, z towards the viewer. Corners at ±60.
- **Pose:** apply `rotateY(θ)` first, then `rotateX(−22°)`, which is CSS's `rotateX(-22deg) rotateY(θ)`. With CSS's rotation formulas:
  - rotateY(a): x′ = x·cos a + z·sin a, z′ = −x·sin a + z·cos a
  - rotateX(a): y′ = y·cos a − z·sin a, z′ = y·sin a + z·cos a

  With rotateX at −22°, the lid tips towards the viewer.
- **Perspective:** 760 units. A screen point is (x, y) · 760 / (760 − z), then scaled by 137/120 dp and centred.
- **Faces:** draw only the faces whose normal points at the viewer after rotation (backface culling). That's enough for a convex cube; draw them far-to-near anyway. Each face gets a solid fill and a 2 dp `lineStrong` outline.

  | Face | Where | Fill (Tenet colour tokens) |
  |---|---|---|
  | front | z = +60 | `lerp(line, ink, 0.2f)` |
  | back | z = −60 | `bg100` |
  | right | x = +60 | `line` |
  | left | x = −60 | `bg200` |
  | top (lid) | y = −60 | `lerp(line, ink, 0.4f)` |
  | bottom | y = +60 | `bg000` |
- **Seal tape**, `signal` fill, with 2 dp `signalEdge` lines along both long sides:
  - a band 26 units wide, centred left to right (x from −13 to +13)
  - on the lid, running front to back
  - on the front and the back, running from the top edge down 52 units

  So the tape runs over the lid and down two faces. Build each tape piece from its four 3D corners and project them, so it bends with perspective. Don't interpolate in 2D.
- **Class 7 diamond** on the front: a square 24 units a side, turned 45° (standing on a corner), centred at x = +34, y = +30 from the face's centre. `signal` fill, 2 dp `signalEdge` outline, no text.
- **Rest pose:** θ = −35°. The front, right and lid faces show. That's the board's "SEALED" cube.
- **API:** `@Composable fun SealedCube(angleY: () -> Float, scale: () -> Float, modifier: Modifier)`, plus a pure `internal fun cubeFaces(angleYDeg: Float, edgePx: Float): List<ProjectedFace>` (visible faces, far to near, with their projected corners and the tape and diamond polygons), so the projection can be unit-tested.
- **Performance:** read `angleY()` and `scale()` only inside the draw lambda, and reuse `Path` objects with `drawWithCache`.

### 2. The portal (the glowing circle)
- A 240 dp circle filled with a radial gradient out to the circle's edge:
  - the colour at 62 % alpha at the centre, 40 % at 60 % of the radius, and 0 % at 93 %
  - `signal` for `TapRole.Giver`, `ok` for `TapRole.Receiver`
- **Glow** while it waits: scale 0.94 ↔ 1.05, 2000 ms each way, `FastOutSlowInEasing` (0.4, 0, 0.2, 1), alternating.
- **The one round thing in the app:** the circle and the stamp's tilt are the only exceptions to "square, 0° or 45°". Everything else in the component stays square.

### 3. The traced outline (the giver's Linked phase)
- The cube's rest silhouette as a path in a 181 × 183 viewBox, scaled by 137/120 and offset (−1, +6) units from centre:
  - inner edges: `M108.7 57.1 L4.0 32.5 M108.7 57.1 L107.6 178.8 M108.7 57.1 L176.3 23.2`
  - outline polygon: `4.0,32.5 78.0,4.0 176.3,23.2 171.5,132.9 107.6,178.8 9.1,145.5`
- **Base:** the inner edges and the outline, 2 dp stroke in `pending`, dashed 6 on / 5 off (in viewBox units, scaled).
- **Runner:** the outline polygon again, 4 dp stroke in `signal`, square caps, miter joins, dashed 70 on / 515 off. Its phase moves from 0 to −585 units in 2200 ms, linear, repeating, so one amber segment laps the outline every 2.2 s.

### 4. `TapPulse`: phases and timings
Layout: a 260 dp tall stage holding the circle and the cube, centred and not clipped, so the circle can open to 1.3× over its neighbours. Under it, `title` uppercase in mono (`t-label`) and `subtitle` in `body`, `inkMuted`, as now.

**Idle turn:** whenever the cube shows and isn't flying, θ advances 360° every 9000 ms, linear, from −35°.

**Waiting**
- Giver: the amber circle glows, and the cube sits in it, turning.
- Receiver: the cyan circle glows, empty.

**Contact on the giver** (t = 0 when `phase` becomes Contact). The board's 6 s loop has contact at 35 %, so these are its keyframes in ms from contact:

| t (ms) | Cube | Circle | Haptics |
|---|---|---|---|
| 0 | | starts opening, 1 → 1.3 over 0–420, `tween` with the default `FastOutSlowInEasing` | `haptics.click()` |
| 0–90 | punch, scale 1 → 1.15 | | |
| 90–180 | back to 1.0; θ holds | | |
| 180–900 | scale 1 → 0.04; blur 0 → 10 dp; θ += 900° (2½ turns). Easing `TenetMotion.EmphasizedAccelerate` (0.3, 0, 0.8, 0.15) on all three. | | |
| 420–1140 | | 1.3 → 0, `EmphasizedAccelerate` | |
| 900 | hidden | | `haptics.thud()`; call `onFlooded()` once |

The caller then switches to Linked. Keep the circle's closing animation running across that switch, using the same state holder, so it finishes at 1140 ms.

**Linked** (giver only): the traced outline (section 3) in the stage, in place of the cube. No circle.

**Contact on the receiver** (t = 0 when `phase` becomes Contact; the board's 46 %):

| t (ms) | Circle | Cube | Haptics |
|---|---|---|---|
| 0 | | | `haptics.click()` |
| 0–360 | 1 → 1.3 | | |
| 360 | | appears at scale 0.04, blur 10 dp, θ = −935° | |
| 360–1080 | 1.3 → 1.2 | | |
| 360–1200 | | scale → 1.0, blur → 0, θ → −35° (2½ turns), `TenetMotion.EmphasizedDecelerate` (0.05, 0.7, 0.1, 1) | |
| 1200 | | lands | `haptics.thud()` |
| 1200–1320 | | → 1.04 | |
| 1320–1560 | | → 1.0, then the idle turn | |
| 1080–1680 | 1.2 → 0, `EmphasizedAccelerate` | | |
| 1680 | | | call `onFlooded()` once |

The receiver has no Linked phase. If it's ever passed one, draw the cube at rest.

**Blur:** `Modifier.blur(r, BlurredEdgeTreatment.Unbounded)` on API 31 and up, applied to the cube's layer only. Below 31, fade the cube's alpha from 1 to 0.5 instead, using the same timing.

**Text:** the title and subtitle are whatever the caller passes. Don't show LINKING…, and remove the old square core, the rings and the flood.

### 5. Reduce motion, inspection mode and Robolectric
Keep the existing `still` check (`LocalInspectionMode || LocalReduceMotion || inRobolectric`). When it's true:
- no glow, no idle turn and no blur
- the cube at rest, θ = −35°
- the trace drawn with its runner at phase 0
- Giver Contact: click, the cube and circle vanish, thud, then call `onFlooded()` at once.
- Receiver Contact: click, the cube appears at rest and the circle is gone, thud, then call `onFlooded()` at once.

`onFlooded` must fire exactly once per Contact on every path, so a handover can never stall.

## Offline behaviour
- Pure UI. The tap and the link work as now.

## Acceptance criteria (checked on a phone)
- [ ] G3 Tap on Ravi's phone: an amber glowing circle with the sealed cube turning slowly in it. The lid tape runs down the front, and the 45° diamond is on the front.
- [ ] Wei Ming's RECEIVE tab: an empty cyan glowing circle.
- [ ] Tap the phones: Ravi's cube punches, spins and shrinks into the circle with a blur, the circle closes, and a thud is felt. Then the dashed outline with an amber line lapping it every ~2 s, and LINKED.
- [ ] On Wei Ming's phone, the cube spins out of the cyan circle, lands with a thud and a small bounce, the circle closes, and the checklist follows.
- [ ] The handover always continues, including with Remove animations on, where both phones jump straight to the end states and still buzz.
- [ ] The cube looks solid from every angle while it spins: no face shows through another, and the tape stays on its faces.
- [ ] It fits at 360 dp wide and at 1.3× font size, and the open circle doesn't push the buttons off screen.

## Test steps
1. Hand Kestrel from Ravi to Wei Ming by tap and watch both phones.
2. Repeat with Remove animations on.

## Notes
- **Tests:**
  - `SealedCubeTest`: at θ = −35° the visible faces are front, right and top, and back, left and bottom are culled. At θ = 145° the back face shows. Every projected point of the front face lies within ±1 px of the expected value for one hand-computed corner.
  - `TapPulseScreenshotTest` (both themes): giver waiting, giver linked (the trace), receiver waiting, receiver landed, and giver waiting at 360 dp with 1.3× font.
- **Brand:** `design/` is read-only, so the round circle is recorded in docs/spec.md §3 instead ("The tap moment: the cube and the portal").
