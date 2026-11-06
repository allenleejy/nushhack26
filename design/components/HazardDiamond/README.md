# HazardDiamond

A square turned 45° carrying the figure 7, the UN dangerous-goods class for radioactive material. It marks an item as a sealed source wherever the item appears.

- **outlined**: 2dp `signal-text` square, `signal-text` figure. Default, on custody labels, timeline headers and lists.
- **filled**: `signal` square with `signal-edge`, `on-signal` figure. On the receiver's acceptance screen next to the liability line.
- **alert**: `alert` square, `on-alert` figure. On the overdue custody label and in a seal-mismatch alert.
- Sizes: `diamond-md` (48dp box) in cards and rows; `diamond-lg` (96dp box) on the acceptance screen.

Compose: a `Box` of the bounding size holding a smaller square with `Modifier.rotate(45f)` and a border, and an unrotated `Text` on top.

The consumer provides: the variant and size. The figure is always 7 in this app; for another item class, change the figure, never the shape.

Don't: use the diamond as decoration, put words inside it, or rotate it to any angle but 45°.
