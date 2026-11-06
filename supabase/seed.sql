-- Demo data (docs/spec.md section 8). All of it is dummy: no real sources, seals or NEA numbers.
-- Safe to run more than once. Signed handovers can't be seeded: make them on the phones.

insert into public.people (id, name, role, staff_no, nea_reg_no, enrol_code_hash) values
  ('a0000000-0000-4000-8000-000000000001', 'Siti Aminah',     'store_officer', 'S-1021', null,           encode(extensions.digest('418203', 'sha256'), 'hex')),
  ('a0000000-0000-4000-8000-000000000002', 'Ravi Kumar',      'radiographer',  'R-2210', 'RW-DEMO-1452', encode(extensions.digest('552917', 'sha256'), 'hex')),
  ('a0000000-0000-4000-8000-000000000003', 'Tan Wei Ming',    'radiographer',  'R-2234', 'RW-DEMO-1478', encode(extensions.digest('730164', 'sha256'), 'hex')),
  ('a0000000-0000-4000-8000-000000000004', 'Jun Lim',         'radiographer',  'R-2251', 'RW-DEMO-1503', encode(extensions.digest('286540', 'sha256'), 'hex')),
  ('a0000000-0000-4000-8000-000000000005', 'Dr Tan Mei Ling', 'rso',           'O-3001', null,           encode(extensions.digest('904372', 'sha256'), 'hex'))
on conflict (id) do nothing;

insert into public.items (id, code, name, source_serial, isotope, seal_no, store_name) values
  ('b0000000-0000-4000-8000-000000000012', 'C-12', 'Kestrel', 'IR-0457', 'Ir-192', 'S-40117', 'Tuas Store 1'),
  ('b0000000-0000-4000-8000-000000000007', 'C-07', 'Osprey',  'IR-0391', 'Ir-192', 'S-40102', 'Tuas Store 1'),
  ('b0000000-0000-4000-8000-000000000015', 'C-15', 'Heron',   'SE-0112', 'Se-75',  'S-40125', 'Tuas Store 1')
on conflict (id) do update set name = excluded.name where public.items.name is null;

-- Each camera starts with a server-made register record: in store with Siti (docs/sync.md A.2).
insert into public.handovers (id, item_id, kind, to_person_id, record_hash, status)
select r.id, r.item_id, 'register', r.to_person_id,
       encode(extensions.digest('CS1-REGISTER|' || r.id::text || '|' || r.item_id::text || '|' || r.to_person_id::text, 'sha256'), 'hex'),
       'accepted'
from (values
  ('c0000000-0000-4000-8000-000000000012'::uuid, 'b0000000-0000-4000-8000-000000000012'::uuid, 'a0000000-0000-4000-8000-000000000001'::uuid),
  ('c0000000-0000-4000-8000-000000000007'::uuid, 'b0000000-0000-4000-8000-000000000007'::uuid, 'a0000000-0000-4000-8000-000000000001'::uuid),
  ('c0000000-0000-4000-8000-000000000015'::uuid, 'b0000000-0000-4000-8000-000000000015'::uuid, 'a0000000-0000-4000-8000-000000000001'::uuid)
) as r (id, item_id, to_person_id)
on conflict (id) do nothing;

-- Authorisation to hold sealed sources (docs/spec.md section 8). Jun Lim's lapsed on 3 Nov,
-- so the demo can show an offline refusal. Store officers and the safety officer have no expiry.
update public.people set authorised_until_ms = v.until_ms
from (values
  ('a0000000-0000-4000-8000-000000000002'::uuid, (extract(epoch from timestamptz '2027-06-30 23:59:59+08') * 1000)::bigint),
  ('a0000000-0000-4000-8000-000000000003'::uuid, (extract(epoch from timestamptz '2027-06-30 23:59:59+08') * 1000)::bigint),
  ('a0000000-0000-4000-8000-000000000004'::uuid, (extract(epoch from timestamptz '2026-11-03 23:59:59+08') * 1000)::bigint)
) as v (id, until_ms)
where people.id = v.id and people.authorised_until_ms is distinct from v.until_ms;
