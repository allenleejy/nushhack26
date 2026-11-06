# RadioCheck

The six-digit check a supervisor runs over the radio, offline, to confirm that a holder's phone has an item's latest custody record. It proves the phone has the chain, not that the person has the item, and it's a live check, not lasting proof.

Two screens, each a label with a solid `signal` header bar (RADIO CHECK · HOLDER / RADIO CHECK · SUPERVISOR, with the name or item on the right) and a 2dp `signal-edge` outline:

- **Supervisor**: a random four-digit challenge in `code-lg`, `signal-text`, to read out (4821). Below it, six square digit cells (`code-cell` 48 × 64dp, 2dp `line-strong` on `bg-000`, bold mono 32sp) in two groups of three for the reply; the current cell has a `signal-edge` border and a 6dp `signal` bar inside its foot.
- **Holder**: the challenge entered in four cells, then the reply in large `signal-text` mono, grouped in threes (305 917), and one sentence saying what it's made from.

**Result**, full width under the supervisor's cells:
- match: `verified`, CHAIN CONFIRMED in bold `ok` mono on `ok-bg`, then the sentence "Wei Ming's phone holds C-12's latest record (#5)."
- mismatch: `gpp_bad`, REPLY DOESN'T MATCH in `alert-text` on `alert-bg`, the possible causes and the tries left. After three wrong replies the check locks for that item.

Digits are always grouped in threes when read aloud. Use the number keypad.

The consumer provides: role, item, holder, the challenge, the reply digits and the result.

Don't: show the shared secret, accept a reply without a fresh challenge, or present a match as proof of where the item is.
