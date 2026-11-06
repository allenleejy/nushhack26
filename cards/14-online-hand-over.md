# 14: Online hand over: send the offer through the server, no QR codes

Wave: 2 · Devin mode: Normal · Depends on: the relay (on `main`: `docs/sync.md` A.10 and D.5), card 03

## Goal
When the phone is online, the giver picks the receiver, touches the fingerprint and waits. The offer reaches the receiver's phone through the server, and the giver's phone finishes by itself when the receiver accepts. No QR codes. If the network is missing or drops at any point, the existing QR screens take over, so a handover never gets stuck.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/give/
- app/src/main/java/com/nushhack26/app/data/repo/GiveRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/GiveScreenshotTest.kt
- app/src/test/java/com/nushhack26/app/ui/give/

## Build
Follow `docs/sync.md` A.10 (giver steps 1, 3, 4 and 5) and `docs/spec.md` §3 G2, G3-online and G5, with that exact text. Card 03's offline flow stays as it is; this card adds the online path beside it.
- **G2:**
  - Decide online or QR when the screen reaches G2: online = `container.isOnline.value && container.serverConfigured`.
  - Show the matching line under Sign ("Online · Ravi accepts on his phone, no codes to scan" / "Offline · you'll swap QR codes").
  - Online offers use `expires_ms = offered_ms + RelayRepository.RELAYED_OFFER_VALID_MS` (3 minutes); QR offers keep 60 s. The expiry is inside the signed string, so it's fixed before **Sign**.
- **After signing:**
  - save the pending offer as now
  - then, if online, call `container.offerRelay.post(record.id, itemId, toPersonId, QrPayloads.offer(record, proof), expiresMs)`
  - `Sent` → G3-online. `Unavailable` or `Refused` → the normal G3 QR (same offer, so its countdown shows the longer expiry).
- **G3-online:**
  - "Sent to <first name>'s phone", the countdown to `expires_ms`, and a waiting mark
  - **Show QR instead** (→ G3 with this offer) and **Cancel offer** (`offerRelay.close(id)`, then delete the pending offer)
  - While visible, every 2 s: `offerRelay.status(id)`.
    - `accepted` → `QrPayloads.parse(acceptance)`, then the same acceptance checks as G4 (`OfferChecker.checkAcceptance`), then `GiveRepository.completeHandover(record)`, then G5 with the online line
    - `declined` → the spec's declined text with **Choose someone else** (delete the pending offer, back to G1)
    - `cancelled` or `unknown` → back to G1 with "This offer is no longer open."
    - a null status 3 times in a row, or `isOnline` turning false → switch to G3 QR with "Lost the network. Show the QR instead."
  - Also watch Room for a handover with the offer's id: add `GiveRepository.handover(id): Flow<HandoverEntity?>`. If the receiver's record arrives by sync before the relay answers, delete the pending offer and go to G5 with it.
  - A failed acceptance check (it shouldn't happen) → G4's error text, and stay on G3 with Show QR instead.
- **Resuming** (`GiveRoute(itemId)` with a pending offer): ask `offerRelay.status(id)`. `open` → G3-online; `accepted` → finish as above; anything else, or no answer → G3 QR as now.
- **The tampered phone** (A.5) is unchanged: `completeHandover` already handles it.

## Offline behaviour
- Offline at G2 → exactly card 03's QR flow.
- Losing the network while waiting → G3 QR with the same offer. The receiver can still scan it, and the giver scans their acceptance.

## Acceptance criteria (checked on a phone)
- [ ] Both phones online: Siti issues C-12 to Ravi with **+90 s** → Sign → fingerprint → "Sent to Ravi's phone". When Ravi accepts on his phone (card 15), Siti's phone moves to G5 within 3 seconds with the same match code as Ravi's. No QR was scanned.
- [ ] Siti taps **Show QR instead** while waiting: the QR appears, Ravi can scan it with Receive, and the handover finishes as in card 03.
- [ ] Ravi taps **Decline**: Siti sees "Ravi declined. Nothing was handed over." and C-12 is still hers.
- [ ] Airplane mode on Siti's phone at G2: the line says offline, and after signing the QR shows as before.
- [ ] Airplane mode on Siti's phone while waiting: within a few seconds G3 QR opens with "Lost the network. Show the QR instead."
- [ ] **Cancel offer** while waiting: Ravi's banner disappears within 3 seconds.
- [ ] Supabase has exactly one `handovers` row for each handover, whichever path finished it.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Both phones online. Phone A (Siti): Store → Issue on C-12 → Ravi → +90 s → Sign → fingerprint.
2. Phone B (Ravi): tap the yellow banner → Scan the seal → Accept → fingerprint (card 15). Watch A finish by itself and compare match codes.
3. Repeat with Show QR instead, with Decline, and with airplane mode switched on halfway.

## Notes
- **Data:** `items`, `people`, `devices`, `handovers`, `pending_offers`; the relay through `container.offerRelay` (never call Supabase directly).
- **Uses:** `data/repo/RelayRepository.kt` and `data/remote/OfferRelayApi.kt`. They're shared, on `main` and tested on the server; don't change them, and if you must, list it under "Shared files changed". For unit tests, build a `RelayRepository` over a fake `OfferRelayApi`.
- **Design:** reuse `HandoverStepper(Giver)`, the who-and-what card and `PrimaryButton`/`TextLinkButton` from card 03. For the waiting mark use `HazardDiamond` with a slow alpha pulse. Read `design/brand.md` first.
