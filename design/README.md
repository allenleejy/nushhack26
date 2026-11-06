# Tenet design system

Tenet is our offline Android app for signed custody handovers (Kotlin, Jetpack Compose, Material 3). This folder is the complete design system: the brand rules, the tokens, every component with its guidelines and a live preview, a starter Compose theme and the fonts. Teammates and AI agents should build every screen from it.

Exported 6 Nov 2026 from the Tenet design system artifact on claude.ai. The artifact is the source of truth: change it there and re-export, rather than editing files here by hand.

## Start here

| If you are… | Read |
| --- | --- |
| A person who wants to see it | Open `index.html` in a browser: every component, live, with a Field/Daylight switch. If previews stay blank, run `python3 -m http.server` in this folder and open `http://localhost:8000`. |
| An agent or designer making screens | `brand.md` first (voice, colour, type, shape, patterns, states, icons), then the `README.md` of each component you use. |
| The build account or Devin writing Compose | `android/README.md`, then copy `android/*.kt` into `ui/theme/` and `fonts/*.ttf` into `res/font/`. Build each component to its `components/<Name>/README.md`. |
| Claude Design, writing a brief | Attach `brand.md`, `tokens.json` and the two images in `reference/`, and name the components the screen needs. |

## The rules in one minute

- **Who holds it, at a glance.** An amber IN CUSTODY side tab means *you* are liable.
- **Two themes.** Field (dark, warm charcoal) is the signature theme; Daylight (light) is for sun and projectors. One accent: amber `signal`. Cyan `ok` = verified. Orange `alert` = a person must act. Neutral dashed `pending` = waiting to sync.
- **Type.** Space Mono, ALWAYS UPPERCASE, for anything you scan, tap or compare. Space Grotesk, sentence case, for sentences. Never a long sentence in uppercase.
- **Shape.** Square corners everywhere. The only cut corners (45°, top right) are on the primary button and the Synced and Fork chips.
- **Marks.** Hazard stripes mean attention; thin hatching means not available (disabled, void). The Class 7 diamond marks a sealed source. The countersigned stamp is the one tilted thing.
- **Voice.** Formal and matter-of-fact: "Custody transfer successful." then the facts. No emoji, no exclamation marks.
- **Field use.** 48dp minimum targets, 72dp primary button, 16sp minimum for sentences, one primary action per screen. QR codes are always black on white.

## Files

| Path | What |
| --- | --- |
| `brand.md` | The brand book: principles, content, colour (with the Material 3 mapping), type, space and shape, patterns, states, iconography |
| `tokens.json` | Every token: colours per theme, type styles, spacing, sizes, strokes, shadows, angles. Each has a usage note. |
| `tokens.css` | The same tokens as CSS custom properties, `[data-theme="field"]` and `[data-theme="daylight"]`, a class per type style, and `@font-face` rules for `fonts/` |
| `components/<Name>/README.md` | Guidelines for one component: anatomy, states, tokens, what the consumer provides, do's and don'ts |
| `components/<Name>/preview.html` | A live HTML preview of that component. Add `?theme=daylight` to the URL for the light theme. |
| `components/bundle.css` | The styles the previews share (HTML reference, not app code) |
| `components/Cover/preview.html` | The system's cover |
| `index.html` | Gallery of every component |
| `android/` | Starter Compose theme generated from the tokens: colours, type, shapes, dimensions, stripe and hatching modifiers, `TenetTheme` |
| `fonts/` | Space Mono and Space Grotesk TTFs (SIL OFL), Material Symbols Sharp for the web previews (Apache 2.0) |
| `reference/` | The mood references the system grew from |

## Components

| Group | Component | Summary |
| --- | --- | --- |
| Actions | [`Button`](components/Button/README.md) | The one primary action per screen (amber, cut corner, 72dp), plus secondary, danger, text and hatched-out disabled buttons. |
| Status | [`CountdownBar`](components/CountdownBar/README.md) | Time left before an item is due back at the store, shown as a bar under the custody label. |
| Status | [`HazardBand`](components/HazardBand/README.md) | The system's two 45° patterns: stripes, which mean attention, and hatching, which means not available. |
| Status | [`OfflineBanner`](components/OfflineBanner/README.md) | A full-width strip under the top app bar that says whether this phone can reach the server and how many records are waiting. |
| Status | [`SyncChip`](components/SyncChip/README.md) | A compact label showing where one record, or this phone's outbox, stands against the server. |
| Custody | [`CustodyCard`](components/CustodyCard/README.md) | The answer to "who holds this item right now?" for one item, drawn as a hazard label: the hero of the home screen and the item detail screen. |
| Custody | [`ForkAlert`](components/ForkAlert/README.md) | The warning label a supervisor sees when the server finds two handovers from the same giver that both follow the same record: the double handover the system can't stop offline but can prove. |
| Custody | [`HazardDiamond`](components/HazardDiamond/README.md) | A square turned 45° carrying the figure 7, the UN dangerous-goods class for radioactive material. |
| Custody | [`RecordPanel`](components/RecordPanel/README.md) | A summary panel set like a printed form, for the supervisor's audit and export screens: the daily location record, the movements in and out of storage, today's counts. |
| Custody | [`TimelineRow`](components/TimelineRow/README.md) | One record in an item's custody chain, newest at the bottom, inside a bg-100 list with 2dp line dividers and square corners. |
| Handover | [`HandoverStep`](components/HandoverStep/README.md) | The parts of the handover flow: a three-step stepper, the seal check result, and the QR panel that carries an offer or an acceptance between two phones. |
| Handover | [`LiabilityLine`](components/LiabilityLine/README.md) | The block that marks the moment liability moves. |
| Handover | [`RadioCheck`](components/RadioCheck/README.md) | The six-digit check a supervisor runs over the radio, offline, to confirm that a holder's phone has an item's latest custody record. |
| Handover | [`Receipt`](components/Receipt/README.md) | The giver's proof that a handover happened, and the one place the system breaks its own 0°/45° rule. |
| Inputs | [`TextField`](components/TextField/README.md) | A square input with its label above, for the few things a person types: a seal number that won't scan, a note, a search. |
| Navigation | [`BottomNav`](components/BottomNav/README.md) | Three destinations along the bottom of the top-level screens. |
| Navigation | [`TopAppBar`](components/TopAppBar/README.md) | The bar at the top of every screen: where you are, a way back, and this phone's sync state. |
| Containers | [`BottomSheet`](components/BottomSheet/README.md) | A panel that slides up over the current screen for a short choice: picking the receiver for a handover, choosing why a seal was replaced, confirming a report. |
| Containers | [`ListRow`](components/ListRow/README.md) | A tappable row in a square list: people on the roster, items in the store, records in the outbox. |
| Feedback | [`EmptyState`](components/EmptyState/README.md) | What a list shows when there's nothing in it yet: what will appear, how to get it, and one action. |
| Feedback | [`LoadingSkeleton`](components/LoadingSkeleton/README.md) | Placeholders while a server-only view loads (the supervisor's chain from all phones, today's records), and a segmented bar while records send. |
| Feedback | [`Snackbar`](components/Snackbar/README.md) | A brief message at the bottom of the screen, above the primary action, that confirms an event or offers one follow-up. |

## Note on this folder

The hackathon kit expects the Claude Design export in `design/` as well. Extract that zip into `design/screens/` so it sits beside this system without overwriting it.
