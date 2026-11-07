# 32: Report a camera missing

Wave: 8 (final fixes) · Devin mode: Normal · Depends on: none

## Goal
From a camera's Timeline, anyone enrolled can report it missing, with the last signed holder already filled in. The report saves at once, offline too, and syncs like every other record. Right now the screen is a placeholder that says "Card 07 builds this screen."

## Files owned
- app/src/main/java/com/nushhack26/app/ui/missing/ReportMissingScreen.kt (replace the placeholder)
- app/src/main/java/com/nushhack26/app/ui/missing/ReportMissingViewModel.kt (new)
- app/src/main/java/com/nushhack26/app/data/repo/SafetyRepository.kt (add `saveMissingReport` only)
- app/src/test/java/com/nushhack26/app/screenshots/ReportMissingScreenshotTest.kt (new)

## Already on main (don't rebuild)
- The route: `ReportMissingRoute(itemId)`, wired in AppNavHost to `ReportMissingScreen(container, itemId, onDone)`. Timeline's **Report missing** button already opens it. Keep that signature.
- Room: `MissingReportEntity` (`id`, `itemId`, `reportedByPersonId`, `reportedByDeviceId`, `lastHandoverId`, `note`, `reportedMs`, `syncStatus`, `syncReason`) and `MissingReportDao.insert` / `observeAll`.
- Sync: `MissingReportEntity.toPayload()` in `data/remote/ServerRows.kt`, `ServerTables.MISSING_REPORTS`, and `SyncEngine` already marks a pushed report synced or rejected. The server's `sync_push` always applies a missing report (docs/sync.md C.5).
- The Outbox already lists missing reports.

## Build
- **`SafetyRepository.saveMissingReport(report: MissingReportEntity)`**: the same shape as `saveResolution`. In one `withTransaction`, insert the report with `syncStatus = SyncMark.WAITING` and `outboxDao().enqueueInsert(ServerTables.MISSING_REPORTS, report.id, report.toPayload())`, then `requestSync()`.
- **ViewModel**: reads through `container.custodyRepository` (`observeItems`, `observeChain(itemId)`, `observeRoster`), `container.identity` and `container.safetyRepository`, with no Supabase calls.
  - The camera: code, name, source serial and isotope, and seal.
  - **Last signed holder**: the receiver of the newest record in the chain that both people signed (`acceptedMs != null`), skipping records hidden by tamper or rejected. With only the register record, it's the store holder ("in store with Siti Aminah"). Show their name, role, and when they signed ("signed 14:29 · H2").
  - `lastHandoverId` is that record's id, or the register record's id.
- **Screen** (spec §3 "Report missing", reg 26(3)), in the app's existing style (TenetType, Tenet.colors, the hazard band and buttons the other screens use):
  - Title "Report C-12 missing" (the camera's code), then the camera card.
  - "Last signed holder" with the prefilled person. It can't be edited: it comes from the signed chain.
  - A **Note** field, optional, up to 280 characters, with hint text "Where and when it was last seen".
  - The primary button **SEND REPORT**. It saves with a phone-made UUID, `reportedByPersonId` and `reportedByDeviceId` from `identity.current()`, and `reportedMs = now`.
  - **After saving**, a confirmation on the same screen: "Reported C-12 missing · 14:52". Under it, the report's own sync mark, observed from Room: "Report queued · sends when online" while waiting, then "Sent ✓" once synced, or the reject reason in red. Then a **DONE** button that calls `onDone`.
  - If this phone already has a report for this camera, show it at the top ("Reported missing by Wei Ming · 14:52") above the form, and still allow a second report.
- **States**: Loading (skeleton), camera not found ("This camera isn't on this phone. Sync once."), a save error ("Couldn't save the report. Try again."), and not enrolled (can't happen from the Timeline, but show the error state, not a crash).

## Offline behaviour
- Everything works in airplane mode: the report saves to Room with its outbox entry straight away and shows "Report queued · sends when online".
- When the network returns, the report syncs once (the outbox op id makes a retry safe) and the mark turns "Sent ✓".

## Acceptance criteria (checked on a phone)
- [ ] Timeline → **Report missing** opens the form, with the right last signed holder: after Siti → Ravi → Wei Ming, it's Wei Ming.
- [ ] In airplane mode, **SEND REPORT** shows the confirmation with "Report queued · sends when online", and the Outbox shows the report waiting.
- [ ] Airplane mode off: within a few seconds the mark turns "Sent ✓", and the server has exactly one row in `missing_reports`.
- [ ] Reopening the form shows "Reported missing by …" at the top.
- [ ] Loading, not-found and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size, with 48 dp touch targets.

## Test steps
1. Siti issues Kestrel to Ravi. Then, on any phone: Kestrel → Timeline → **Report missing**.
2. Check the holder, type a note, turn on airplane mode, then **SEND REPORT**.
3. Turn airplane mode off and watch the mark turn "Sent ✓".

## Notes
- Data: `missing_reports` (Room and server), `handovers` and `people` read only.
- Screenshots: `ReportMissingScreenshotTest` renders the stateless `ReportMissingContent(state, onAction)` for the form, queued, sent and not-found states.
- Don't edit shared files (AppNavHost, Routes, AppContainer, entities, theme).
