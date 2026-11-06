# Tenet: brand book

A field instrument for custody, drawn like a hazard label. Tenet is an offline Android app (Kotlin, Jetpack Compose, Material 3) in which two people sign every handover of a controlled item, such as a radiography camera holding a sealed source, and swap the record by QR code with no network. The interface must work inside a steel tank, at night, in gloves and in a hurry.

## Principles

1. **Who holds it, at a glance.** Every screen answers "who is liable for this item right now?" before anything else. An amber IN CUSTODY tab means *you*.
2. **Offline is normal.** No network is the expected state, not an error. Waiting records are neutral; only real problems (a fork, an overdue return, a seal mismatch) turn orange.
3. **Show the evidence.** When the app makes a claim ("Ravi handed C-12 over twice"), show the signatures, sequence numbers and seal numbers behind it, in mono, so a person can check them. Nothing is deleted: voided records stay visible.
4. **One next step.** Each screen has one primary action, full width at the bottom, within thumb reach.
5. **Built for gloves and glare.** 48dp minimum targets, 72dp primary buttons, 16sp minimum body text, high contrast in both themes.
6. **Square, and at 0° or 45°.** No rounded corners. Stripes, hatching, cut corners and the hazard diamond all sit at 45°; text runs level or vertically. The countersigned stamp is the one tilted thing.

## Content

- **Voice:** formal and matter-of-fact, like an instrument readout or a logbook entry. Confirm the event in system terms first, then give the facts on the next line: "Custody transfer successful." then "C-12 · Holder: Wei Ming · Return by 19:00". No chatty phrasing, no "you're all set".
- **Casing:** two registers. Everything set in Space Mono is UPPERCASE: headlines, titles, buttons, labels, chips, side tabs, names in the chain, IDs, times and status lines. Everything set in Space Grotesk is sentence case: instructions, explanations, banner messages, helper and empty-state copy. Never set a full sentence of more than a few words in uppercase mono; it slows reading in a hurry.
- **Buttons** are a verb and its object, in uppercase mono: SCAN SEAL, SIGN AND SHOW QR, SCAN ACCEPTANCE, RETURN C-12, DECIDE WHO HOLDS C-12. A disabled button says why: SIGN · SCAN SEAL FIRST.
- **Side tabs and stamps** use one to three words: IN CUSTODY, OVERDUE, HELD, VOID, COUNTERSIGNED.
- **Names and IDs:** people by the name colleagues use (Siti, Ravi, Wei Ming, Dr Tan). Item and source IDs exactly as printed, in `record`: C-12, IR-0457, SEAL 88-2041.
- **Time:** 24-hour, no seconds: 07:30, 19:00. Durations in short words: "5 h 58 min left", "12 min late". A phone time is a claim; say "signed 13:02" for phone times and "arrived 19:41" for server times.
- **Name the person and the evidence** in alerts: "Ravi signed two handovers that both follow record #4." Never an error code alone.
- **Waiting, not failed:** "3 handovers saved on this phone", "Waiting to sync". Reserve "failed" for something that really failed.
- **The regulatory line** on the receiver's acceptance screen is stated outright: "This is sealed source IR-0457."
- No emoji, no exclamation marks, no jokes.

## Colour

Two themes from one set of roles, taken from hazard labels: amber on warm charcoal. **Field** (dark) is the signature theme for night and enclosed spaces; **Daylight** (light) is for sun and the projector. Follow the system setting, with a manual override in Settings.

- Ground is `bg-000` (warm charcoal in Field, never near-black: a softer ground keeps amber and orange from glaring). Content sits on `bg-100` panels; sheets, banners and inset wells use `bg-200`. Separate panels with 2dp rules and tone, not shadows.
- Text is `ink`; secondary text, labels and timestamps are `ink-muted`. Both pass 6:1 or better on `bg-000`, `bg-100` and `bg-200` in both themes.
- **`signal` (amber) is the only accent** and means "you, now": the IN CUSTODY tab, label header bars and outlines, the primary action, the current handover step, hazard stripes, the filled Class 7 diamond and the focus ring in Field. Text on it is `on-signal`. Every amber fill or outline carries a 2dp `signal-edge` (ink in Daylight, where amber on white has no 3:1 edge). Amber text uses `signal-text`, which turns **brown** in Daylight; `signal-bg` is the brown-amber tint behind it.
- **`ok` (cyan)** means verified: synced, both signatures check out, the seal matches, countersigned. Tint `ok-bg`.
- **`alert` (orange)** means a person must act: fork, overdue, seal mismatch, revoked signer. Use `alert` for fills, borders and stripes, and `alert-text` for words and small icons (a shade lighter in Field so it reads on every panel). Text on an alert fill is `on-alert`; tint `alert-bg`.
- **`pending` (neutral)** means waiting to sync. Draw it dashed, never filled.
- `ok` and `alert` are cyan and orange so they never depend on red–green hue, and every status also carries a word and an icon.
- **`inverse`** flips against the theme (light in Field, dark in Daylight) and is used only for the `Snackbar`, with `on-inverse` text, `inverse-action` for its action and `inverse-ok` for its check.
- QR codes are always `qr-ink` on `qr-ground` (black on white) in both themes.

