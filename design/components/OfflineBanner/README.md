# OfflineBanner

A full-width strip under the top app bar that says whether this phone can reach the server and how many records are waiting. Offline is a normal working mode, so the banner is calm: `bg-200` with a 2dp `line-strong` bottom edge, not red.

- **offline**: an outlined `OFFLINE` tag (`record-label`), the count in plain words ("3 handovers saved on this phone"), and a mono second line with the last sync time. Shown on every screen while offline.
- **syncing**: tag in `ok`, progress in words ("2 of 3 checked by server"). Shown only while records are moving.
- **online**: collapses to a `SyncChip` (synced) on `bg-100`; hide it entirely after 3 seconds if nothing is waiting.

Tapping the banner opens the outbox. Height at least 64dp (`target-row`), square corners (`radius-none`).

The consumer provides: connection state, waiting count, last sync time, and (when syncing) progress.

Don't: block the screen, use alert colours for being offline, or say "error" or "failed" for records that are simply waiting.
