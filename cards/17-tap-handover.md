# 17: Tap in the handover: one tap and two fingerprints, no network

Wave: 3 · Devin mode: Normal · Depends on: cards 14, 15 and 16 merged

## Goal
Offline, the giver holds their phone to the receiver's once. The receiver's checks run at once, they scan the seal and accept with a fingerprint, and the giver's phone finishes by itself. The QR codes stay on screen as the fallback.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/give/
- app/src/main/java/com/nushhack26/app/ui/receive/
- app/src/test/java/com/nushhack26/app/screenshots/GiveScreenshotTest.kt, ReceiveScreenshotTest.kt
- app/src/test/java/com/nushhack26/app/ui/give/, app/src/test/java/com/nushhack26/app/ui/receive/

## Build
Follow `docs/sync.md` A.11 and `docs/spec.md` §3 G3 (Tap), R1 (Tap), R5 (Tapped) and G5, with that exact text. Use only `container.tapLink` (card 16's API); don't change `nearby/`.
- **Permissions:** the first time G3 or R1 opens and `tapLink.available` is `NeedsPermission`, ask for `tapLink.requiredPermissions()` once. If refused, hide the tap lines.
- **G3 (the QR offer screen, card 03):**
  - while it's shown and `available` is `Ready`, collect `tapLink.serve(offerString)` and show "Or hold your phone to <first name>'s"
  - `Tapped` → "Tapped · waiting for <first name> to scan the seal and accept"
  - `Acceptance(text)` → `QrPayloads.parse(text)`, then the same checks as G4, then `completeHandover`, then G5 with "Signed by both · tapped, no network"
  - `Failed` → keep the QR and show nothing alarming
  - **Scan acceptance** stays as the fallback
- **G3-online (card 14):** leave it as it is. Tap is the offline path.
- **R1 (card 04):**
  - while it's shown and `available` is `Ready`, use `tapLink.rememberTapReader(enabled = true)` and show "Or hold your phone to the giver's"
  - on a tap, feed `payload.offer` into the same path as a scanned code, then `tapLink.join(payload)`, and keep the session in the ViewModel
  - show "Linked to <giver>'s phone" when `linked` is true
- **R5:** after `saveAccepted`, if there's a session, `session.send(QrPayloads.acceptance(record))`.
  - `true` → R5 Tapped: "Sent to <giver>'s phone, no network", the match code, then "You hold …"
  - `false` → the normal acceptance QR with "Couldn't reach <giver>'s phone. Show this instead."
  - close the session when the screen leaves
- A refused offer (any ✗ on R2) closes the session and stores nothing, as now.

## Offline behaviour
- This whole card is the offline path. It needs NFC and Bluetooth on, but no network.
- If either is off, the QR flow works exactly as before.

## Acceptance criteria (checked on two phones)
- [ ] Both phones in airplane mode with Bluetooth and NFC on (or `airplane_mode_radios` set as in A.11): Siti issues C-12 to Ravi → Sign → fingerprint → G3. Ravi opens Receive and Siti holds her phone to his.
  - Ravi's checklist turns green at once.
  - Seal → Accept → fingerprint → "Sent to Siti's phone, no network".
  - Siti's phone moves to G5 by itself with the same match code.
- [ ] The same in the other direction (Ravi returns C-12 to Siti).
- [ ] Bluetooth off on Ravi's phone after the tap: R5 shows the acceptance QR fallback, and Siti can scan it.
- [ ] A tapped offer made out to someone else gets the red "This is a copy" screen, and nothing is sent back.
- [ ] Back online, both phones sync and Supabase has exactly one `handovers` row per handover.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Both phones offline (network off, Bluetooth and NFC on). Phone A: Issue C-12 to Ravi → Sign. Phone B: Receive → tap phones.
2. Phone B: Scan the seal → Accept → fingerprint. Watch A finish by itself and compare match codes.
3. Repeat in the other direction, then reconnect and check Supabase.

## Notes
- **Data:** as cards 03 and 04; no new tables.
- **Uses:** `container.tapLink` and `TapPayload` (card 16), `QrPayloads`, `OfferChecker`, `GiveRepository`, `ReceiveRepository`.
- **Design:** for the tap line use a `ListRow`-style row with `Icons.Sharp.Nfc` and Space Grotesk body text, under the QR panel. "Linked" uses `SyncChip`'s synced style.
