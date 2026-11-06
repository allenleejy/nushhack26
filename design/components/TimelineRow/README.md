# TimelineRow

One record in an item's custody chain, newest at the bottom, inside a `bg-100` list with 2dp `line` dividers and square corners.

Each row: a square marker on the rail, the handover as GIVER → RECEIVER in `label` (uppercase Space Mono, with the `arrow_forward` icon between names), a status line in uppercase mono with icon, a meta line (`#sequence · what it was`), and the giver's phone time on the right in bold `record`. If the server's arrival time differs by more than an hour, add ARRIVED 19:41 to the meta line.

| State | Marker | Status line |
| --- | --- | --- |
| verified | solid `ok` square | `verified` · BOTH SIGNED · SEAL OK, in `ok` |
| waiting | dashed `pending` square | `schedule` · BOTH SIGNED · WAITING, in `pending` |
| fork | solid `alert` diamond (the square turned 45°) | `call_split` · FORK · ALSO FOLLOWS #4, in `alert-text` |
| supervisor decision | solid `ink` square | `gavel` · #5 UPHELD · #5′ VOIDED, in `ink` |
| void | hollow `line-strong` square | VOIDED BY DR TAN 19:52, in `ink-muted` |

**Void.** A branch a supervisor rejected stays in the chain: nothing is deleted. The row is hatched (`line` lines at 45°), its names and time are struck through in `ink-muted`, and an 18dp strip at the right end, full row height, carries VOID in rotated text reading bottom to top (`bg-100` on `ink-muted`).

Minimum row height 64dp; the whole row is tappable and opens the record (both signatures, seal, phone and arrival times).

The consumer provides: giver, receiver, sequence number, label, phone time, arrival time, state and, for void, who voided it and when.

Don't: order rows by phone time (order comes from the chain links), hide a forked or voided row, or delete anything.
