# ListRow

A tappable row in a square list: people on the roster, items in the store, records in the outbox.

At least `target-row` (64dp) tall, `space-4` side padding, inside a `bg-100` list with a 2dp `line` border and hairline dividers. Three slots:
- **Leading**: a 40dp square with initials in bold mono and a 2dp `line-strong` border for a person, or a `HazardDiamond` for a sealed source.
- **Text**: the title in bold uppercase mono at 16sp (WEI MING, C-07 · IR-0391), then one supporting line in `body` at 14sp, sentence case (Radiographer · authorised).
- **Trailing**: a short mono word with an icon (PICK ›), a `SyncChip`, or nothing.

**Selected**: `signal-bg` fill, a 6dp `signal` bar on the left edge, the leading square filled `signal` with `on-signal` initials, trailing SELECTED with a check in `signal-text`.
**Blocked**: someone who may not accept a source (registration expired, access revoked) is hatched, their name struck through, the reason as the supporting line, BLOCKED on the right, and the row can't be picked.

Compose: M3 `ListItem` with `RectangleShape` and custom leading content.

The consumer provides: leading (initials or item), title, supporting text, trailing content, selected or blocked state and the onClick.

Don't: hide blocked people (show them, struck out, so the reason is clear), or put two lines of supporting text.
