# HazardBand

The system's two 45° patterns: **stripes**, which mean attention, and **hatching**, which means not available.

**Stripes**: bold, `stripe-width` (8dp) wide, repeating every 16dp, in a `stripe-band` (12dp) strip.
- **on-signal**: `on-signal` on `signal`. The foot of the in-custody label, the `LiabilityLine` column, the last hour of the `CountdownBar`.
- **alert**: `alert` on `alert-bg`. The foot of the overdue label, the overdue `CountdownBar`.
- **ink**: `ink` on `bg-000`, double height, as a divider just above a step that moves liability.

**Hatching**: thin `line` lines, `hatch-line` (3dp) every `hatch-pitch` (10dp), over `bg-200`. Hatching strikes out a disabled button (with a dashed `line-strong` edge and the reason on a solid patch) and a voided record in the custody chain.

Compose: draw both in a `Canvas` or `drawBehind`, clipped to the shape: diagonal lines stepped by the pitch.

The consumer provides: the variant and the area it covers. It has no text and no touch target of its own.

Don't: use stripes on more than two things per screen, put text directly on stripes or hatching, animate either, or swap their meanings.
