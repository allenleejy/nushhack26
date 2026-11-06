# LoadingSkeleton

Placeholders while a server-only view loads (the supervisor's chain from all phones, today's records), and a segmented bar while records send. Local data loads instantly, so most screens never show either.

**Skeleton**: square `bg-200` blocks in the shape of what's coming (a custody label's tab, ID and cells; a list row's leading square and two lines) inside the component's own outline in `line`. No shimmer, no pulse, no hatching (hatching means unavailable, not loading).

**Segmented progress**: when the count is known, one square segment per record, 16dp tall with 4dp gaps and 2dp borders: done segments solid `ok`, the current one `ok` stripes on `ok-bg`, the rest empty. A `record-label` on the left (SENDING TO SERVER) and the count on the right (2 OF 3). The `OfflineBanner` uses the same count.

When the count is unknown, use the M3 `LinearProgressIndicator` with `StrokeCap.Butt`, `ok` on `bg-200`.

The consumer provides: the shape to imitate, or the total and done counts.

Don't: show a skeleton for local data, use a spinner, or animate the stripes.
