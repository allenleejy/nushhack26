# 23: Me, Outbox and dates: shorter, plainer text

Wave: 4 (polish) · Devin mode: Lite · Depends on: none

## Goal
Me says your role and how long it's valid in one line, the Access card only talks about offline access, the empty Outbox says "Every record has been synced.", and every date in the app reads DD.MM.YYYY.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/me/MeScreen.kt, MeViewModel.kt
- app/src/main/java/com/nushhack26/app/ui/outbox/OutboxScreen.kt
- app/src/main/java/com/nushhack26/app/auth/AccessRules.kt (the date helpers only)
- app/src/test/java/com/nushhack26/app/auth/AccessRulesTest.kt
- app/src/test/java/com/nushhack26/app/screenshots/OutboxScreenshotTest.kt
- app/src/test/java/com/nushhack26/app/screenshots/MeAccessScreenshotTest.kt (new)

Don't edit `MeScreenshotTest.kt`: an open PR edits it.

## Build
- **Me, the line under the name** (`record`, uppercase), replacing "RADIOGRAPHER · R-2210":
  - authorised with an expiry: `RADIOGRAPHER · R-2210 · VALID TO 30.06.2027`
  - no expiry, or the store officer: `STORE OFFICER · S-1021`
  - lapsed: `RADIOGRAPHER · R-2251 · LAPSED 03.11.2026`, in `alertText`
  - not on the active roster: `RADIOGRAPHER · R-2251 · NOT ON THE ACTIVE ROSTER`, in `alertText`
  - before the person has synced: `RADIOGRAPHER`

  Keep `testTag("me_role")` on this line.
- **Me, the Access card:** remove the "Authorised radiographer · valid to …" line and its `me_authorised` tag. The card keeps only "Access confirmed 14:02 · valid offline until Mon 14:02" (`me_lease`) and **LOCK NOW**.
- **Dates:** `AccessRules.longDate` and `AccessRules.shortDate` both return `dd.MM.yyyy` (`Locale.UK`). That changes:
  - the G1 picker: "Jun Lim · authorisation lapsed 03.11.2026"
  - R2 line 6: "Authorised · radiographer · valid to 30.06.2027"
  - the refusal: "You aren't authorised to hold sealed sources (lapsed 03.11.2026)"

  Update `AccessRulesTest` to match. Times and the lease lines ("Mon 14:02") don't change.
- **Outbox empty:** `OUTBOX_EMPTY_TEXT = "Every record has been synced."`. The last sync time stays under it.

## Offline behaviour
- Text only. Everything shows offline, as before.

## Acceptance criteria (checked on a phone)
- [ ] Me (tap the initials square) shows `RADIOGRAPHER · R-2210 · VALID TO 30.06.2027` under RAVI KUMAR, and the Access card has only the offline-access line and LOCK NOW.
- [ ] On Jun's phone, or by picking Jun in G1: `LAPSED 03.11.2026` in orange.
- [ ] Outbox with nothing waiting: "Every record has been synced."
- [ ] It fits at 360 dp and 1.3× font size.

## Test steps
1. Open Me on Ravi's and Siti's phones.
2. Sync, then open OUTBOX.

## Notes
- **Data:** `people.authorised_until`, through `MeViewModel` as now.
- **Screenshots:** `MeAccessScreenshotTest`: valid, lapsed, store officer. Update `OutboxScreenshotTest` for the new text.
