# 29: Done as a torn custody receipt, and the cube online

Wave: 7 (motion) · Devin mode: Normal · Depends on: the prep on `main` (the `CustodyReceipt` stub with its final signature, `ReceiptNumbers`, `TapRole`); card 28 fills in TapPulse in the same wave

## Goal
Every handover ends with a custody receipt that prints out from under the hazard band like a till roll torn off at both ends. The stamp then lands on it with a thud and a ripple. Online handovers play the same cube-and-portal moment as a tap.

## Source of truth
The design canvas **https://claude.ai/artifact/2B5tMaPVPTC3Ajzdb2BHhH**, boards "Done · torn receipt" (`project/Done.dc.html`) and "Record receipt" (`project/Receipt.dc.html`), with their stylesheet. Use the default "theme paper", not thermal. This card copies every number from the CSS (`.rcpt`, `.rcpt.torn2`, `.rr`, `.rr2`, `.ld`, `.ld2`, `.tot`, `.stampzone`, `.stamp`, `.wv`/`.w0–.w3`, and the keyframes `print`, `drop`, `squash` and `wave`). Where this card and the board disagree, follow this card.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/CustodyReceipt.kt
- app/src/main/java/com/nushhack26/app/ui/give/GiveScreen.kt, GiveComponents.kt, GiveViewModel.kt
- app/src/main/java/com/nushhack26/app/ui/receive/ReceiveScreen.kt, ReceiveParts.kt, ReceiveViewModel.kt
- app/src/test/java/com/nushhack26/app/screenshots/CustodyReceiptScreenshotTest.kt (new)
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveReceiptScreenshotTest.kt (new)
- app/src/test/java/com/nushhack26/app/screenshots/GiveTapScreenshotTest.kt, GiveScreenshotTest.kt

Rules:
- Keep `ReceiptContent`, `ReceiptParty`, `ReceiptStamp`, `ReceiptStyle` and `CustodyReceipt`'s signature exactly as they are on `main`. Card 30 calls them in the same wave.
- Don't edit `ReceiveScreenshotTest.kt` or `ReceiveTapScreenshotTest.kt`, because an open PR edits them. Put new Receive cases in `ReceiveReceiptScreenshotTest.kt`.
- Don't edit `TapPulse.kt` (card 28) or `ReceiptNumbers.kt`. Keep ReceiveScreen's `role = TapRole.Receiver` and the `asTab`, `active`, `session` and `onFlowChange` contract with AppNavHost.

## Build

### 1. `CustodyReceipt`: the till roll
1 design px = 1 dp. "Paper" is `bg100`, "ink" `ink`, "muted" `inkMuted`, "rule" `lineStrong` and "ok" `ok`.

**Type and spacing**
- **Text:** Space Mono 14/20, uppercase, letter spacing 0.02 em, ink. Bold where marked.
- **Padding:** Done 20 top, 20 sides, 18 bottom. Record 18, 18, 22.

**Lines**
- **Title:** `TENET` bold 22/26, letter spacing 0.12 em, centred, with `CUSTODY RECEIPT` bold centred 2 dp under it. Record only, when `offline`: `OFFLINE` in muted, centred.
- **Rule `rr`:** a 2 dp dashed line in the rule colour, with 10 dp above and below.
- **Rule `rr2`:** a double rule 6 dp tall in ink (two 2 dp lines with a 2 dp gap), with 10 dp above and below.
- **Leader line `ld`:** the label, a dotted leader that fills the gap (2 dp dots in muted, sitting 4 dp above the baseline, at least 12 dp long), then the value in bold. If the value doesn't fit, it wraps under, right-aligned.
- **Pair line `ld2`:** the label left and the value right.
- **Indent `ind`:** 28 dp.
- **Total line `tot`:** `HELD BY` at 14 sp, and the name bold at 20/26, on one baseline.
- **Stamp zone:** an empty cell 124 dp tall (Done) or 132 dp (Record), with the stamp centred in it.
- **The stamp:** 3 dp `ok` border, paper fill, padding 8/16, rotated −9°, bold mono uppercase in `ok`:
  - `word` at 20/24
  - `time` at 16/22
  - `line` at 13/16, letter spacing 0.06 em

**Torn edges:** an 8 dp zigzag in the paper colour, teeth every 12 dp, drawn just outside the box. Done tears top and bottom. Record tears only the bottom.

