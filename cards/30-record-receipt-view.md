# 30: The record receipt: one handover, printed

Wave: 7 (motion) · Devin mode: Normal · Depends on: the prep on `main` (`RecordRoute(handoverId)` with a placeholder `RecordScreen`, the `onOpenRecord` callbacks on TimelineScreen and OutboxScreen already wired in AppNavHost, `CustodyReceipt`'s final signature, `ReceiptNumbers`); card 29 fills in `CustodyReceipt` in the same wave

## Goal
Tapping a handover on an item's timeline, or in the outbox, opens it as a printed custody receipt. The receipt feeds out of a printer slot with both signers' keys, the signature check, the countersigned stamp, a barcode and the record's hash: one record a judge can read at a glance.

## Source of truth
The design canvas **https://claude.ai/artifact/2B5tMaPVPTC3Ajzdb2BHhH**, board "Record receipt" (`project/Receipt.dc.html`). Card 29 builds the receipt itself (`ReceiptStyle.Record`): the slot, the mouth, the print and the stamp. This card supplies its content and the screen around it.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/record/ (RecordScreen.kt, plus a new RecordViewModel.kt and RecordModel.kt)
- app/src/main/java/com/nushhack26/app/ui/timeline/TimelineScreen.kt, TimelineRow.kt (only to make handover rows open the receipt)
- app/src/main/java/com/nushhack26/app/ui/outbox/OutboxScreen.kt, OutboxModel.kt (only to make handover rows open the receipt)
- app/src/test/java/com/nushhack26/app/screenshots/RecordScreenshotTest.kt (new)
- app/src/test/java/com/nushhack26/app/ui/record/RecordModelTest.kt (new)

Rules:
- Don't edit `CustodyReceipt.kt` (card 29) or `ReceiptNumbers.kt`. Keep the `RecordScreen(container, handoverId)`, `TimelineScreen(…, onOpenRecord)` and `OutboxScreen(container, onOpenRecord)` signatures as they are on `main`.

## Build

### 1. `RecordModel.kt`: pure, unit-tested
`fun recordReceipt(h: HandoverEntity, chain: List<HandoverEntity>, item: ItemEntity?, roster: Roster, nowMs: Long): Pair<ReceiptContent, ReceiptStamp>`

| Field | Value |
|---|---|
| `recordNo` | `ReceiptNumbers.recordNo(h, chain)` |
| `followsNo` | `recordNo − 1`, or null for #1 or unknown |
| `date`, `signedTime` | `h.acceptedMs` (or `h.offeredMs` when that's null) as `dd.MM.yyyy` and `HH:mm` |
| item fields | `item.displayName`, `item.code`, and `source` the same way card 24's code lines show the source serial and isotope |
| `sealNo`, `sealMatches` | `h.sealNo`, and `true` |
| `from` | `ReceiptParty(giver full name, "KEY " + Signatures.keyFp(giver device public key))`, or null for a register record |
| `to` | the same for the receiver. A key line is left out when that device's key isn't on this phone. |
| `returnBy` | `h.dueMs` as `HH:mm`, or null |
| `via` | null: the record doesn't store how it travelled, so don't guess |
| `heldBy` | the receiver's full name |
| `signatures`, `signaturesOk` | from `checkSignatures(h, parent, roster)` in `TimelineViewModel.kt` (the same check the timeline shows): Valid → `2 OF 2 VERIFIED`, ok; Invalid → `SIGNATURE INVALID`, not ok; UnknownKey → `SIGNER NOT ON THIS PHONE`, not ok; Unsigned (register) → `REGISTERED BY THE SERVER`, ok |
| `hash` | `h.recordHash` (blank → null) |
| `offline` | true when the record isn't on the server yet (`serverSeq == null`), or arrived more than 60 s after it was signed (`receivedAt − acceptedMs > 60 000`) |
| `verifyCode` | `ReceiptNumbers.verifyCode(Canonical.matchCode(h.recordHash))`; blank when the hash is invalid |
| `footer` | "Synced" when `serverSeq != null`, else "Saved on this phone · not synced yet" |

The hash line itself is printed by card 29 from `hash`; `ReceiptNumbers.shortHash` gives "SHA 4F2A 91C0 77E3 0B1D".

**Stamp:**
- `COUNTERSIGNED` / `HH:mm` / the receiver's first name, for example `WEI MING` (the first name as the rest of the app shortens it)
- for a register record: `REGISTERED` / time / `IN STORE`

### 2. `RecordViewModel` and `RecordScreen`
- **Data:** read through `container.custodyRepository` only:
  - the handover from `observeHandovers()`
  - its chain from `observeChain(itemId)`
  - the item from `observeItems()`
  - `observeRoster()`
- **States:**
  - Loading: `LoadingState`
  - Not found: `EmptyState("Record not on this phone", "Sync, then try again.")`
  - Loaded: `CustodyReceipt(content, stamp, ReceiptStyle.Record)` in a vertically scrolling column with 16 dp side gutters
- **Title:** "Receipt" (already in `titleFor`).
- **Updates:** when the record syncs while it's open, the footer and OFFLINE update. The print animation doesn't replay; card 29 keys it on the verify code.

### 3. Opening it
- **Timeline (T):** every handover row, including the register row, is clickable (whole row, 48 dp minimum, ripple-free square press highlight in `bg200`) and calls `onOpenRecord(handoverId)`. Add a trailing receipt_long icon (`Icons.AutoMirrored.Sharp.ReceiptLong`, or `Icons.Sharp.ReceiptLong` if that's what this icons version has) (24 dp, `inkMuted`) to those rows. Non-handover rows don't change.
- **Outbox (E4):** rows whose table is `handovers` open the receipt the same way, with the same icon. Other rows don't change.

## Offline behaviour
- Everything is read from Room, so the receipt opens offline. Waiting records show OFFLINE and "Saved on this phone · not synced yet".

## Acceptance criteria (checked on a phone)
- [ ] Kestrel's timeline: tap the latest handover. The receipt feeds out of the slot, then the COUNTERSIGNED stamp lands with a thud.
- [ ] The receipt shows RECORD #n, FOLLOWS #n−1, both names with their KEY lines, SIGNATURES 2 OF 2 VERIFIED in cyan, the barcode, the SHA line and the verify code. The verify code matches the Done screens of that handover.
- [ ] The register row opens a receipt with REGISTERED, no FROM, and RECORD #1.
- [ ] Offline, hand something over and open it from OUTBOX: it shows OFFLINE and "Saved on this phone · not synced yet". After syncing, the footer reads "Synced".
- [ ] Back returns to the timeline or the outbox.
- [ ] It fits at 360 dp wide and at 1.3× font size; long names wrap.

## Test steps
1. Open Kestrel's timeline and tap each row.
2. Go offline, hand over, open the record from OUTBOX, then sync.

## Notes
- **Tests:**
  - `RecordModelTest`: a signed handover (numbers, both keys, VERIFIED, footer), a register record, a record whose receiver key is unknown, and the offline rule (unsynced, and late arrival).
  - `RecordScreenshotTest`: loaded, in both themes, at 360 dp with 1.3× font, and not found. Wrap it in `LocalInspectionMode provides true`.
- **Data:** `handovers`, `items`, `people`, `devices` (public keys), all read-only.
