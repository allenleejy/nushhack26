# 02: Sync engine: push the outbox, pull changes, handle every result

Wave: 1 · Devin mode: Fusion · Depends on: the plan job (the server functions, `SyncApi` and the sync tests are on `main`)

## Goal
Whenever there's a network, the phone sends its waiting records to `sync_push`, marks each one by its result, pulls everything new from `sync_pull` into Room, and shows the outcome. Nothing is lost, doubled or silently dropped.

## Files owned
- app/src/main/java/com/nushhack26/app/sync/SyncEngine.kt
- app/src/main/java/com/nushhack26/app/sync/SyncWorker.kt
- app/src/main/java/com/nushhack26/app/data/remote/SupabaseSyncApi.kt
- app/src/test/java/com/nushhack26/app/sync/

## Build
Follow `docs/sync.md` §D and §E exactly.
- **`SupabaseSyncApi`** implements the `SyncApi` interface, calling the RPCs `sync_push(ops)` and `sync_pull(since)` through supabase-kt Postgrest. If there's no client or no session, it throws `NotReadyException`, so the worker retries later.
- **`SyncEngine.run()`:**
  1. Push the outbox in batches of 100, oldest first.
  2. For each result, update the local record and delete its outbox entry (E's table). On a network error, call `markFailed` and stop.
  3. Pull from `sync_state.last_server_seq` until `more` is false, upserting with the merge rules in E.
  4. Write `LAST_SYNC_AT`, or `LAST_ERROR` on failure.
- **Mapping:** use `ServerRows.kt` (`@Serializable` rows and the `toEntity`/`toRow` mappers already in `data/remote/`) for every table.
- **`SyncWorker.doWork()`** calls `container.syncEngine.run()`, with the retry and backoff it already has.
- **Tests:** remove every `@Ignore("card 02")` in `app/src/test/.../sync/` and make them pass against `FakeSyncServer`. Fix the fake only where it differs from `docs/sync.md` §C, and say so in the pull request.

## Offline behaviour
- With no network, the engine never runs, and records wait in the outbox, shown by the sync bar.
- With the network back, it runs by itself (the scheduler already triggers on reconnect).

## Acceptance criteria (checked on a phone)
- [ ] All sync tests in `app/src/test/.../sync/` pass, with none ignored.
- [ ] Online, on a phone enrolled through card 07's Enrol (or `EnrolRepository.enrol`): Sync now pulls the roster and the cameras, and the sync bar shows "All synced". Devices are no longer pushed: the server adds them in `enrol_device` (docs/sync.md C.2).
- [ ] On a phone that isn't enrolled, the server raises "not enrolled" (C.0): the engine keeps the outbox and writes `LAST_ERROR`, like a network error.
- [ ] Offline: create a record (a handover from card 03 or 04). The sync bar shows "1 waiting". Airplane mode off, and it goes to "All synced", with one row on the server.
- [ ] A forced failure (no server config) shows the error in Debug, and the record stays waiting.
- [ ] A pulled fork alert appears in the local `alerts` table, so card 06 shows it.

## Test steps
1. `./gradlew quickCheck`
2. Phone online: Debug → **Sync now**. The outbox empties, and the last sync time updates.
3. Airplane mode: make a handover. Airplane off → Sync now. The server has it once.

## Notes
- **Data:** every table in `docs/sync.md` §B, plus `outbox` and `sync_state`.
- The pushed payload is the server row JSON for that record (snake_case keys). Already written into `outbox.payload` by the repositories.
- Don't add libraries. Use supabase-kt's `postgrest.rpc`.
