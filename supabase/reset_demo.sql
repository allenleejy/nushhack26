-- Demo reset: put the server back to the start of docs/spec.md section 2.
-- The usual way is `bash scripts/reset-demo.sh` on the laptop. This file is the same reset
-- for the SQL editor or the Supabase MCP. It is NOT a migration: the work is done by
-- demo_reset_apply (supabase/migrations/20261107120000_demo_reset.sql).
-- Safe to run many times in a row.
--
-- After a reset, every phone must forget what it saw and pull the clean state:
--   on each phone: Debug (tap the version on Me 5 times) -> "Clear local data" -> "Sync now".
-- The phone's identity key is in its own secure chip, so clearing local data does NOT
-- un-enrol the phone: it pulls its device row again on the next sync.

select public.demo_reset_apply(false);

-- ---------------------------------------------------------------------------
-- Full re-enrol instead: also withdraws every enrolled phone. On its next sync each phone
-- wipes itself and shows Enrol, ready to enrol as anyone (seed.sql codes).
--select public.demo_reset_apply(true);
-- ---------------------------------------------------------------------------
