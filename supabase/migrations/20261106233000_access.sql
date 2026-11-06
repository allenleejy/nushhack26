-- Access (the Sudden Dev requirement): only enrolled, authorised staff can use the server.
-- The rules are in docs/sync.md: B (tables), C.0 (who may call), C.2 (enrolment) and C.3.4 (authorisation).

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

-- When a person's authorisation to hold sealed sources lapses, in epoch ms. Null: no expiry.
alter table public.people add column authorised_until_ms bigint;
grant select (authorised_until_ms) on public.people to authenticated;

-- Every enrolment try, so wrong codes can be limited. Never pulled, never readable.
create table public.enrol_attempts (
  id bigint generated always as identity primary key,
  uid uuid not null,
  staff_no text not null,
  ok boolean not null,
  at timestamptz not null default now()
);
create index enrol_attempts_uid_at on public.enrol_attempts (uid, at);
create index enrol_attempts_staff_at on public.enrol_attempts (staff_no, at);
alter table public.enrol_attempts enable row level security;
revoke all on public.enrol_attempts from anon, authenticated;

-- ---------------------------------------------------------------------------
-- Who may read: only a session that has enrolled a device for an active person
-- ---------------------------------------------------------------------------

create or replace function public.is_enrolled() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from devices d join people p on p.id = d.person_id
    where d.created_by = auth.uid() and p.active and d.deleted_at is null
  );
$$;
revoke execute on function public.is_enrolled() from public, anon;
grant execute on function public.is_enrolled() to authenticated;

alter policy "team reads people" on public.people using (public.is_enrolled());
alter policy "team reads people" on public.people rename to "enrolled staff read people";
alter policy "team reads devices" on public.devices using (public.is_enrolled());
alter policy "team reads devices" on public.devices rename to "enrolled staff read devices";
alter policy "team reads items" on public.items using (public.is_enrolled());
alter policy "team reads items" on public.items rename to "enrolled staff read items";
alter policy "team reads handovers" on public.handovers using (public.is_enrolled());
alter policy "team reads handovers" on public.handovers rename to "enrolled staff read handovers";
alter policy "team reads alerts" on public.alerts using (public.is_enrolled());
alter policy "team reads alerts" on public.alerts rename to "enrolled staff read alerts";
alter policy "team reads resolutions" on public.resolutions using (public.is_enrolled());
alter policy "team reads resolutions" on public.resolutions rename to "enrolled staff read resolutions";
alter policy "team reads missing reports" on public.missing_reports using (public.is_enrolled());
alter policy "team reads missing reports" on public.missing_reports rename to "enrolled staff read missing reports";

-- ---------------------------------------------------------------------------
-- Enrolment: staff number plus one-time code (docs/sync.md C.2)
-- ---------------------------------------------------------------------------

-- Returns {"result": "applied | duplicate | rejected | locked", "reason", "person", "device"}.
create or replace function public.enrol_device(p_staff_no text, p_code text, p_device jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_uid uuid := auth.uid();
  v_staff text := upper(trim(coalesce(p_staff_no, '')));
  per people;
  d devices;
  old devices;
begin
  if v_uid is null then
    raise exception 'not signed in';
  end if;

  if (select count(*) from enrol_attempts where not ok and at > now() - interval '10 minutes'
        and (uid = v_uid or staff_no = v_staff)) >= 5 then
    return jsonb_build_object('result', 'locked', 'reason', 'too many wrong codes');
  end if;

  select * into per from people where upper(staff_no) = v_staff and active;
  if not found or per.enrol_code_hash is null
     or per.enrol_code_hash <> encode(extensions.digest(coalesce(p_code, ''), 'sha256'), 'hex') then
    insert into enrol_attempts (uid, staff_no, ok) values (v_uid, v_staff, false);
    return jsonb_build_object('result', 'rejected', 'reason', 'wrong code');
  end if;

  d := jsonb_populate_record(null::devices, p_device);
  if d.id is null or d.public_key is null or d.key_fp is null
     or d.key_security not in ('strongbox', 'tee', 'software') then
    return jsonb_build_object('result', 'rejected', 'reason', 'malformed device');
  end if;

  select * into old from devices where id = d.id;
  if found then
    -- A retry after a dropped connection: the same device for the same person is fine.
    if old.person_id = per.id and old.public_key = d.public_key and old.created_by = v_uid then
      return jsonb_build_object('result', 'duplicate', 'reason', null,
        'person', to_jsonb(per) - 'enrol_code_hash', 'device', to_jsonb(old));
    end if;
    return jsonb_build_object('result', 'rejected', 'reason', 'device id already used');
  end if;

  d.person_id := per.id;
  d.app_version := coalesce(d.app_version, '');
  d.version := 1;
  d.server_seq := null;
  d.deleted_at := null;
  d.performed_at := coalesce(d.performed_at, now());
  d.received_at := now();
  d.created_by := v_uid;
  insert into devices select (d).*;
  insert into enrol_attempts (uid, staff_no, ok) values (v_uid, v_staff, true);

  select * into d from devices where id = d.id;
  return jsonb_build_object('result', 'applied', 'reason', null,
    'person', to_jsonb(per) - 'enrol_code_hash', 'device', to_jsonb(d));
end $$;
revoke execute on function public.enrol_device(text, text, jsonb) from public, anon;
grant execute on function public.enrol_device(text, text, jsonb) to authenticated;

-- Devices now arrive only through enrol_device, which checks the code.
create or replace function public._apply_device(v_op_id uuid, p jsonb) returns jsonb
language plpgsql security definer set search_path = public, extensions as $$
begin
  return _reject(v_op_id, 'devices', p, 'devices are added by enrol_device only');
end $$;

-- ---------------------------------------------------------------------------
-- Authorisation in the chain (docs/sync.md C.3.4): unchanged except the receiver check
-- ---------------------------------------------------------------------------

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
  v_to people;
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
  select * into v_to from people where id = h.to_person_id;
  v_to_role := v_to.role;
  if not ((h.kind = 'issue' and v_from_role = 'store_officer' and v_to_role = 'radiographer')
       or (h.kind = 'transfer' and v_from_role = 'radiographer' and v_to_role = 'radiographer')
       or (h.kind = 'return' and v_from_role = 'radiographer' and v_to_role = 'store_officer')) then
    return _reject(v_op_id, 'handovers', p, 'not authorised');
  end if;
  -- 4b. Whoever takes a source out of the store must be active and authorised when they accept.
  if h.kind in ('issue', 'transfer') then
    if not v_to.active then
      return _reject(v_op_id, 'handovers', p, 'not authorised: inactive');
    end if;
    if v_to.authorised_until_ms is not null and v_to.authorised_until_ms < coalesce(h.accepted_ms, 0) then
      return _reject(v_op_id, 'handovers', p, 'not authorised: lapsed');
    end if;
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

-- ---------------------------------------------------------------------------
-- The two sync calls: only for enrolled sessions (docs/sync.md C.0)
-- ---------------------------------------------------------------------------

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
  if not is_enrolled() then
    raise exception 'not enrolled' using errcode = '42501';
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
  if not is_enrolled() then
    raise exception 'not enrolled' using errcode = '42501';
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

revoke execute on function public._apply_device(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public._apply_handover(uuid, jsonb) from public, anon, authenticated;
revoke execute on function public.sync_push(jsonb) from public, anon;
revoke execute on function public.sync_pull(bigint) from public, anon;
grant execute on function public.sync_push(jsonb) to authenticated;
grant execute on function public.sync_pull(bigint) to authenticated;
