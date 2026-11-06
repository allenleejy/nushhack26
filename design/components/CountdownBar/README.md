# CountdownBar

Time left before an item is due back at the store, shown as a bar under the custody label. It changes pattern as the deadline nears, so the state reads even at a glance or in glare.

- **plenty of time**: solid `signal` fill on `bg-100`, with a 2dp `line-strong` border. The fill is the share of the sign-out period that remains.
- **last hour** (60 minutes or less): the fill becomes `on-signal` and `signal` hazard stripes.
- **overdue**: the bar fills completely with `alert` stripes on `alert-bg`, the border turns `alert`, and the label and value switch to `alert-text` (RETURN OVERDUE · 12 MIN LATE).

Above the bar: a `record-label` (RETURN BY 19:00) on the left and the remaining time in bold `record` on the right (5 H 58 MIN, 38 MIN). The bar is 16dp tall, square.

It runs from the phone's clock with no network. The overdue state also fires a notification.

The consumer provides: sign-out time, due time and the current time.

Don't: animate the stripes, show seconds, or use the bar for anything but a return deadline.