### Material 3 mapping (Compose `ColorScheme`)

| M3 role | Token |
| --- | --- |
| background | `bg-000` |
| surface, surfaceContainer | `bg-100` |
| surfaceContainerHigh, surfaceVariant | `bg-200` |
| onBackground, onSurface | `ink` |
| onSurfaceVariant | `ink-muted` |
| outline | `line-strong` |
| outlineVariant | `line` |
| primary / onPrimary | `signal` / `on-signal` |
| primaryContainer / onPrimaryContainer | `signal-bg` / `ink` |
| tertiary / tertiaryContainer | `ok` / `ok-bg` |
| error / onError | `alert` / `on-alert` |
| errorContainer / onErrorContainer | `alert-bg` / `ink` |
| secondary / onSecondary | `ink` / `bg-100` |
| inverseSurface / inverseOnSurface | `inverse` / `on-inverse` |
| inversePrimary | `inverse-action` |

Keep extra roles (`signal-edge`, `signal-text`, `alert-text`, `pending`, `focus`, `inverse-ok`, `qr-ground`, `qr-ink`) in a small `TenetColors` object provided through a `CompositionLocal`. Turn off dynamic colour.

## Type

Two families from the same design lineage, both on Google Fonts under the SIL Open Font License. Bundle the TTFs in `res/font` so the app never needs the network for type. The system is mono-led: most of what a person sees is Space Mono in uppercase, with Space Grotesk for anything read as a sentence.

- **Space Mono** (`mono`, 400 and 700 only), always UPPERCASE, for everything a person scans, taps or compares: `readout-xl` for the item ID on the custody label (one per screen), `headline` for what the screen is for, `title` for app bars, label and alert headers, `label` for buttons, tabs and names in the chain, `record` for IDs, seal numbers, times and sequence numbers (bold for the one that matters most), `record-label` for field labels, chips, side tabs and status lines, `code-lg` for the six-digit radio check code in groups of three ("305 917").
- **Space Grotesk** (`sans`), sentence case, for reading: `body-lg` for instructions in the handover flow and alert explanations, `body` for banner messages, helper text and empty states, `body-strong` to stress a name or fact inside a sentence. No sentence is ever below 16sp.
- Space Mono's zero and capital O are close. Item IDs, seal numbers and source IDs use digits and avoid the letter O, so the two never meet in a value a person compares.
- Space Mono has no arrow glyph: draw "Giver → Receiver" with the `arrow_forward` icon, not a text character.
- **Rotated text** reads bottom to top (`angle-text`), in bold uppercase mono with wide tracking, only on a solid side tab (`tab-width`, 36dp): the custody label's state, the QR caption, the VOID strip. One to three words. In Compose, swap width and height in a layout modifier before rotating, so the tab measures correctly.

Sizes are in sp. Layouts must still work at 1.3× system font size: let rows grow, never clip.

## Space, size and shape

