# SyncChip

A compact label showing where one record, or this phone's outbox, stands against the server. Every state has an icon and a word, never colour alone.

| State | Look | Icon (Material Symbols Sharp) | Text |
| --- | --- | --- | --- |
| synced | `ok` on `ok-bg`, top-right corner cut (`cut-sm`) | `cloud_done` | SYNCED |
| waiting | `pending` text, dashed 2dp `pending` border, no fill, square | `schedule` | 3 WAITING (outbox count) or WAITING |
| conflict | `on-alert` on `alert`, top-right corner cut (`cut-sm`) | `call_split` | FORK (custody) or CONFLICT (other records) |
| offline | `ink` on `bg-200`, `line-strong` border, square | `cloud_off` | OFFLINE |

Text is `record-label` in bold (mono, uppercase, 13sp), 32dp tall; when tappable (it opens the outbox) wrap it in a 48dp touch target. Compose: `CutCornerShape(topEnd = 8.dp)` for synced and conflict, `RectangleShape` for the rest.

Use it in the top app bar (phone-level: outbox count), at the end of a timeline row and inside the custody label's Sync cell. Waiting is normal and neutral: don't style it as a warning.

The consumer provides: the state and, for waiting, the count.
