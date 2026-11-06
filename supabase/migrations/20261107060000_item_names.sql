-- Each camera gets a name people say out loud (KESTREL for C-12), shown above its code.
-- Display only: names are never signed, and an empty name falls back to the code.
-- The items trigger bumps server_seq on update, so phones pull the names on their next sync.

alter table public.items add column if not exists name text;

update public.items set name = 'Kestrel' where code = 'C-12' and name is null;
update public.items set name = 'Osprey'  where code = 'C-07' and name is null;
update public.items set name = 'Heron'   where code = 'C-15' and name is null;