- 4dp grid: `space-1` 4 through `space-12` 48. Screen gutter and card padding `space-4`; between cards and sections `space-6`.
- Touch: nothing tappable under `target-min` (48dp). List rows and secondary buttons `target-row` (64dp). The primary action `target-primary` (72dp), full width minus gutters, pinned above the gesture bar.
- Frame: 412 × 915dp phone. **Screen anatomy, top to bottom:** `TopAppBar` (`app-bar`, 64dp) with the phone's `SyncChip`; the `OfflineBanner` when offline; content with a `space-4` gutter; the one primary action pinned above the `BottomNav` (`nav-bar`, 80dp). Inside the handover flow the bottom nav is hidden and the app bar carries a hazard band instead of its rule.
- Short choices open in a `BottomSheet`; confirmations appear as a `Snackbar` above the primary action; decisions are never a snackbar.
- **Corners are square** (`radius-none`) everywhere: buttons, chips, fields, cards, labels, sheets, the QR panel, focus rings, step markers, timeline markers. Compose: `RectangleShape` for every M3 shape slot.
- **Cut corners** are the one exception: a 45° cut on the top-right corner of the primary button (`cut-md`, 16dp) and of the Synced and Fork chips (`cut-sm`, 8dp). Nothing else. Compose: `CutCornerShape(topEnd = …)`.
- **Label anatomy** for anything that states custody or a warning: a 2dp outline, a solid header bar or side tab, content in boxed cells divided by 2dp rules, and an optional stripe band at the foot. See `CustodyCard` and `ForkAlert`.
- Borders: `stroke-control` (2dp) for controls, outlines and cell rules; `stroke-hairline` (1dp) in `line` for dividers inside panels.
- Elevation: everything in the page is `elevation-0`. Only bottom sheets and the snackbar use `elevation-sheet`.

## Patterns and marks

- **Hazard stripes** (`stripe-width` 8dp, `stripe-band` 12dp, at `angle-stripe`) mean attention. `on-signal` on `signal` marks what you hold and the moment liability moves; `alert` on `alert-bg` marks overdue and forks; `ink` on `bg-000` is a divider before a liability step. At most two striped things per screen, never behind text. See `HazardBand`, `CountdownBar`, `LiabilityLine`.
- **Hatching** (`hatch-line` 3dp every `hatch-pitch` 10dp, `line` over `bg-200`) means not available. It strikes out disabled buttons and voided records. Stripes are bold; hatching is quiet. Never swap them.
- **The Class 7 diamond**, a square at 45° carrying a 7 (the UN transport class for radioactive material), marks every sealed source. See `HazardDiamond`.
- **The countersigned stamp** (`stroke-stamp`, rotated `angle-stamp`) appears only on a receipt once the receiver's signature verifies. See `Receipt`.
- **Registration marks** ("+" at the corners) frame summary panels on the supervisor's audit screens, and nothing else. See `RecordPanel`.

## States

- **Focus:** a solid 3dp (`stroke-focus`) square ring in `focus`, 2dp outside the shape. 6:1 or better on every surface in both themes.
- **Pressed:** the fill moves one step (`bg-100` → `bg-200`; signal fills darken 8% via a state layer). No ripple colour other than `ink` at 12%.
- **Disabled:** hatched out, with a dashed `line-strong` border, the label in `ink-disabled` and the reason on a solid patch ("Sign · scan seal first").
- **Void:** hatched, struck through in `ink-muted`, and a VOID side strip. Still tappable, still in the chain.
- **Sync:** synced, waiting, conflict and offline look the same everywhere: see `SyncChip`.
- **Due:** solid bar, then stripes in the last hour, then orange stripes when overdue: see `CountdownBar`.
- **Empty states** say what will appear and how: "No items yet. Items signed over to you appear here." with a large icon for the kind of list and one action if there is one. Never the Class 7 diamond there: it reads as a count.
- **Loading:** `bg-200` skeleton blocks, square, no shimmer. Local data loads instantly, so skeletons only appear for server-only views.
- **Motion:** short and functional, 150–200ms. The custody label switches to the new holder once, on the moment of signing; the stamp appears once. Stripes and hatching never move. Respect "remove animations".

## Iconography

Material Symbols **Sharp**, weight 500, 24dp in rows, chips and buttons (`icon-md`), 32dp in banners and alerts (`icon-lg`). In Compose use `Icons.Sharp.*` from `material-icons-extended`. Outlined by default; filled only for an active state (the alarm when overdue, the warning symbol in an alert header). Icons always sit beside a word, never alone, except standard app bar icons with content descriptions. The Class 7 diamond is drawn, not an icon.

| Meaning | Symbol |
| --- | --- |
| Synced | `cloud_done` |
| Waiting to sync | `schedule` |
| Offline | `cloud_off` |
| Syncing | `sync` |
| Fork / conflict | `call_split` |
| Warning header | `warning` |
| Seal matches, signatures verified | `verified` |
| Seal mismatch | `gpp_bad` |
| Overdue | `alarm` |
| Show QR | `qr_code_2` |
| Scan | `qr_code_scanner` |
| Giver → receiver | `arrow_forward` |
| Hand over | `swap_horiz` |
| Supervisor decision | `gavel` |
| Report missing | `report` |

There is no logo yet: set TENET in Space Mono 700. The launcher icon is the filled Class 7 diamond on a `bg-000` square.
