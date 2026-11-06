# Receipt

The giver's proof that a handover happened, and the one place the system breaks its own 0°/45° rule.

A `bg-100` panel with a 2dp `line-strong` border: a `record-label` heading (RECEIPT · RECORD #5), then key–value rows (item, from, to, seal) with labels left and bold `record` values right, divided by dashed `line` hairlines.

**The stamp.** When the receiver's acceptance is scanned and their signature checks out, a stamp appears over the empty middle of the receipt: a 3dp `ok` box (`stroke-stamp`) on `bg-100`, rotated `angle-stamp` (−9°), reading COUNTERSIGNED / 13:02 / WEI MING in bold uppercase mono. It's the only rotated element on any screen, so it reads as a human mark and as the "done" moment. Place it so it never covers a value. Compose: `Modifier.rotate(-9f)`.

Until the acceptance is scanned, the stamp's place shows "Handover not confirmed. Scan Wei Ming's acceptance." in `body`, with SCAN ACCEPTANCE as the primary action.

The consumer provides: record number, item, giver, receiver, seal, the acceptance time and whether it's countersigned.

Don't: show the stamp before the signature verifies, tilt anything else, or animate the stamp beyond a single 150ms appearance.
