# HandoverStep

The parts of the handover flow: a three-step stepper, the seal check result, and the QR panel that carries an offer or an acceptance between two phones.

**Stepper.** Three equal square boxes across the top of every handover screen. The giver sees *Scan seal → Show offer → Scan acceptance*; the receiver sees *Scan offer → Scan seal → Sign and show*. Step numbers sit in square markers. Done: an `ok` check on `ok-bg`. Current: `signal-bg` box with a `signal-text` edge and a solid `signal` marker. Upcoming: `line` edge, `ink-muted` label. Labels are Space Mono 700, uppercase, 13sp.

**Seal check.** A full-width row after the seal scan. Match: `verified` icon, SEAL MATCHES C-12 in bold uppercase mono, `ok` on `ok-bg`, the number in `record`. Mismatch: `gpp_bad`, SEAL DOESN'T MATCH C-12. DON'T SIGN. in `alert-text` on `alert-bg`, with the scanned and expected numbers stacked so a person can compare them digit by digit. A mismatch hatches out the Sign button.

**QR panel.** Always `qr-ground` (white) with `qr-ink` modules, in both themes. At least 280dp square (`qr-min`) with a 16dp quiet zone, framed by a 2dp `signal-edge`. A `signal` side tab on its left carries a rotated caption naming what it holds (OFFER · C-12 · SEQ 5), reading bottom to top. One instruction above it in `body-lg`, sentence case ("Ask Wei Ming to scan this code."). Raise screen brightness while a QR is showing. The QR in the preview is illustrative.

The consumer provides: the role (giver or receiver), the current step, the seal result and the QR payload.

Don't: put more than one QR on screen, shrink the QR to fit text, or let the receiver sign before the seal check passes.
