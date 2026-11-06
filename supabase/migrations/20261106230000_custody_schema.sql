-- Schema v1: the signed custody chain for radiography cameras.
-- The rules are in docs/sync.md: section B (tables) and section C (what sync_push checks).

create extension if not exists pgcrypto with schema extensions;

-- One counter for every table, so a phone can pull "everything after the last server_seq I saw".
create sequence if not exists public.server_seq_seq;

create or replace function public.set_server_seq() returns trigger
language plpgsql as $$
begin
  new.server_seq := nextval('public.server_seq_seq');
  return new;
end $$;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table public.people (
  id uuid primary key,
  name text not null,
  role text not null check (role in ('store_officer', 'radiographer', 'rso')),
  staff_no text not null,
  nea_reg_no text,
  enrol_code_hash text,
  active boolean not null default true,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);

create table public.devices (
  id uuid primary key,
  person_id uuid not null references public.people (id),
  public_key text not null,
  key_fp text not null,
  key_security text not null check (key_security in ('strongbox', 'tee', 'software')),
  app_version text not null default '',
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);

create table public.items (
  id uuid primary key,
  code text not null unique,
  source_serial text not null,
  isotope text not null,
  seal_no text not null,
  store_name text not null,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);

create table public.handovers (
  id uuid primary key,
  item_id uuid not null references public.items (id),
  kind text not null check (kind in ('register', 'issue', 'transfer', 'return')),
  -- No foreign key: an orphan arrives before the record it follows.
  prev_id uuid,
  prev_hash text,
  from_person_id uuid references public.people (id),
  from_device_id uuid references public.devices (id),
  to_person_id uuid not null references public.people (id),
  to_device_id uuid references public.devices (id),
  seal_no text,
  due_ms bigint,
  nonce text unique,
  offered_ms bigint,
  expires_ms bigint,
  accepted_ms bigint,
  giver_sig text,
  receiver_sig text,
  record_hash text not null unique,
  status text not null default 'accepted' check (status in ('accepted', 'forked', 'overruled', 'orphan')),
  status_reason text,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);
create index handovers_item_seq on public.handovers (item_id, server_seq);
create index handovers_prev on public.handovers (prev_id);

create table public.alerts (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('fork', 'seal_mismatch', 'orphan', 'rejected')),
  item_id uuid references public.items (id),
  person_id uuid references public.people (id),
  handover_ids uuid[] not null default '{}',
  -- For a fork: the record both branches follow.
  fork_prev_id uuid,
  reason text not null,
  op_table text,
  op_payload jsonb,
  resolution_id uuid,
  closed_at timestamptz,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);
create index alerts_item on public.alerts (item_id);

create table public.resolutions (
  id uuid primary key,
  alert_id uuid not null references public.alerts (id),
  item_id uuid not null references public.items (id),
  chosen_handover_id uuid not null references public.handovers (id),
  decided_by_person_id uuid not null references public.people (id),
  decided_by_device_id uuid not null references public.devices (id),
  note text not null default '',
  decided_ms bigint not null,
  sig text not null,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);

create table public.missing_reports (
  id uuid primary key,
  item_id uuid not null references public.items (id),
  reported_by_person_id uuid not null references public.people (id),
  reported_by_device_id uuid not null references public.devices (id),
  last_handover_id uuid,
  note text not null default '',
  reported_ms bigint not null,
  version int not null default 1,
  server_seq bigint,
  deleted_at timestamptz,
  performed_at timestamptz not null default now(),
  received_at timestamptz not null default now(),
  created_by uuid
);

-- Every op sync_push has seen, so a repeat gets the same answer. Never pulled.
create table public.applied_ops (
  op_id uuid primary key,
  table_name text not null,
  result text not null,
  reason text,
  status text,
  record_hash text,
  applied_at timestamptz not null default now()
);

create trigger people_seq before insert or update on public.people for each row execute function public.set_server_seq();
create trigger devices_seq before insert or update on public.devices for each row execute function public.set_server_seq();
create trigger items_seq before insert or update on public.items for each row execute function public.set_server_seq();
create trigger handovers_seq before insert or update on public.handovers for each row execute function public.set_server_seq();
create trigger alerts_seq before insert or update on public.alerts for each row execute function public.set_server_seq();
create trigger resolutions_seq before insert or update on public.resolutions for each row execute function public.set_server_seq();
create trigger missing_reports_seq before insert or update on public.missing_reports for each row execute function public.set_server_seq();

