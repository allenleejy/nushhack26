# TopAppBar

The bar at the top of every screen: where you are, a way back, and this phone's sync state.

`app-bar` (64dp) tall on `bg-100` with a 2dp `line-strong` bottom rule, square. A 48dp leading icon button (`menu` on top-level screens, `arrow_back` inside, `close` inside the handover flow), the screen title in `title` (Space Mono 700 uppercase, 20sp) with an optional `record-label` subtitle under it, and a trailing `SyncChip` for the phone's outbox (3 waiting, synced or offline).

**Inside the handover flow** the bottom rule is replaced by a 6dp hazard band (`on-signal` on `signal`), so the whole flow reads as "liability is about to move". The subtitle shows the step (STEP 2 OF 3).

The `OfflineBanner` sits directly under the bar when the phone is offline.

Compose: M3 `TopAppBar` with `containerColor = bg100`, a `Divider` or a striped `Box` below it.

The consumer provides: title, optional subtitle, the leading action and the sync state.

Don't: put more than one action icon on the right besides the chip, centre the title, or use a large collapsing bar.
