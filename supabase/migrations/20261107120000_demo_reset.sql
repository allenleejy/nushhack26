-- Demo reset you can call yourself (docs/spec.md §2, reset-demo.bat).
--
-- demo_reset_apply(withdraw) does the work. Only the database owner can run it, so the
-- SQL editor and supabase/reset_demo.sql call it directly.
-- demo_reset(token, withdraw) is the same through the REST API with the publishable key,
-- guarded by a long random token. Only the token's SHA-256 is on the server, in
-- demo_secrets, which has RLS on and no policies, so no phone can read it.
--
-- withdraw = false: every signed handover, alert, resolution, report and relayed offer is
-- deleted, so each camera is "In store" with Siti again. Enrolled phones stay enrolled;
-- each one needs Debug -> Clear local data -> Sync now.
-- withdraw = true: also withdraws every enrolled phone. On its next sync each phone wipes
-- itself and shows Enrol, ready to enrol as anyone (seed.sql codes).

create table public.demo_secrets (
  name text primary key,
  sha256 text not null
);
alter table public.demo_secrets enable row level security;
revoke all on table public.demo_secrets from public, anon, authenticated;

create or replace function public.demo_reset_apply(p_withdraw boolean default false) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  v_handovers int;
  v_withdrawn int := 0;
begin
  -- Rows that reference handovers or alerts go first.
  delete from resolutions where true;
  delete from alerts where true;
  delete from missing_reports where true;
  delete from offer_relay where true;
  delete from enrol_attempts where true;
  -- Op receipts for the deleted rows, so a retried push is applied fresh. The register
  -- records were seeded directly, so they have no receipts.
  delete from applied_ops where table_name in ('handovers', 'alerts', 'resolutions', 'missing_reports');
  delete from handovers where kind <> 'register';
  get diagnostics v_handovers = row_count;

  if p_withdraw then
    update devices set revoked_at = now(), version = version + 1,
      revoked_by = (select id from people where role = 'rso' order by staff_no limit 1)
    where revoked_at is null and deleted_at is null;
    get diagnostics v_withdrawn = row_count;
  else
    -- A phone withdrawn during rehearsal works again: its session's newest device only, so
    -- rows a phone has since replaced by enrolling again stay withdrawn.
    update devices d set revoked_at = null, revoked_by = null, version = d.version + 1
    where d.revoked_at is not null and not exists (
      select 1 from devices n where n.created_by = d.created_by and n.received_at > d.received_at);
  end if;

  return jsonb_build_object('result', 'reset', 'handovers_deleted', v_handovers,
    'phones_withdrawn', v_withdrawn);
end;
$$;
revoke execute on function public.demo_reset_apply(boolean) from public, anon, authenticated;

create or replace function public.demo_reset(p_token text, p_withdraw boolean default false) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
begin
  if not exists (
    select 1 from demo_secrets
    where name = 'reset' and sha256 = encode(extensions.digest(coalesce(p_token, ''), 'sha256'), 'hex')
  ) then
    return jsonb_build_object('result', 'rejected', 'reason', 'wrong token');
  end if;
  return demo_reset_apply(p_withdraw);
end;
$$;
revoke execute on function public.demo_reset(text, boolean) from public;
grant execute on function public.demo_reset(text, boolean) to anon, authenticated;
