# Button

One `primary` per screen, full width at the bottom, 72dp tall (`target-primary`): the next step of the process, written as a verb and its object in `label` (Space Mono 700, uppercase): SIGN AND SHOW QR, SCAN ACCEPTANCE, RETURN C-12.

- **primary**: `signal` fill, `on-signal` label, 2dp `signal-edge` border, and the system's 45° cut on the top-right corner (`cut-md`). Compose: M3 `Button` with `shape = CutCornerShape(topEnd = 16.dp)`, `primary` / `onPrimary`, `BorderStroke(2.dp, signalEdge)`.
- **secondary**: transparent with a 2dp `line-strong` border, 64dp tall (`target-row`), square. M3 `OutlinedButton` with `RectangleShape`. The alternative path on the same screen (SCAN SEAL before signing, SHOW MY RECEIPT).
- **danger**: `alert` border, `alert-text` label. Only for acts with consequences: REPORT MISSING, REFUSE HANDOVER. Never the primary.
- **text**: underlined `ink` label, 48dp target. Navigation and extras (VIEW CHAIN, NOT NOW).
- **disabled (hatched out)**: the button stays full size but is struck out: `bg-200` with 45° hatching (`line`, `hatch-line` every `hatch-pitch`), a dashed 2dp `line-strong` border, no cut corner, and the reason on a solid `bg-000` patch in `ink-muted` (SIGN · SCAN SEAL FIRST). Compose: draw the hatching in `Modifier.drawBehind`.
- **focus**: 3dp `focus` ring, 2dp outside, square.

The consumer provides: the label, an optional leading icon (Material Symbols Sharp, 24dp), the onClick, enabled state and, when disabled, the reason.

Don't: put two primaries on one screen, cut corners on anything but the primary button and the Synced and Fork chips, or grey a button out without hatching and a reason.