-- ---------------------------------------------------------------------------
-- Row-level security: every signed-in phone reads everything; nobody writes
-- directly. All writes go through sync_push.
-- ---------------------------------------------------------------------------

alter table public.people enable row level security;
alter table public.devices enable row level security;
alter table public.items enable row level security;
alter table public.handovers enable row level security;
alter table public.alerts enable row level security;
alter table public.resolutions enable row level security;
alter table public.missing_reports enable row level security;
alter table public.applied_ops enable row level security;

create policy "team reads people" on public.people for select to authenticated using (true);
create policy "team reads devices" on public.devices for select to authenticated using (true);
create policy "team reads items" on public.items for select to authenticated using (true);
create policy "team reads handovers" on public.handovers for select to authenticated using (true);
create policy "team reads alerts" on public.alerts for select to authenticated using (true);
create policy "team reads resolutions" on public.resolutions for select to authenticated using (true);
create policy "team reads missing reports" on public.missing_reports for select to authenticated using (true);

-- The enrolment code hash is never readable: a 6-digit code's hash is easy to reverse.
revoke select on public.people from anon, authenticated;
grant select (id, name, role, staff_no, nea_reg_no, active, version, server_seq, deleted_at, performed_at, received_at, created_by)
  on public.people to authenticated;
revoke all on public.applied_ops from anon, authenticated;

-- ---------------------------------------------------------------------------
-- Helpers used inside sync_push
-- ---------------------------------------------------------------------------

-- Records the result of an op and returns it in the shape docs/sync.md D.1 describes.
create or replace function public._op_result(
  v_op_id uuid, v_table text, v_result text, v_reason text, v_status text, v_hash text, v_seq bigint
) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
begin
  insert into applied_ops (op_id, table_name, result, reason, status, record_hash)
  values (v_op_id, v_table, v_result, v_reason, v_status, v_hash)
  on conflict (op_id) do nothing;
  return jsonb_build_object('op_id', v_op_id, 'result', v_result, 'reason', v_reason, 'status', v_status, 'server_seq', v_seq);
end $$;

-- A rejected op is never stored as a record, but it's never dropped silently either:
-- it becomes a "rejected" alert with its payload.
create or replace function public._reject(
  v_op_id uuid, v_table text, p jsonb, v_reason text
) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_item uuid;
  v_person uuid;
begin
  begin
    v_item := (p ->> 'item_id')::uuid;
  exception when others then
    v_item := null;
  end;
  if v_item is not null and not exists (select 1 from items where id = v_item) then
    v_item := null;
  end if;
  begin
    v_person := coalesce((p ->> 'from_person_id')::uuid, (p ->> 'decided_by_person_id')::uuid, (p ->> 'person_id')::uuid);
  exception when others then
    v_person := null;
  end;
  if v_person is not null and not exists (select 1 from people where id = v_person) then
    v_person := null;
  end if;

  insert into alerts (kind, item_id, person_id, reason, op_table, op_payload, created_by)
  values ('rejected', v_item, v_person, v_reason, v_table, p, auth.uid());

  return _op_result(v_op_id, v_table, 'rejected', v_reason, null, p ->> 'record_hash', null);
end $$;

-- Works out where one stored handover fits in its camera's chain, sets its status,
-- and raises or closes alerts. Called for a new record, and again for each orphan
-- once the record it follows arrives. Returns the status.
create or replace function public._settle_handover(v_id uuid) returns text
language plpgsql security definer set search_path = public, extensions as $$
declare
  h handovers;
  prev handovers;
  it items;
  v_status text;
  v_reason text;
  v_giver text;
  v_alert uuid;
  v_siblings uuid[];
  v_child uuid;
