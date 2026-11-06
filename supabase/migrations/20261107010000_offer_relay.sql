-- Online handover relay (docs/sync.md A.10 and D.5).
--
-- When both phones are online, the server carries the two strings the QR codes carry
-- offline: the giver's signed offer (CS1O:) to the receiver, and the receiver's signed
-- acceptance (CS1A:) back to the giver. It is only a mailbox: each phone still checks every
-- signature itself, and the handover record still reaches the server through sync_push.
-- Rows are never pulled by sync_pull; they are read only through the relay_* functions.

create table public.offer_relay (
  id uuid primary key,                       -- the handover id the offer is for
  item_id uuid not null references public.items (id),
  from_person_id uuid not null references public.people (id),
  to_person_id uuid not null references public.people (id),
  offer text not null check (offer like 'CS1O:%' and length(offer) <= 2000),
  acceptance text check (acceptance is null or (acceptance like 'CS1A:%' and length(acceptance) <= 1000)),
  status text not null default 'open' check (status in ('open', 'accepted', 'declined', 'cancelled')),
  expires_at timestamptz not null,
  created_by uuid not null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index offer_relay_inbox on public.offer_relay (to_person_id, status, expires_at);
create index offer_relay_item on public.offer_relay (item_id, from_person_id, status);

alter table public.offer_relay enable row level security;
revoke all on public.offer_relay from anon, authenticated;

-- The person this session enrolled as, or null.
create or replace function public._my_person() returns uuid
language sql stable security definer set search_path = public as $$
  select d.person_id from devices d join people p on p.id = d.person_id
  where d.created_by = auth.uid() and p.active and d.deleted_at is null
  order by d.received_at desc nulls last
  limit 1;
$$;
revoke execute on function public._my_person() from public, anon, authenticated;

-- Giver: post a signed offer. A newer offer for the same item replaces my older open ones.
create or replace function public.relay_offer(
  p_id uuid, p_item_id uuid, p_to_person_id uuid, p_offer text, p_expires_ms bigint
) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  me uuid := _my_person();
  existing offer_relay;
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  if p_to_person_id = me then
    return jsonb_build_object('result', 'rejected', 'reason', 'can''t hand to yourself');
  end if;
  if not exists (select 1 from people where id = p_to_person_id and active) then
    return jsonb_build_object('result', 'rejected', 'reason', 'unknown receiver');
  end if;

  select * into existing from offer_relay where id = p_id;
  if found then
    if existing.from_person_id <> me then
      return jsonb_build_object('result', 'rejected', 'reason', 'not your offer');
    end if;
    return jsonb_build_object('result', 'duplicate', 'status', existing.status);
  end if;

  update offer_relay set status = 'cancelled', updated_at = now()
  where item_id = p_item_id and from_person_id = me and status = 'open';

  insert into offer_relay (id, item_id, from_person_id, to_person_id, offer, expires_at)
  values (p_id, p_item_id, me, p_to_person_id, p_offer, to_timestamp(p_expires_ms / 1000.0));
  return jsonb_build_object('result', 'posted');
end;
$$;

-- Receiver: open offers made out to me that haven't long expired (phone clocks may disagree,
-- so allow 30 seconds; the phone's own check, docs/sync.md A.4.5, decides).
create or replace function public.relay_inbox() returns jsonb
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := _my_person();
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  return coalesce((
    select jsonb_agg(jsonb_build_object(
      'id', r.id,
      'item_id', r.item_id,
      'from_person_id', r.from_person_id,
      'offer', r.offer,
      'expires_ms', (extract(epoch from r.expires_at) * 1000)::bigint
    ) order by r.created_at)
    from offer_relay r
    where r.to_person_id = me and r.status = 'open' and r.expires_at > now() - interval '30 seconds'
  ), '[]'::jsonb);
end;
$$;

-- Receiver: send my signed acceptance back. Repeating the same acceptance is harmless.
create or replace function public.relay_accept(p_id uuid, p_acceptance text) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  me uuid := _my_person();
  r offer_relay;
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  select * into r from offer_relay where id = p_id for update;
  if not found or r.to_person_id <> me then
    return jsonb_build_object('result', 'rejected', 'reason', 'no such offer');
  end if;
  if r.status = 'accepted' then
    return jsonb_build_object('result', case when r.acceptance = p_acceptance then 'duplicate' else 'rejected' end,
                              'reason', case when r.acceptance = p_acceptance then null else 'already accepted' end);
  end if;
  if r.status <> 'open' then
    return jsonb_build_object('result', 'rejected', 'reason', 'offer ' || r.status);
  end if;
  update offer_relay set status = 'accepted', acceptance = p_acceptance, updated_at = now() where id = p_id;
  return jsonb_build_object('result', 'accepted');
end;
$$;

-- Receiver: say no. Giver: take the offer back. Both only while it's open.
create or replace function public.relay_close(p_id uuid) returns jsonb
language plpgsql security definer set search_path = public as $$
declare
  me uuid := _my_person();
  r offer_relay;
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  select * into r from offer_relay where id = p_id for update;
  if not found or me not in (r.from_person_id, r.to_person_id) then
    return jsonb_build_object('result', 'rejected', 'reason', 'no such offer');
  end if;
  if r.status <> 'open' then
    return jsonb_build_object('result', 'unchanged', 'status', r.status);
  end if;
  update offer_relay
  set status = case when me = r.to_person_id then 'declined' else 'cancelled' end, updated_at = now()
  where id = p_id;
  return jsonb_build_object('result', 'closed', 'status', case when me = r.to_person_id then 'declined' else 'cancelled' end);
end;
$$;

-- Either side: where the offer stands, with the acceptance once there is one.
create or replace function public.relay_status(p_id uuid) returns jsonb
language plpgsql stable security definer set search_path = public as $$
declare
  me uuid := _my_person();
  r offer_relay;
begin
  if me is null then
    raise exception 'not enrolled' using errcode = '42501';
  end if;
  select * into r from offer_relay where id = p_id;
  if not found or me not in (r.from_person_id, r.to_person_id) then
    return jsonb_build_object('status', 'unknown');
  end if;
  return jsonb_build_object('status', r.status, 'acceptance', r.acceptance);
end;
$$;

revoke execute on function public.relay_offer(uuid, uuid, uuid, text, bigint) from public, anon;
revoke execute on function public.relay_inbox() from public, anon;
revoke execute on function public.relay_accept(uuid, text) from public, anon;
revoke execute on function public.relay_close(uuid) from public, anon;
revoke execute on function public.relay_status(uuid) from public, anon;
grant execute on function public.relay_offer(uuid, uuid, uuid, text, bigint) to authenticated;
grant execute on function public.relay_inbox() to authenticated;
grant execute on function public.relay_accept(uuid, text) to authenticated;
grant execute on function public.relay_close(uuid) to authenticated;
grant execute on function public.relay_status(uuid) to authenticated;
