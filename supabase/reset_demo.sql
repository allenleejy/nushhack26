-- Demo reset: put the server back to the start of docs/spec.md section 2.
-- Run it through the Supabase MCP or the SQL editor. It is NOT a migration: it only
-- deletes rows and clears one column, so it never appears in supabase/migrations/.
-- Safe to run many times in a row.
--
-- After a reset, every phone must forget what it saw and pull the clean state:
--   on each phone: Debug (tap the version on Me 5 times) -> "Clear local data" -> "Sync now".
-- The phone's identity key is in its own secure chip, so clearing local data does NOT
-- un-enrol the phone: it pulls its device row again on the next sync.

begin;

-- Rows that reference handovers or alerts go first.
delete from public.resolutions;
delete from public.alerts;
delete from public.missing_reports;
delete from public.offer_relay;
delete from public.enrol_attempts;

-- Op receipts for the deleted rows, so a retried push after the reset is applied
-- fresh instead of answered from a stale receipt. The three register records were
-- seeded directly (not through sync_push), so they have no receipts to keep.
delete from public.applied_ops
where table_name in ('handovers', 'alerts', 'resolutions', 'missing_reports');

-- Every signed handover. The three server-made register records stay: each camera
-- starts "In store" with Siti again.
delete from public.handovers where kind <> 'register';

-- A phone withdrawn during rehearsal works again. people, items and devices stay,
-- so enrolled phones stay enrolled.
update public.devices set revoked_at = null, revoked_by = null where revoked_at is not null;

commit;

-- ---------------------------------------------------------------------------
-- Optional: full re-enrol. Uncomment to also forget every phone, so each device
-- enrols from scratch with a staff number and one-time code (seed.sql codes).
--delete from public.devices;
-- ---------------------------------------------------------------------------
