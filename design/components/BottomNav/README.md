# BottomNav

Three destinations along the bottom of the top-level screens. Hidden inside the handover flow, which takes the full screen.

`nav-bar` (80dp) on `bg-100` with a 2dp `line-strong` top rule, three equal square cells divided by `line` hairlines. Each cell: a 24dp Material Symbols Sharp icon over a label in bold uppercase mono, 12sp.

**Selected**: the cell fills with `signal-bg`, a 6dp `signal` bar runs along its top edge over the rule, the icon fills in `signal-text`, and the label turns `ink`. **Badge**: a square `alert` block with the count in `on-alert` mono, at the icon's top right; only for alerts that need action.

Destinations by role:
- Radiographer: CUSTODY (`inventory_2`), SCAN (`qr_code_scanner`), OUTBOX (`outbox`).
- Store officer: ITEMS, SCAN, OUTBOX.
- Supervisor: ITEMS, ALERTS (`warning`), RECORDS (`description`).

Compose: M3 `NavigationBar` with `indicatorColor = Color.Transparent` and the bar and fill drawn on the selected item.

The consumer provides: the destinations, the selected one and any alert count.

Don't: show more than three destinations, use a round indicator pill, or badge anything that isn't an alert.