**Order, Done** (the sections in brackets matter for the wave):
1. [w1] the title
2. `rr`
3. [w1] `RECORD #5` | `07.11.2026` and `FOLLOWS #4` | `SIGNED 13:02`, both as `ld2`
4. `rr`
5. [w2] `1 × KESTREL` (bold), then indented `CAMERA C-12`, `SOURCE IR-0457 · IR-192`, and the `ld` line `SEAL S-40117 … MATCHES` (value in ok)
6. `rr`
7. [w2] `FROM … RAVI KUMAR`, `TO … TAN WEI MING`, `RETURN BY … 19:00`, all `ld`
8. `rr2`
9. [w1] the total line `HELD BY | TAN WEI MING`
10. `rr2`
11. [w0] the stamp zone
12. [w3] `VERIFY CODE 482 107`, bold and centred

**Order, Record:**
1. the title (with OFFLINE)
2. `rr`
3. the two `ld2` lines
4. `rr`
5. the item block
6. `rr`
7. `FROM`, then right-aligned in muted `KEY 3F:A9:12:7C`; `TO`, then its key line; `RETURN BY`; and `VIA … NFC TAP` when `via` is set
8. `rr2`, the total line, `rr2`
9. `SIGNATURES … 2 OF 2 VERIFIED`, in ok, or `alertText` when `signaturesOk` is false
10. the stamp zone
11. the barcode
12. the hash line in muted, centred
13. `VERIFY CODE 482 107`, bold and centred
14. `rr`
15. `footer` in Space Grotesk 15/22, normal case, centred

**The barcode** (Record only):
- 44 dp tall and centred: 60 cells alternating bar and space, starting with a bar.
- Each cell is 2 dp, or 4 dp with probability 0.35. Draw it from a deterministic PRNG seeded from the first 8 hex digits of `hash`, so it never changes for a record. Bars are in ink.
- With no `hash`, leave out the barcode and the hash line.

Lines whose value is null aren't printed. `FOLLOWS` is left out when `followsNo` is null, and `#` + number is left out when `recordNo` is null.

**Record style frame** (above the receipt):
- A **printer slot**: full width, 40 dp, `bg200`, 2 dp `lineStrong` border. `RECORD #5` on the left in `recordLabel`, and an 8 dp square `ok` light on the right, with 12 dp padding.
- Under it, a **mouth**: a 6 dp bar in #121211, inset 14 dp each side.
- The receipt hangs from the mouth, inset 20 dp each side.

**Motion.** It plays once per receipt: `rememberSaveable`, keyed on `verifyCode + style`. Times are from first composition.

| | Done | Record |
|---|---|---|
| Print: clip the receipt from the top down, so it appears line by line, in **steps** (no smooth wipe) | 22 steps over 1200 ms, after a 150 ms delay | 26 steps over 1700 ms, no delay |
| The stamp appears | at 1450 ms | at 1900 ms |
| Drop: scale 2.2 → 1.0, already rotated −9°, full opacity, `CubicBezierEasing(0.4f, 0f, 1f, 1f)` | 260 ms | 260 ms |
| Landing | `haptics.thud()`, then the squash: 320 ms, linear keyframes 1.0 → 0.94 (15 %) → 1.03 (55 %) → 1.0 | the same |
| Wave | at landing (1710 ms) | none |

**The wave:** each marked section moves on `translationY` through linear keyframes over 420 ms: 0 → 6 dp (18 %) → −2 dp (45 %) → 1 dp (70 %) → 0. Section w0 starts at landing, w1 25 ms later, w2 32 ms later, and w3 48 ms later. Use `graphicsLayer` only, so nothing re-lays out.

- **Reduce motion:** everything is in its final state at once, and one thud fires.
- **Inspection mode** (screenshots): the final state, with no haptics.
- **Accessibility:** merge into one description, for example "Custody receipt, record 5. Kestrel, camera C-12, handed over by Ravi Kumar to Tan Wei Ming at 13:02. Held by Tan Wei Ming. Verify code 482 107."