begin
  select * into h from handovers where id = v_id;
  select * into it from items where id = h.item_id;
  select * into prev from handovers where id = h.prev_id;

  if not found then
    update handovers set status = 'orphan', status_reason = 'waiting for an earlier record' where id = v_id;
    if not exists (select 1 from alerts where kind = 'orphan' and closed_at is null and v_id = any (handover_ids)) then
      insert into alerts (kind, item_id, person_id, handover_ids, reason, created_by)
      values ('orphan', h.item_id, h.from_person_id, array[v_id], 'Waiting for an earlier record', auth.uid());
    end if;
    return 'orphan';
  end if;

  -- The record it follows is here now.
  update alerts set closed_at = now()
  where kind = 'orphan' and closed_at is null and v_id = any (handover_ids);

  if prev.to_person_id is distinct from h.from_person_id or prev.record_hash is distinct from h.prev_hash then
    -- It was stored as an orphan, but it doesn't fit the chain. Keep it, flag it.
    v_reason := case when prev.to_person_id is distinct from h.from_person_id
                     then 'giver was not the holder' else 'broken link' end;
    update handovers set status = 'orphan', status_reason = v_reason where id = v_id;
    insert into alerts (kind, item_id, person_id, handover_ids, reason, created_by)
    values ('rejected', h.item_id, h.from_person_id, array[v_id], v_reason, auth.uid());
    return 'orphan';
  end if;

  if prev.status = 'orphan' then
    v_status := 'orphan';
    v_reason := 'waiting for an earlier record';
  elsif prev.status in ('forked', 'overruled') then
    v_status := 'forked';
    v_reason := 'follows a disputed record';
  elsif exists (select 1 from handovers c where c.prev_id = prev.id and c.id <> v_id and c.status = 'accepted') then
    v_status := 'forked';
    v_reason := 'second handover after the same record';
  else
    v_status := 'accepted';
    if h.seal_no is distinct from it.seal_no then
      v_reason := 'seal mismatch';
      insert into alerts (kind, item_id, person_id, handover_ids, reason, created_by)
      values ('seal_mismatch', h.item_id, h.to_person_id, array[v_id],
              format('%s accepted with seal %s (expected %s)', it.code, coalesce(h.seal_no, 'none'), it.seal_no), auth.uid());
    end if;
  end if;

  update handovers set status = v_status, status_reason = v_reason where id = v_id;

  if v_status = 'forked' then
    select array_agg(c.id order by c.server_seq) into v_siblings from handovers c where c.prev_id = prev.id;
    select a.id into v_alert from alerts a
    where a.kind = 'fork' and a.closed_at is null and a.item_id = h.item_id
      and (a.handover_ids && v_siblings or prev.id = any (a.handover_ids))
    limit 1;
    if v_alert is null then
      select name into v_giver from people where id = h.from_person_id;
      insert into alerts (kind, item_id, person_id, handover_ids, fork_prev_id, reason, created_by)
      values ('fork', h.item_id, h.from_person_id, v_siblings, prev.id,
              format('%s signed two handovers of %s after the same record', v_giver, it.code), auth.uid());
    else
      update alerts
      set handover_ids = (select array_agg(distinct x) from unnest(handover_ids || v_id) x)
      where id = v_alert;
    end if;
  end if;

  -- Any orphan waiting for this record can be placed now.
  if v_status <> 'orphan' then
    for v_child in select id from handovers where prev_id = v_id and status = 'orphan' order by server_seq loop
      perform _settle_handover(v_child);
    end loop;
  end if;

  return v_status;
end $$;

