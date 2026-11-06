# CustodyCard

The answer to "who holds this item right now?" for one item, drawn as a hazard label: the hero of the home screen and the item detail screen.

Anatomy, left to right and top to bottom:
1. **Side tab**, `tab-width` (36dp) wide, full height, with rotated text reading bottom to top in bold uppercase mono: IN CUSTODY, OVERDUE or HELD.
2. **ID block**: a small `record-label` line above the item ID in `readout-xl` (64sp), with the `HazardDiamond` (Class 7) top right.
3. **Cells**: three boxed cells divided by 2dp rules: source (or signed time), return time, sync state. Each is a `record-label` over a bold `record` value.
4. **Foot**: a `HazardBand` across the bottom edge.

| State | Tab | Border and rules | ID | Foot |
| --- | --- | --- | --- | --- |
| in custody (you are liable) | `signal` fill, `on-signal` text | `signal-edge` | `signal-text` | `on-signal` stripes on `signal` |
| overdue | `alert` fill, `on-alert` text | `alert`, body `alert-bg` | `ink`; "Was due" value in `alert-text` | `alert` stripes on `alert-bg` |
| held (someone else) | `bg-200`, `ink-muted` text | `line-strong` | `ink`, 40sp | none: nothing here is yours |

Square corners throughout. In Compose, rotate the tab text with a layout modifier that swaps width and height before `Modifier.rotate(-90f)`, so the tab measures correctly.

The consumer provides: item ID, source ID, holder (or you), signed time, due time, sync state. Tapping the card opens the custody chain. Pair it with a `CountdownBar` directly below on the holder's home screen.

Don't: show a holder the signatures don't support (read only from the latest verified record), use the amber tab for anything but "you hold this", or add more than three cells.
