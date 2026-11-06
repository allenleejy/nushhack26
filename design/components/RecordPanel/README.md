# RecordPanel

A summary panel set like a printed form, for the supervisor's audit and export screens: the daily location record, the movements in and out of storage, today's counts.

A `bg-100` panel with a 1dp `line-strong` border and a "+" registration mark at each corner in `signal-text`, centred on the corner and overhanging it by 9dp (keep 8dp of space around the panel). A header row in `record-label` (DAILY RECORD · REG 26 on the left, the date on the right) over a hairline, then label–value lines in uppercase mono, divided by dashed `line` hairlines, with the value bold on the right. A line that needs action (FORKS 1) is `alert-text`.

Quiet on purpose: it reports, it doesn't alert. Tapping a line opens the records behind it.

The consumer provides: the title, the date and the lines (label, value, whether it needs action).

Don't: use registration marks on the holder's screens, on interactive cards, or more than one panel deep.
