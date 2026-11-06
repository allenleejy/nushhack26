# TextField

A square input with its label above, for the few things a person types: a seal number that won't scan, a note, a search.

- **Label** above the box in bold `record-label` (uppercase mono, 13sp), `ink-muted`; `signal-text` while focused; `alert-text` on error.
- **Box**: `field-height` (64dp), `bg-100`, 2dp `line-strong` border, square. Values a person compares (seal numbers, IDs) are set in bold uppercase Space Mono at 20sp; free text (notes) in `body`, sentence case. A trailing 24dp icon can scan instead (`qr_code_scanner`) or show the result (`gpp_bad`).
- **Focused**: 2dp `signal-edge` border plus a 4dp `signal` bar along the inside bottom edge, and a square `signal-text` caret.
- **Error**: `alert` border on `alert-bg`, and the help line becomes the reason in `alert-text`, naming what was expected ("Doesn't match C-12. Expected 88-2041.").
- **Disabled**: hatched out like a disabled button: dashed border, `bg-200` with `line` hatching, the reason as the value in `ink-disabled`.
- **Help** below in `body` at 14sp, `ink-muted`, sentence case.

Compose: `OutlinedTextField` with `shape = RectangleShape`, the label moved outside the box, `focusedBorderColor = signalEdge`, `errorBorderColor = alert`.

The consumer provides: the label, value, placeholder, help or error text, keyboard type and an optional trailing action.

Don't: use a floating label, round the box, or ask for typing when a scan will do.
