# ForkAlert

The warning label a supervisor sees when the server finds two handovers from the same giver that both follow the same record: the double handover the system can't stop offline but can prove.

Built like a hazard warning label, with square corners and a 2dp `alert` border on `bg-100`:
1. **Header**: a 56dp `alert` cell holding the filled `warning` symbol in `on-alert`, beside the claim in `title`, uppercase, `alert-text` (HANDED OVER TWICE), with the item and detection time in `record-label` under it.
2. **Evidence sentence** in `body-lg`, sentence case, naming the person in `alert-text` bold: "**Ravi** signed two handovers that both follow record #4. His own signatures show it."
3. **Two boxed cells**, divided by 2dp `line-strong` rules, one per branch: branch and sequence, receiver, signed time and seal result. Equal weight: the server never guesses which is real.
4. **One primary action**: DECIDE WHO HOLDS C-12.

The supervisor's decision becomes a signed record in the chain: the upheld branch stays verified, the other becomes a void `TimelineRow`.

The same label serves any conflict between two records: change the header text and keep the two cells. Use the same header for an overdue escalation or a seal mismatch with one cell.

The consumer provides: item, giver, the common parent record, both branches (receiver, time, seal result), detection time and the resolve action.

Don't: auto-pick a winner, shorten the evidence sentence to an error code, or show the fork without naming who signed both.
