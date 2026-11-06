-- Device revocation (docs/sync.md A.12, C.3.3b and D.6).
--
-- The safety officer withdraws a lost or stolen phone. From then on its session is no longer
-- enrolled (it can't read, push or relay), other phones learn of it on their next pull and
-- refuse its new signatures, and the server rejects handovers it signed after the revocation.
-- The withdrawn phone finds out through device_status() and wipes its local data.

alter table public.devices add column revoked_at timestamptz;
alter table public.devices add column revoked_by uuid references public.people (id);

-- A withdrawn device no longer makes its session enrolled.
create or replace function public.is_enrolled() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from devices d join people p on p.id = d.person_id
    where d.created_by = auth.uid() and p.active and d.deleted_at is null and d.revoked_at is null
  );
$$;

create or replace function public._my_person() returns uuid
language sql stable security definer set search_path = public as $$
  select d.person_id from devices d join people p on p.id = d.person_id
  where d.created_by = auth.uid() and p.active and d.deleted_at is null and d.revoked_at is null
  order by d.received_at desc nulls last
  limit 1;
$$;
revoke execute on function public._my_person() from public, anon, authenticated;

-- The safety officer withdraws a device. Not their own current one.
create or replace function public.revoke_device(p_device_id uuid) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  me uuid := _my_person();
  d devices;
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  if (select role from people where id = me) is distinct from 'rso' then
    return jsonb_build_object('result', 'rejected', 'reason', 'only the safety officer can withdraw a phone');
  end if;
  select * into d from devices where id = p_device_id for update;
  if not found then
    return jsonb_build_object('result', 'rejected', 'reason', 'unknown device');
  end if;
  if d.created_by = auth.uid() then
    return jsonb_build_object('result', 'rejected', 'reason', 'you can''t withdraw your own phone');
  end if;
  if d.revoked_at is not null then
    return jsonb_build_object('result', 'duplicate', 'device', to_jsonb(d));
  end if;
  update devices set revoked_at = now(), revoked_by = me, version = version + 1
  where id = p_device_id returning * into d;
  return jsonb_build_object('result', 'revoked', 'device', to_jsonb(d));
end;
$$;

-- Any signed-in phone: has a device I enrolled been withdrawn? Works even when no longer enrolled.
create or replace function public.device_status() returns jsonb
language sql stable security definer set search_path = public as $$
  select coalesce((
    select jsonb_build_object('revoked', d.revoked_at is not null, 'revoked_at', d.revoked_at, 'device_id', d.id)
    from devices d where d.created_by = auth.uid() and d.deleted_at is null
    order by d.received_at desc nulls last limit 1
  ), jsonb_build_object('revoked', false, 'revoked_at', null, 'device_id', null));
$$;

revoke execute on function public.revoke_device(uuid) from public, anon;
revoke execute on function public.device_status() from public, anon;
grant execute on function public.revoke_device(uuid) to authenticated;
grant execute on function public.device_status() to authenticated;

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
  -- 3b. A device the safety officer withdrew can't sign anything after it was withdrawn
  --     (docs/sync.md C.3.3b). Records it signed before then still count.
  if exists (select 1 from devices where id in (h.from_device_id, h.to_device_id)
             and revoked_at is not null and revoked_at <= coalesce(to_timestamp(h.accepted_ms / 1000.0), now())) then
    return _reject(v_op_id, 'handovers', p, 'device revoked');
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
