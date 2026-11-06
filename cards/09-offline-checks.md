# 09: Offline checks: catch a double handover on the phone, and refuse withdrawn keys

Wave: overnight · Devin mode: Normal · Depends on: cards 01, 05; the revocation plumbing on `main` (`docs/sync.md` A.12)

## Goal
Two checks that work with no network:
- **A double handover:** today a fork is only caught when the records reach the server. If a phone already holds a record showing the giver handed this camera to someone else, R2 refuses the second handover on the spot. The timeline shows a phone-side warning when it holds both branches.
- **A withdrawn phone:** once a phone has synced the safety officer's revocation, its offline checks refuse anything the withdrawn phone signed after that moment.

## Files owned
- app/src/main/java/com/nushhack26/app/crypto/OfferChecker.kt
- app/src/main/java/com/nushhack26/app/crypto/Custody.kt
- app/src/main/java/com/nushhack26/app/crypto/Roster.kt
- app/src/main/java/com/nushhack26/app/ui/timeline/
- app/src/test/java/com/nushhack26/app/crypto/ (add new test files; keep the existing ones passing)
- app/src/test/java/com/nushhack26/app/screenshots/TimelineScreenshotTest.kt

## Build
Follow `docs/sync.md` A.4, A.6 and A.12, and `docs/spec.md` §3 R2 and T, with that exact text.
- **Not already handed over** (part of R2):
  - in `OfferChecker.check`, if `local` holds a different handover with the same `prev_id` (not hidden by tamper, not rejected), the offer fails
  - add it to the existing "Not used before" line as a second reason, so R2 keeps six lines
  - the text: "Ravi Kumar already handed C-12 to Tan Wei Ming at 14:30. This would be a second handover." (names from the roster, time from that record's `accepted_ms`)
  - `looksLikeCopy` stays true for it, so the red header reads "This is a copy. It can't be used."
- **Withdrawn keys:**
  - `DeviceEntity.revokedAtMs` is already on `main` and filled by sync.
  - Add `Roster.withdrawnAt(deviceId): Long?`.
  - In `OfferChecker.check`, line 1 fails if the giver's device was withdrawn at or before `offered_ms + 15 s`: "<name>'s phone was withdrawn by the safety officer at 14:40."
  - The proof record `p` fails line 2 if either of its devices was withdrawn before its `accepted_ms` (same 15 s allowance).
  - In `OfferChecker.checkAcceptance`, refuse a receiver device withdrawn before `accepted_ms`, with the same text.
  - Signatures made before the withdrawal still verify.
- **`deriveCustody`:** keep it exactly as it is for the Disputed status. Add `localFork: Boolean` to `CustodyState`: true when the fork is visible on this phone but none of its branch records has a `server_seq` yet.
- **Timeline (T):**
  - when `localFork`: a red warning card above the branches: "This phone holds two handovers of C-12 from Ravi Kumar after H1. Only one can be right. The safety officer will decide once this phone syncs."
  - a key withdrawn after a record was signed shows "key withdrawn 14:40" beside that entry's signature check, in the muted colour
- **Tests** (unit, in `test/.../crypto/`):
  - a second handover from the same previous record is refused
  - a different camera's record doesn't trigger it
  - offers and acceptances from a withdrawn device are refused only after `revokedAtMs` (with the 15 s allowance)
  - `localFork` turns false once one branch has a `server_seq`

## Offline behaviour
- Everything here is offline: it uses only what's in Room.
- A phone that hasn't synced the revocation can't know about it. That's expected, and the server still rejects the record (C.3.3b).

## Acceptance criteria (checked on a phone)
- [ ] Debug **Tampered phone** on Ravi. Offline, Ravi hands C-12 to Wei Ming, then signs a second offer of C-12 for Siti. Siti hasn't seen the first handover, so she accepts it, as the demo needs.
  - Then on Wei Ming's phone (which holds the first record), scan Ravi's second offer to Siti: R2 refuses it with the "already handed" text.
- [ ] On a phone holding both branches before syncing, C-12's timeline shows the red local warning.
- [ ] After the safety officer withdraws Ravi's phone and Siti's phone syncs: an offer Ravi signs afterwards is refused on Siti's phone with the withdrawn text, even in airplane mode. Older records on C-12's timeline still show ✓, with "key withdrawn 14:40".
- [ ] `./gradlew quickCheck` passes, with the new tests.

## Test steps
1. Run the crypto unit tests.
2. On two phones, run the tamper scenario above with one phone holding both records.
3. After card 13 merges: withdraw a device on the emulator as Dr Tan, sync the other phone, and try an offer from the withdrawn phone.

## Notes
- **Data:** `handovers`, `devices` (`revokedAtMs`), `people`, `resolutions`.
- **Don't touch** `ui/receive/`. It shows whatever lines `OfferChecker` returns.
- **Don't touch** `DeviceKeys.kt` or `IdentityRepository.kt` (card 13 is there).
- **Design:** the warning card uses `Tenet.colors.alert` with `hazardStripes`, as the Disputed chip does. Read `design/brand.md`.