### 2. The Done screens (G5 and R Done)
- **Replaces:** `ParcelLabel` and the verify line under it are replaced by `CustodyReceipt(style = Done)`. Leave `ParcelLabel.kt` in place, unused.
- **Stepper:** Done drops it, so the receipt prints straight out from under the top bar's hazard band.
- **Under the receipt:** the sync chip with "Not synced yet" (or "Synced"), then DONE, as now. The receipt scrolls if it's taller than the screen, and DONE stays reachable.
- **Data:** build `ReceiptContent` in the ViewModels from what they already hold, plus `CustodyRepository.observeChain(itemId)` for the numbers:

  | Field | Value |
  |---|---|
  | `recordNo` | `ReceiptNumbers.recordNo(record, chain)` |
  | `followsNo` | `recordNo − 1`, or null when `recordNo` is 1 or null |
  | `date` | `acceptedMs` as `dd.MM.yyyy` |
  | `signedTime` | `acceptedMs` as `HH:mm` |
  | `itemName`, `itemCode` | `item.displayName`, `item.code` |
  | `source` | "IR-0457 · IR-192": the source serial and isotope, as card 24's code lines use them |
  | `sealNo`, `sealMatches` | the seal, and `true` (it was checked) |
  | `from`, `to` | the giver and receiver names, no keys |
  | `returnBy` | the due time `HH:mm`, or null |
  | `heldBy` | the receiver's full name on the giver; `YOU` on the receiver |
  | `verifyCode` | `ReceiptNumbers.verifyCode(matchCode)` |
- **Stamps:**
  - giver: `HANDED OVER` / `13:02` / `TO WEI MING` (the receiver's first name)
  - receiver: `RECEIVED` / `13:02` / `FROM RAVI` (the giver's first name)

  Returns to the store read the same way, with Siti.

### 3. The cube online
The tap path already gets the cube through card 28's TapPulse. Add it to the online path:
- **Giver, G3 online** (`GiveStep.Sent`): at the top of SentStep, show `TapPulse(role = Giver)`.
  - On entering Sent, run Contact, then switch to Linked when `onFlooded` fires.
  - Title "Sent", subtitle "Waiting for Wei Ming to open it, scan the seal and sign."
  - Keep the rest of SentStep (declined, relay errors, cancel) under it.
- **Receiver, relayed offer** (opened from the banner, `relayed`): before R2, show `TapPulse(role = Receiver)` in Contact, with title `KESTREL · C-12` and subtitle "Sent by Siti, through the server." When `onFlooded` fires, go on to R2 as now.
- **Receiver, tap path:** once the offer's item is known during Contact, change the title to `KESTREL · C-12` and the subtitle to "Scan the seal on Kestrel (C-12)." Before that, keep "HOLD TO THE GIVER'S PHONE".

## Offline behaviour
- Pure UI. The receipt reads only Room data the screens already hold, so it prints offline.

## Acceptance criteria (checked on a phone)
- [ ] After a tap handover, both phones show the receipt printing out line by line from under the hazard band, torn at both ends.
- [ ] Then the stamp falls with a thud and the receipt ripples outward from it.
- [ ] Ravi's receipt: RECORD #n, FOLLOWS #n−1, today's date as DD.MM.YYYY, 1 × KESTREL, SEAL S-40117 … MATCHES, FROM/TO/RETURN BY on dot leaders, HELD BY TAN WEI MING, stamp HANDED OVER / time / TO WEI MING, VERIFY CODE.
- [ ] Wei Ming's receipt: HELD BY YOU, and stamp RECEIVED / time / FROM RAVI. Both phones show the same record number and verify code.
- [ ] Leaving Done and coming back doesn't replay the print.
- [ ] Remove animations on: the receipt is there at once, with one thud.
- [ ] Online: Ravi's cube goes into the amber circle once sent, then the traced outline. Wei Ming opens the banner, the cube comes out of the cyan circle, then the checklist.
- [ ] It fits at 360 dp wide and at 1.3× font size: long names wrap, and DONE stays reachable.

## Test steps
1. Hand Kestrel over by tap, then by the online path, and watch both phones through to Done.
2. Return Kestrel to Siti and check the receipts.
3. Repeat one handover with Remove animations on.

## Notes
- **Screenshots:**
  - `CustodyReceiptScreenshotTest`: Done for the giver and for the receiver, and Record with every line, in both themes, plus Done at 360 dp with 1.3× font and a long name.
  - `ReceiveReceiptScreenshotTest`: R Done.
  - `GiveTapScreenshotTest`: G5.

  Wrap the receipt in `LocalInspectionMode provides true` so it renders the final state.
- **Design:** this replaces card 22's parcel label. Its timings follow the design board, not card 22.