create or replace function public._apply_handover(v_op_id uuid, p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  h handovers;
  it items;
  v_offer text;
  v_accept text;
  v_hash text;
  v_from_role text;
  v_to_role text;
  v_status text;
  v_seq bigint;
begin
  h := jsonb_populate_record(null::handovers, p);

  if h.id is null or h.item_id is null or h.to_person_id is null or h.prev_id is null or h.kind is null
     or h.kind not in ('issue', 'transfer', 'return') then
    return _reject(v_op_id, 'handovers', p, 'malformed record');
  end if;

  select * into it from items where id = h.item_id for update;
  if not found then
    return _reject(v_op_id, 'handovers', p, 'unknown camera');
  end if;

  -- 1. The hash must match the fields (docs/sync.md A.2).
  v_offer := 'CS1-OFFER|' || h.id::text || '|' || h.item_id::text || '|' || h.kind
    || '|' || coalesce(h.prev_id::text, '') || '|' || coalesce(h.prev_hash, '')
    || '|' || coalesce(h.from_person_id::text, '') || '|' || coalesce(h.from_device_id::text, '')
    || '|' || h.to_person_id::text || '|' || coalesce(h.due_ms::text, '') || '|' || coalesce(h.nonce, '')
    || '|' || coalesce(h.offered_ms::text, '') || '|' || coalesce(h.expires_ms::text, '');
  v_accept := 'CS1-ACCEPT|' || v_offer || '|' || coalesce(h.giver_sig, '') || '|' || coalesce(h.to_device_id::text, '')
    || '|' || coalesce(h.seal_no, '') || '|' || coalesce(h.accepted_ms::text, '');
  v_hash := encode(extensions.digest(convert_to(v_accept || '|' || coalesce(h.receiver_sig, ''), 'UTF8'), 'sha256'), 'hex');
  if v_hash is distinct from h.record_hash then
    return _reject(v_op_id, 'handovers', p, 'hash mismatch');
  end if;

  -- 2. A one-time number works once.
  if h.nonce is null or exists (select 1 from handovers where nonce = h.nonce and id <> h.id) then
    return _reject(v_op_id, 'handovers', p, 'replayed offer');
  end if;

  -- 3. Both devices must be on the roster and belong to the people named.
  if not exists (select 1 from devices where id = h.from_device_id and person_id = h.from_person_id)
     or not exists (select 1 from devices where id = h.to_device_id and person_id = h.to_person_id) then
    return _reject(v_op_id, 'handovers', p, 'unknown signer');
  end if;

  -- 4. The roles must fit the kind.
  select role into v_from_role from people where id = h.from_person_id;
  select role into v_to_role from people where id = h.to_person_id;
  if not ((h.kind = 'issue' and v_from_role = 'store_officer' and v_to_role = 'radiographer')
       or (h.kind = 'transfer' and v_from_role = 'radiographer' and v_to_role = 'radiographer')
       or (h.kind = 'return' and v_from_role = 'radiographer' and v_to_role = 'store_officer')) then
    return _reject(v_op_id, 'handovers', p, 'not authorised');
  end if;

  -- 5. If the previous record is here, the giver must have held it and the link must match.
  if exists (select 1 from handovers where id = h.prev_id and to_person_id is distinct from h.from_person_id) then
    return _reject(v_op_id, 'handovers', p, 'giver was not the holder');
  end if;
  if exists (select 1 from handovers where id = h.prev_id and record_hash is distinct from h.prev_hash) then
    return _reject(v_op_id, 'handovers', p, 'broken link');
  end if;

  -- Store it, then work out where it fits (rules 5–10).
  h.status := 'orphan';
  h.status_reason := null;
  h.version := 1;
  h.server_seq := null;
  h.deleted_at := null;
  h.performed_at := coalesce(to_timestamp(h.accepted_ms / 1000.0), now());
  h.received_at := now();
  h.created_by := auth.uid();
  insert into handovers select (h).*;

  v_status := _settle_handover(h.id);
  select server_seq into v_seq from handovers where id = h.id;

  return _op_result(v_op_id, 'handovers', case when v_status = 'accepted' then 'applied' else 'conflict' end,
                    case when v_status = 'accepted' then null else (select status_reason from handovers where id = h.id) end,
                    v_status, h.record_hash, v_seq);
end $$;

create or replace function public._apply_device(v_op_id uuid, p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  d devices;
  v_seq bigint;
begin
  d := jsonb_populate_record(null::devices, p);
  if d.id is null or d.person_id is null or d.public_key is null or d.key_fp is null
     or d.key_security not in ('strongbox', 'tee', 'software') then
    return _reject(v_op_id, 'devices', p, 'malformed record');
  end if;
  if not exists (select 1 from people where id = d.person_id and active) then
    return _reject(v_op_id, 'devices', p, 'unknown person');
  end if;
  -- Wave 2 adds the enrolment code check here (docs/sync.md C.2).

  d.app_version := coalesce(d.app_version, '');
  d.version := 1;
  d.server_seq := null;
  d.deleted_at := null;
  d.performed_at := coalesce(d.performed_at, now());
  d.received_at := now();
  d.created_by := auth.uid();
  insert into devices select (d).*;
  select server_seq into v_seq from devices where id = d.id;
  return _op_result(v_op_id, 'devices', 'applied', null, null, null, v_seq);
end $$;

create or replace function public._apply_resolution(v_op_id uuid, p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  r resolutions;
  a alerts;
  v_root uuid;
  v_cursor uuid;
  v_seq bigint;
begin
  r := jsonb_populate_record(null::resolutions, p);
  if r.id is null or r.alert_id is null or r.item_id is null or r.chosen_handover_id is null
     or r.decided_by_person_id is null or r.decided_by_device_id is null or r.decided_ms is null or r.sig is null then
    return _reject(v_op_id, 'resolutions', p, 'malformed record');
  end if;
  if not exists (select 1 from people where id = r.decided_by_person_id and role = 'rso') then
    return _reject(v_op_id, 'resolutions', p, 'not a safety officer');
  end if;
  if not exists (select 1 from devices where id = r.decided_by_device_id and person_id = r.decided_by_person_id) then
    return _reject(v_op_id, 'resolutions', p, 'unknown signer');
  end if;

  select * into a from alerts where id = r.alert_id for update;
  if not found or a.kind <> 'fork' then
    return _reject(v_op_id, 'resolutions', p, 'not a fork alert');
  end if;
  if a.closed_at is not null then
    return _reject(v_op_id, 'resolutions', p, 'already resolved');
  end if;
  if not (r.chosen_handover_id = any (a.handover_ids)) then
    return _reject(v_op_id, 'resolutions', p, 'not part of this fork');
  end if;

  r.note := coalesce(r.note, '');
  r.version := 1;
  r.server_seq := null;
  r.deleted_at := null;
  r.performed_at := to_timestamp(r.decided_ms / 1000.0);
  r.received_at := now();
  r.created_by := auth.uid();
  insert into resolutions select (r).*;

  -- The chosen branch starts at the child of the fork point that leads to the chosen record.
  v_cursor := r.chosen_handover_id;
  loop
    select prev_id into v_root from handovers where id = v_cursor;
    exit when v_root is null or v_root = a.fork_prev_id;
    v_cursor := v_root;
  end loop;
  v_root := v_cursor;

  with recursive branch (id, chosen) as (
    select c.id, c.id = v_root from handovers c where c.prev_id = a.fork_prev_id
    union all
    select c.id, b.chosen from handovers c join branch b on c.prev_id = b.id
  )
  update handovers h
  set status = case when b.chosen then 'accepted' else 'overruled' end,
      status_reason = case when b.chosen then h.status_reason else 'overruled by the safety officer' end
  from branch b
  where h.id = b.id and h.status <> 'orphan';

  update alerts set resolution_id = r.id, closed_at = now() where id = a.id;

  select server_seq into v_seq from resolutions where id = r.id;
  return _op_result(v_op_id, 'resolutions', 'applied', null, null, null, v_seq);
end $$;

create or replace function public._apply_missing_report(v_op_id uuid, p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  m missing_reports;
  v_seq bigint;
begin
  m := jsonb_populate_record(null::missing_reports, p);
  if m.id is null or m.item_id is null or m.reported_by_person_id is null
     or m.reported_by_device_id is null or m.reported_ms is null then
    return _reject(v_op_id, 'missing_reports', p, 'malformed record');
  end if;
  m.note := coalesce(m.note, '');
  m.version := 1;
  m.server_seq := null;
  m.deleted_at := null;
  m.performed_at := to_timestamp(m.reported_ms / 1000.0);
  m.received_at := now();
  m.created_by := auth.uid();
  insert into missing_reports select (m).*;
  select server_seq into v_seq from missing_reports where id = m.id;
  return _op_result(v_op_id, 'missing_reports', 'applied', null, null, null, v_seq);
end $$;

create or replace function public._apply_op(op jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_op_id uuid := (op ->> 'op_id')::uuid;
  v_table text := op ->> 'table';
  p jsonb := coalesce(op -> 'payload', '{}'::jsonb);
  prior applied_ops;
begin
  select * into prior from applied_ops where op_id = v_op_id;
  if found then
    if v_table = 'handovers' and prior.record_hash is not null
       and prior.record_hash is distinct from (p ->> 'record_hash') then
      -- Same id, different content: someone changed a signed record.
      insert into alerts (kind, item_id, reason, op_table, op_payload, created_by)
      values ('rejected', (select id from items where id::text = p ->> 'item_id'),
              'conflicting copy of the same record', v_table, p, auth.uid());
      return jsonb_build_object('op_id', v_op_id, 'result', 'rejected',
                                'reason', 'conflicting copy of the same record', 'status', null, 'server_seq', null);
    end if;
    return jsonb_build_object('op_id', v_op_id,
                              'result', case when prior.result = 'rejected' then 'rejected' else 'duplicate' end,
                              'reason', prior.reason, 'status', prior.status, 'server_seq', null);
  end if;

  if coalesce(op ->> 'kind', 'insert') <> 'insert' then
    return _reject(v_op_id, v_table, p, 'only inserts are allowed');
  end if;

  return case v_table
    when 'handovers' then _apply_handover(v_op_id, p)
    when 'devices' then _apply_device(v_op_id, p)
    when 'resolutions' then _apply_resolution(v_op_id, p)
    when 'missing_reports' then _apply_missing_report(v_op_id, p)
    else _reject(v_op_id, v_table, p, 'unknown table')
  end;
end $$;

-- ---------------------------------------------------------------------------
-- The two calls the phones make
-- ---------------------------------------------------------------------------

-- Applies a batch of outbox entries in one transaction (docs/sync.md D.1).
create or replace function public.sync_push(ops jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  op jsonb;
  r jsonb;
  results jsonb := '[]'::jsonb;
begin
  if auth.uid() is null then
    raise exception 'not signed in';
  end if;
  -- One push at a time, so server_seq order matches commit order and a pull never skips a row.
  perform pg_advisory_xact_lock(726354);

  for op in select value from jsonb_array_elements(ops) loop
    begin
      r := _apply_op(op);
    exception when others then
      begin
        r := _reject((op ->> 'op_id')::uuid, op ->> 'table', coalesce(op -> 'payload', '{}'::jsonb), 'server error: ' || sqlerrm);
      exception when others then
        r := jsonb_build_object('op_id', op ->> 'op_id', 'result', 'rejected', 'reason', 'unreadable op', 'status', null, 'server_seq', null);
      end;
    end;
    results := results || jsonb_build_array(r);
  end loop;
  return results;
end $$;

-- Returns every row changed after `since`, at most 2,000 per call (docs/sync.md D.2).
create or replace function public.sync_pull(since bigint) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  lim constant int := 2000;
  v_max bigint;
  v_more boolean;
  v_rows jsonb;
begin
  if auth.uid() is null then
    raise exception 'not signed in';
  end if;

  with all_rows (t, s, j) as (
    select 'people', p.server_seq, to_jsonb(p) - 'enrol_code_hash' from people p where p.server_seq > since
    union all select 'devices', d.server_seq, to_jsonb(d) from devices d where d.server_seq > since
    union all select 'items', i.server_seq, to_jsonb(i) from items i where i.server_seq > since
    union all select 'handovers', h.server_seq, to_jsonb(h) from handovers h where h.server_seq > since
    union all select 'alerts', a.server_seq, to_jsonb(a) from alerts a where a.server_seq > since
    union all select 'resolutions', r.server_seq, to_jsonb(r) from resolutions r where r.server_seq > since
    union all select 'missing_reports', m.server_seq, to_jsonb(m) from missing_reports m where m.server_seq > since
  ),
  page as (
    select t, s, j, row_number() over (order by s) as rn from (select * from all_rows order by s limit lim + 1) x
  ),
  kept as (select * from page where rn <= lim)
  select
    coalesce((select max(s) from kept), since),
    (select count(*) from page) > lim,
    coalesce((select jsonb_object_agg(t, arr) from (select t, jsonb_agg(j order by s) as arr from kept group by t) g), '{}'::jsonb)
  into v_max, v_more, v_rows;

  return jsonb_build_object(
    'max_seq', v_max,
    'more', v_more,
    'rows', jsonb_build_object(
      'people', coalesce(v_rows -> 'people', '[]'::jsonb),
      'devices', coalesce(v_rows -> 'devices', '[]'::jsonb),
      'items', coalesce(v_rows -> 'items', '[]'::jsonb),
      'handovers', coalesce(v_rows -> 'handovers', '[]'::jsonb),
      'alerts', coalesce(v_rows -> 'alerts', '[]'::jsonb),
      'resolutions', coalesce(v_rows -> 'resolutions', '[]'::jsonb),
      'missing_reports', coalesce(v_rows -> 'missing_reports', '[]'::jsonb)
    )
  );
end $$;

-- Only the two public calls are reachable from the phones.
revoke execute on function public._op_result(uuid, text, text, text, text, text, bigint) from public, anon, authenticated;
revoke execute on function public._reject(uuid, text, jsonb, text) from public, anon, authenticated;
revoke execute on function public._settle_handover(uuid) from public, anon, authenticated;
revoke execute on function public._apply_handover(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public._apply_device(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public._apply_resolution(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public._apply_missing_report(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public._apply_op(jsonb) from public, anon, authenticated;
revoke execute on function public.set_server_seq() from public, anon, authenticated;
revoke execute on function public.sync_push(jsonb) from public, anon;
revoke execute on function public.sync_pull(bigint) from public, anon;
grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(bigint) to authenticated;
