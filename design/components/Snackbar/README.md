# Snackbar

A brief message at the bottom of the screen, above the primary action, that confirms an event or offers one follow-up. The only surface that flips against the theme (`inverse`), so it stands apart from panels.

Square, at least 56dp tall, `inverse` with `on-inverse` text and `elevation-sheet`. An 8dp marker strip on the left edge says the kind:
- **confirmation**: `inverse-ok` strip and check. The voice rule applies: the event in system terms, in bold uppercase mono ("CUSTODY TRANSFER SUCCESSFUL."), then the facts in mono below (C-12 · HOLDER: WEI MING · 13:02).
- **information**: `on-inverse` strip, a sentence in `body` ("3 handovers saved. They will send when online.").
- **problem**: a 14dp strip of `alert` hazard stripes; a short bold title and what to do.

One optional action on the right in bold uppercase mono, `inverse-action` (brown in Field, amber in Daylight): OUTBOX, TYPE IT, UNDO.

Show for 4 seconds, or 8 with an action. Never for anything that needs a decision: that's a `ForkAlert` or a dialog.

Compose: M3 `Snackbar` with `shape = RectangleShape`, `containerColor = inverse`, `actionColor = inverseAction`, and the strip drawn in the content.

The consumer provides: the kind, the message (title and facts, or a sentence) and an optional action.

Don't: stack snackbars, use one for a fork or an overdue return, or put more than one action.
