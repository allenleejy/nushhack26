# BottomSheet

A panel that slides up over the current screen for a short choice: picking the receiver for a handover, choosing why a seal was replaced, confirming a report.

`bg-200`, full width, square corners, a 2dp `signal-edge` rule along its top edge and `elevation-sheet` shadow, over a scrim of black at 45%. A square grab bar (`handle`, 32 × 4dp, `line-strong`) at the top, then a header row with the title in `title` (HAND C-12 TO) and a `record-label` note on the right, over a 2dp `line-strong` rule. Content below with `space-4` padding, usually a `ListRow` list, ending with one primary button that repeats the choice (HAND OVER TO SITI).

Swipe down or tap the scrim to dismiss; nothing is saved until the primary button is pressed.

Compose: M3 `ModalBottomSheet` with `shape = RectangleShape`, `containerColor = bg200` and a custom drag handle.

The consumer provides: the title, the note, the content and the primary action.

Don't: stack sheets, put a second primary action in a sheet, or use a sheet for the handover itself (that's a full-screen flow).
