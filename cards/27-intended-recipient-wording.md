# 27: Checklist wording: "Intended recipient"

Wave: 6 (bug fixes) · Devin mode: Lite · Depends on: none

## Goal
The receiver's checklist (R2) says "Intended recipient" instead of the awkward "Made out to you".

## Files owned
- app/src/main/java/com/nushhack26/app/crypto/OfferChecker.kt (the one line of text only)
- app/src/test/java/com/nushhack26/app/crypto/TokenCoreTest.kt
- app/src/test/java/com/nushhack26/app/ui/receive/RelayedReceiveTest.kt
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveScreenshotTest.kt
- app/src/test/java/com/nushhack26/app/screenshots/ReceiveTapScreenshotTest.kt

## Build
In `OfferChecker`, the check line for `h.toPersonId == me.personId`:
- passes: `Intended recipient`
- fails: `Not intended recipient · for Tan Wei Ming` (the receiver's name, as now, after a middle dot)

Change only the text. The check itself, its order in the list and the refused header don't change.

Update every test and screenshot fixture that has the old text (`grep -rn "Made out to" app/src`): TokenCoreTest, RelayedReceiveTest, ReceiveScreenshotTest and ReceiveTapScreenshotTest.

## Offline behaviour
- Text only. The checklist runs offline, as before.

## Acceptance criteria (checked on a phone)
- [ ] Receive a handover meant for you: the third line reads "✓ Intended recipient".
- [ ] Scan an offer meant for someone else (the judge's photo in the demo): the line reads "✗ Not intended recipient · for Tan Wei Ming" in red.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Run demo script §2 step 2 and read the checklist.
2. Scan an offer made for another person.

## Notes
- The build account has already updated docs/spec.md and docs/sync.md to the new wording.
- **Data:** `handovers.to_person_id`, unchanged.
