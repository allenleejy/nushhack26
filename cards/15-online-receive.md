# 15: Online receive: the incoming-offer banner, accept with seal and fingerprint

Wave: 2 · Devin mode: Normal · Depends on: the relay (on `main`: `docs/sync.md` A.10 and D.5), card 04

## Goal
When someone online hands a camera to me, my phone shows a banner wherever I am. I tap it, the offline checks run on the offer, I scan the seal, touch my fingerprint, and my acceptance goes back to the giver through the server. No QR codes. If sending fails, the acceptance QR shows instead.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/incoming/
- app/src/main/java/com/nushhack26/app/ui/receive/
- app/src/main/java/com/nushhack26/app/data/repo/ReceiveRepository.kt
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveScreenshotTest.kt, IncomingScreenshotTest.kt

## Build
Follow `docs/sync.md` A.10 (receiver steps 2, 4 and 5) and `docs/spec.md` §3 R0, R2–R5 with that exact text. Card 04's QR flow stays as it is.
- **R0 `IncomingOfferBanner(container, onOpen)`:**
  - It's already placed in `AppScaffold`, under the top bar, hidden during a handover.
  - Collect `container.offerRelay.incoming` with `collectAsStateWithLifecycle`; collecting it is what turns the polling on.
  - For each offer, read the giver's name and the item code from Room (add the queries to `ReceiveRepository`).
  - Hide offers whose id is already a local handover, and offers past `expires_ms`.
  - Show the newest one: "**Siti Aminah is handing you C-12** · Scan its seal to accept", a countdown, and **Open** → `onOpen(offer.id)`.
  - Vibrate once (short) the first time each offer id appears.
- **`ReceiveScreen(container, onDone, relayedOfferId)`:** the parameter already exists, and `ReceiveRelayedRoute` passes it.
  - When it's set, load the offer with `container.offerRelay.incomingOffer(id)` and feed its `offer` string into the same path as a scanned code (`QrPayloads.parse`, then R2's checks, including line 6). R1 is skipped.
  - Not found → the error state "This offer is no longer open. Ask <giver> to send it again." (or without the name if unknown).
  - R2–R4 add a text button **Decline**: `offerRelay.close(id)`, then "Declined. Siti still holds C-12.", then `onDone`.
- **After `saveAccepted`** (unchanged):
  - if relayed, `offerRelay.accept(id, QrPayloads.acceptance(record))`
  - `Sent` → R5-online: "Sent to Siti · ask her to check the match code", the match code, then "You hold C-12. Return by …" and the sync state
  - otherwise → R5 with the acceptance QR and "Couldn't reach the server. Show this to Siti instead."
- A refused relayed offer behaves as card 04: nothing stored, nothing sent. Also call `close(id)` so the giver isn't left waiting, and say so in the PR.

## Offline behaviour
- No banner while offline: the inbox needs the network. Receive by QR works exactly as card 04.
- Losing the network after accepting: the record is saved and queued as now, and the acceptance QR is the fallback.

## Acceptance criteria (checked on a phone)
- [ ] Both phones online: within 3 seconds of Siti signing, Ravi's phone (on any tab) shows "Siti Aminah is handing you C-12" and vibrates once.
- [ ] **Open** → the six green lines → **Scan the seal** → match → **Accept** → fingerprint → "Sent to Siti", the same match code as Siti's, and C-12 in My custody.
- [ ] A wrong seal sticker blocks it, as in card 04.
- [ ] **Decline** → Siti's phone says Ravi declined. Ravi holds nothing.
- [ ] On Jun Lim's phone, an offer can't be sent at all (G1 greys him out). If one arrives anyway (Debug), line 6 refuses it.
- [ ] Airplane mode on Ravi's phone right after Accept → the acceptance QR shows with the fallback line, and Siti can scan it.
- [ ] Nothing doubles on Supabase: one `handovers` row per handover.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Both phones online. Phone A (Siti): Issue C-12 to Ravi (card 14).
2. Phone B (Ravi): wait for the banner → Open → Scan the seal → Accept → fingerprint.
3. Repeat with Decline, with a wrong seal, and with airplane mode switched on right after Accept.

## Notes
- **Data:** `items`, `people`, `devices`, `handovers`; the relay through `container.offerRelay` (never call Supabase directly).
- **Uses:** `data/repo/RelayRepository.kt`, `data/remote/OfferRelayApi.kt` and the `IncomingOfferBanner` slot in `AppScaffold`. They're shared and on `main`; don't change them, and if you must, list it under "Shared files changed". For unit tests, build a `RelayRepository` over a fake `OfferRelayApi`.
- **Design:** the banner is signal yellow (`Tenet.colors.signal` on `onSignal`), full width, with `HazardDiamond(Filled)` on the left and Space Mono uppercase for the item code. Read `design/brand.md` first. Reuse card 04's components for R2–R5.
