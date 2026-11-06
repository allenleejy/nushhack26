# E. Offline sync technology: how to do it correctly on Next.js + Supabase (free) + Vercel Hobby

Research agent E. Compiled 2026-11-06. All pages were fetched on that date (call it "accessed 2026-11-06").
Tags: [fact] = official docs, specs, WebKit, MDN, caniuse. [reported] = blogs, issues, forums, third-party listings. [inference] = my own reasoning, or general knowledge I did not verify in this session.
Evidence IDs (E1, E2, ...) point to the Evidence table at the end.

---

## 1. Key findings (ranked by importance to the build)

1. **iPhone Safari has no Background Sync API.** caniuse shows iOS Safari "not supported" in every listed version (3.2 through 27.2); desktop Safari and Firefox also lack it; MDN marks it "Limited availability, not Baseline" (E9, E10). So sync can only run while the app is in the foreground: on app open, on the `online` event, on `visibilitychange`, on a timer while there are pending ops, and via a manual "Sync now" button. Plan the demo and the UX around that. [fact]
2. **Correctness should live on the server, not in a library.** Whatever client store is used, the properties a DSO judge will probe (no duplicates, no impossible state such as a key checked out twice, nothing silently lost, revoked users rejected) can only be guaranteed by an idempotent, validating server-side apply function in Postgres. PowerSync's own docs say the same: it is "server-authoritative", the default is per-field last-write-wins, operations "must be safe to receive more than once", and the backend can reject writes that violate business rules (E5). [fact]
3. **No off-the-shelf library gives "detect the conflict and let a human resolve it" for free on Supabase.** PowerSync (the only official Supabase-integrated option) gives you an ordered upload queue and a Supabase-JWT-aware sync service, but conflict policy is whatever you write in `uploadData()` and in Postgres (E3, E5). ElectricSQL has no write path at all (E14). Replicache is in maintenance mode (E12). Triplit's founder joined Supabase and Supabase "hasn't decided" how to ship offline (E13). Dexie Cloud and Instant are their own backends, not Supabase (E7, E13). [fact/reported]
4. **A hand-rolled outbox is buildable in 16 hours and is the easiest design to explain and defend.** About 300 to 500 lines: Dexie (IndexedDB wrapper) outbox, a sync loop, one Postgres `apply_op` function, a conflicts table, a review screen. [inference]
5. **Safari data-loss rule: install to the home screen and sync often.** WebKit's 2020 policy deletes all script-writable storage (IndexedDB, localStorage, SessionStorage, service-worker registrations and cache) after 7 days of Safari use without user interaction on that site. Home-screen web apps "have their own days-of-use counter" that matches actual use of the app, and WebKit says deleting their data would be a serious bug (E4). WebKit's 2023 storage post gives home-screen apps the same quota as a browser and says `persist()` is granted on heuristics such as "opened as a Home Screen Web App" (E8). [fact]
6. **Service worker for the app shell is needed and has a build-tool catch.** Serwist (`@serwist/next` + `serwist`) is the maintained route for App Router, but its getting-started guide "is meant for webpack"; Turbopack has a separate guide, and 9.4 changed to an external CLI build step (E6, E6b). Next 16 uses Turbopack by default [inference, general knowledge], so pin `next build --webpack` or follow the Turbopack guide, and test an actual offline cold start on a phone early. [fact/reported]
7. **`navigator.onLine` is explicitly unreliable.** MDN: `true` only means "connected to a network", a LAN with no internet counts as online, and MDN advises using it only as a hint (E11). Detect reachability with a real probe (`fetch('/api/ping', {cache:'no-store'})` with a timeout) and treat any failed sync request as offline. [fact + inference]
8. **PowerSync + Next.js + Safari has live rough edges.** Open issue #785: deadlocks with `OPFSCoopSyncVFS` on Next 16 + Turbopack in Chrome and Safari, avoided by `IDBBatchAtomicVFS` (E16); the troubleshooting page lists a Safari/iOS "Maximum call stack size exceeded" fix of `useWebWorker:false`, which disables multi-tab (E16). Shared web workers are "disabled by default on Android, iOS and Safari" (E3). Fine for a well-staffed team, risky for a 16-hour agent build. [fact/reported]
9. **Supabase anonymous sign-in is rate limited to 30 requests per hour per IP by default (configurable in the dashboard)** (E17). A hackathon venue shares one public IP. Create identities ahead of time or raise the limit. Also: anonymous users cannot be recovered after clearing browser data or switching device, so tie each anonymous identity to a roster entry via an enrolment code (inference). [fact + inference]
10. **Supabase free plan limits are comfortable for a demo:** 2 projects, 500 MB DB, 5 GB egress, 50,000 MAU, 200 peak realtime connections, 2M realtime messages, 1 GB file storage (E18). Session timebox and inactivity timeout are Pro-only, so forced server-side session expiry is not available on free; enforce revocation in your own tables/RLS (E17b). [fact]

---

## 2. Conflict strategy by record type

Principle (applies to everything): **every operation ends in exactly one recorded outcome: `applied`, `duplicate`, `conflict` (needs human), or `rejected` (with reason).** Nothing is dropped silently, nothing is overwritten without a trace. Physical events that already happened (a key was handed over) cannot be "rejected" out of existence, only recorded and flagged. [inference]

### Strategy notes

| Strategy | What it does | Failure mode for security records | Verdict |
|---|---|---|---|
| Last-write-wins (LWW) on whole record | Highest timestamp wins | A device with a fast or manipulated clock wins every conflict; a legitimate offline edit silently disappears; two guards each "check out" a key and one record vanishes. A write timestamped in the future wins forever (E19). | Reject for anything safety-relevant. |
| Per-field LWW | LWW per column | Fine when fields are independent (checklist item values); still wrong for related fields (state + holder) because you can get a state that never existed. PowerSync's default is exactly this (E5). | OK only for independent, low-stakes fields, and only with server-side ordering, not device clocks. |
| Field-level merge with base version | Op carries `base_version`; if server version equals base, apply; if different but field untouched, merge; if same field differs, flag | Needs per-field or per-row versioning. | **Use for checklists.** |
| CRDTs (Automerge, Yjs) | Guarantee convergence without coordination | They converge, but cannot enforce invariants ("key held by at most one person"); they merge into a state the business rules forbid. Extra bundle, memory and history growth. Appropriate for collaborative text, not for custody. PowerSync docs themselves suggest Yjs only for "collaborative edits" (E5). | Do not use for the core model. At most for free-text notes (not needed). [inference] |
| Append-only event log reconciled on server | Never edit; every action is an immutable event with a UUID | None for logging; order and totals are derived. Duplicates are prevented by the PK. | **Use for patrol logs and as the audit trail for everything.** |
| Hybrid logical clock (HLC) | (wall time, counter, node) tuple; orders events and tolerates skew better than raw timestamps | Does not detect concurrency, only orders. Does not stop a malicious clock. Papers cited assume clocks within a bound (E19). | Use for display ordering and drift flagging, not for deciding winners. [inference] |
| Vector / version clocks | Detect that two edits were concurrent | Grows with the number of devices | For this app a server-assigned integer `version` per record plus the op's `base_version` gives the same concurrency detection without a vector. [inference] |
| Server-side validation (state machine in Postgres) | `apply_op` checks `expected_state` / allowed transitions under `SELECT ... FOR UPDATE` | None if every write goes through it | **Mandatory for custody and any state machine.** |

### Mapping

| Record type | Examples | Strategy | Detail |
|---|---|---|---|
| Append-only log | Patrol checkpoints, incident notes, door-open events | Event log, no conflicts possible | Client generates `op_id` (UUID, ideally UUIDv7) as the primary key; server does `INSERT ... ON CONFLICT (op_id) DO NOTHING` and reports `duplicate`. Store both `client_ts` (claim) and `server_received_at` (trusted). Order the authoritative view by `server_received_at`, then per-device `device_seq`. |
| Field-mergeable | Shift checklists, inspection forms | One row per checklist item with `version`. Ops are `set_item(item_id, value, base_version)` | Same value from two devices: idempotent merge, no conflict. Different items: both applied. Same item, different values and `base_version` is stale: store a `conflict` row holding both claims; keep the server value in force until a supervisor picks. Completed safety items are set-once; un-completing is a new, attributed event. |
| State machine / custody | Key checkout and return, weapon or asset custody, access-card issue | Ops are transitions: `checkout(key, by, expected_state='available', base_version)` | Server validates inside `apply_op` with a row lock. First op to reach the server wins the transition; a second `checkout` of the same key becomes `conflict` ("already held by A since server time T; B claims physical possession at client time T'"), the key is marked `disputed`, and a supervisor resolves it on a review screen with both claims and the hash-chain proof. Never auto-pick by device timestamp. |
| Revoked-access ops | Anything signed by a user whose access was removed while offline | Quarantine | `rejected: access_revoked` plus a `security_events` row. The op is kept (never deleted) for review. |

Clock handling: devices keep a monotonic per-device counter (`device_seq`) and a hash chain (section 5), so ordering and gaps are provable without trusting the wall clock. The server flags `abs(client_ts - server_received_at_adjusted) > threshold` as `clock_drift` but still accepts the event. [inference]

---

## 3. Library comparison

Free-plan facts below come from the vendor pages cited. "Setup effort" is my estimate for AI coding agents working from task cards. [inference]

| Library | 2026 status | Free-plan limits | Setup effort (agents) | iPhone support | Conflict model |
|---|---|---|---|---|---|
| **PowerSync** (Cloud + Supabase) | Active; official Supabase integration guide; Open Edition (self-host) is free (E2, E3). | Free: 2 GB synced/month, 500 MB hosted, 50 peak concurrent clients, up to 2 instances; deactivated after 1 week of inactivity (the page also says 2 weeks, inconsistent). Pro from $49/mo. (E2) | Medium to high, about 4 to 8 hours including Postgres publication, sync rules/streams, JWT config, a WASM SQLite worker, and Safari debugging. | Works in Safari/iOS; default `IDBBatchAtomicVFS` has the broadest compat; `OPFSCoopSyncVFS` is recommended for Safari multi-tab; shared workers off on iOS so multi-tab is "less reliable"; known iOS call-stack error and a Next 16/Turbopack deadlock issue (E3, E16). | Server-authoritative. Ordered CRUD queue (PUT/PATCH/DELETE) drained through your `uploadData()` into Supabase. Default per-field LWW; custom resolution and business-rule rejections are your code; docs do not say how rejected writes should be surfaced (E5). |
| **ElectricSQL** | Docs moved to electric.ax (301 redirect from electric-sql.com). Read-path only (E14). A 2026 listing claiming local write support conflicts with this and is unverified (E14b). | Cloud free tier: not found. | High: you build the entire write path yourself (TanStack DB, PGlite, or custom). | Not assessed. | None built in. Guide suggests rollbacks by clearing local state or flagging rejected writes (E14). **Not a fit.** |
| **RxDB + Supabase replication plugin** | Active; plugin page documents setup (E15). Whether the plugin and OPFS storage are premium: not found on the page. | RxDB core is free; plugin pricing not found. | Medium. Needs text PK, `_modified` and `_deleted` columns, realtime publication, RLS (E15). | Page does not cover iOS; IndexedDB storage works in Safari [inference]. | RxDB sync engine with optimistic-concurrency push and a conflict handler (page says handled by the engine; model details not on the page) (E15). Soft deletes only. |
| **Dexie.js (OSS)** | Mature IndexedDB wrapper [inference, general knowledge]. | Free, MIT [inference]. | Low. A good local store for a hand-rolled outbox. | Works on iOS Safari (it is IndexedDB) [inference]. | None; it is storage only. |
| **Dexie Cloud** | Hosted service separate from Supabase; on-prem Docker option (E7). | Free: 3 production users (50,000 evaluation users), 100 MB, 10 simultaneous connections; pricing figures on the page are inconsistent (E7). | Medium, but your data would live in Dexie Cloud, not Supabase. | Not assessed. | Not assessed. **Breaks the fixed stack.** |
| **TinyBase** | Not researched this session. | not found | not found | not found | not found. No Supabase integration found. Low priority. |
| **Rocicorp Replicache** | Maintenance mode; "won't add new features"; recommends moving to Zero (E12). | Open source, free (E12). | n/a | n/a | Push/pull mutators with server rebase. **Skip: unmaintained for new work.** |
| **Rocicorp Zero** | Described as public alpha with PostgreSQL support in a secondary source; needs a long-running `zero-cache` server (E12). Offline-write behaviour: not found. | Self-host only; cannot run on Vercel Hobby. [inference] | High. | Not found. | Server mutators. **Skip.** |
| **Triplit** | Co-founder joined Supabase 2025-10-08; Supabase says it has not decided how to deliver offline functionality (E13). | not found | n/a | n/a | n/a. **Skip: direction unclear.** |
| **Instant** | Active; offline cache plus persistent write outbox, flushed in order on reconnect (E13b). | Not researched. | Low to medium. | Not found. | Server reconciles; own backend, **not Supabase**, so it breaks the fixed stack. |
| **Hand-rolled outbox** (Dexie or `idb` + Supabase RPC) | Fully under our control. | Within Supabase free limits (E18). | **Medium: 6 to 8 hours for core, reusable pattern.** | Plain IndexedDB works on iOS Safari/home-screen [inference]. | Whatever we define: idempotent ops, `base_version` checks, state-machine validation, conflicts table. |

---

## 4. Browser and PWA limits (as of 2026)

| Topic | What the sources say | Implication |
|---|---|---|
| Background Sync API | Not supported on iOS Safari or desktop Safari (caniuse, up to iOS 27.2 / Safari 27.3 listed); supported on Chrome and Chrome for Android; Firefox not supported; MDN "Limited availability, not Baseline" (E9, E10). [fact] | Do not rely on it. Foreground sync only. Optional Android-only enhancement, skip for the hackathon. |
| Safari 7-day rule | 2020 WebKit post: all script-writable storage (IndexedDB, localStorage, media keys, SessionStorage, service worker registrations and cache) deleted after 7 days of Safari use without user interaction on the site (E4). [fact] | Sites used from a Safari tab are at risk. Anything not yet synced is at risk; sync often and warn when `pending_ops` is old. |
| Home-screen apps | Same 2020 post: home-screen web apps are not part of Safari, have their own days-of-use counter that matches actual use of the app, and WebKit does not expect their first-party data to be deleted (E4). The post does not say outright they are exempt. [fact] | Make "Add to Home Screen, then enrol inside the installed app" a required onboarding step. Note: any open WebKit bug on home-screen cookie behaviour (E4b, 2020, resolution not found). |
| Quotas (Safari 17+, iOS 17) | Non-browser apps: origin quota up to 15% of disk, overall up to 20%; browser apps 60%/80%. A home-screen web app has the same quotas as a browser app. `StorageManager.estimate()` available. Handle `QuotaExceededError`. (E8) [fact] | Quota is not the constraint for a text-based security app. A third-party guide claims 50 MB (E4c); this is older and conflicts with the WebKit 2023 post. |
| `navigator.storage.persist()` | `persist()` / `persisted()` exist; WebKit grants based on heuristics "like whether the website is opened as a Home Screen Web App" (E8). Eviction is LRU and can happen "when a site has not been used for some time"; origins with persistent storage may be excluded. [fact] | Call `persist()` after first login, show the result in a diagnostics screen, log if denied. |
| Eviction anecdotes in 2026 | A March 2026 forum post and a January 2026 article repeat the 7-day purge for non-installed sites and longer retention for installed PWAs (E4d). [reported] | Consistent with WebKit's text; test on real devices. |
| Service worker in Next.js | Serwist is the maintained path: `@serwist/next` + `serwist`, `swSrc: app/sw.ts`, `swDest: public/sw.js`, `additionalPrecacheEntries` for an offline page, `fallbacks` for document requests (E6). Guide "is meant for webpack"; Turbopack has its own guide; 9.4 moved to an external `@serwist/cli` build step that precaches prerendered routes (E6, E6b). Third-party sources call Serwist the successor to next-pwa; I found no official next-pwa deprecation notice (E6). | Use Serwist, pin the bundler explicitly, and run an airplane-mode cold-start test in the first two hours. Fallback: a hand-written minimal `sw.js` with runtime caching (`/_next/static/*` cache-first, navigation network-first with cache fallback) [inference]. |
| Offline navigation in App Router | Not found in the sources I read how Serwist handles RSC (`?_rsc=`) requests offline. | Risk. Mitigation: make the operational screens client components that read from IndexedDB, keep them under one route (tab state) or use plain hard navigation, and precache every route used. Test offline. [inference] |
| Detecting online/offline | `navigator.onLine` can be true without internet; `online`/`offline` events share the limitation; MDN says use it only as a hint and handle real fetch failures (E11). [fact] | Probe `/api/ping`, track consecutive failures, show three states: Online, Offline, "Syncing (N pending)". |
| WebSockets / realtime | Not researched in sources. | iOS suspends JS and sockets when the app is backgrounded [inference]; treat Supabase Realtime as a live-view nicety and always pull on foreground/reconnect. |

---

## 5. On-device security

All of the following is design guidance. Items marked "unverified" use general knowledge I did not check against a source in this session. [inference]

1. **Encrypt the payload, not the index.** Store records in IndexedDB as `{id, type, status, updated_at, iv, ciphertext}`, keep only non-sensitive fields queryable, and put everything else in an AES-GCM ciphertext (WebCrypto `SubtleCrypto`, available in secure contexts, which Vercel provides). Unverified: Safari supports PBKDF2 + AES-GCM + non-extractable keys stored in IndexedDB.
2. **PIN-derived key with a wrapped data key.** Generate a random 256-bit data-encryption key (DEK) at enrolment. Derive a key-encryption key (KEK) from the PIN with PBKDF2-SHA-256 and a random salt, using a high iteration count (OWASP guidance was 600,000 for PBKDF2-HMAC-SHA256 at last check; unverified here). Store only `wrap(DEK, KEK)` plus salt. Changing the PIN re-wraps one key instead of re-encrypting the database. Argon2 is not in WebCrypto.
3. **Honest limits of a PIN.** A 4 to 6 digit PIN can be brute-forced offline if an attacker extracts the encrypted data, because the attempt counter lives on the device. Use at least 6 digits, add an attempt delay and a wipe after N failures (best effort), keep the DEK in memory only, auto-lock after 1 to 2 minutes in the background (via `visibilitychange`), and say plainly that the device passcode and iOS sandbox are the real boundary. Cache the minimum data (current site/shift only).
4. **Offline sign-in and session expiry.** Supabase access tokens default to 1 hour (typical 5 min to 1 hour) and refresh tokens are single-use (10-second reuse window); refreshing needs the network (E17b). So offline "sign-in" is only a local PIN unlock of the cached identity. Add an **offline lease**: at each online auth, the server returns `lease_expires_at` (for example 12 or 24 hours). After the lease expires the app locks writes until the user re-authenticates online. Expired JWTs do not block local operation; they only block sync until refresh.
5. **Access revoked while offline.** The device cannot know. At sync, `apply_op` checks `profiles.active`, `device.revoked_at`, and role/site membership at the time of sync, and also compares with the op's `client_ts` to report "revoked at X, op created at Y". Quarantine the op (never delete), write `security_events`, notify a supervisor. Supabase session timebox/inactivity timeouts are Pro-only (E17b), so revocation must be done in your own tables plus an admin sign-out/refresh-token revoke from a service-role route.
6. **Signed or hash-chained entries.** Per device, each op carries `seq`, `prev_hash`, and `hash = SHA-256(prev_hash || canonical_json(op))`. The server stores the last accepted head per device and rejects or flags gaps, reorders, or edits relative to the previously synced head. An optional upgrade: a per-device ECDSA P-256 key pair generated at enrolment, the private key non-extractable in IndexedDB, public key registered with the server, each op hash signed and verified in a Next.js route handler. Be explicit in the pitch: this is **tamper-evident, not tamper-proof**. An attacker with full control of a device could rewrite its unsynced tail before syncing; they cannot alter anything already anchored on the server.
7. **Device clock manipulation.** Treat `client_ts` as a claim. Record `server_received_at` on every op, compute drift, flag over a threshold (for example 5 minutes), and keep ordering by server receipt then `device_seq`. Reject ops whose `client_ts` is far in the future (E19 notes the same defence).
8. **Lost device.** A web app cannot be remotely wiped. Mitigations: (a) data is encrypted at rest under a PIN-wrapped key; (b) the supervisor marks the device `revoked` and its next sync is rejected; (c) lease expiry plus wipe-on-next-open; (d) anonymous identities are lost on clearing data (E17), so each device is enrolled against a roster record with an invite code and can be re-enrolled; (e) minimal local cache. State that unsynced data on a lost device is lost, and that this is a stated limit of PWAs.
9. **No secrets in the bundle.** Anonymous sign-in plus RLS means anyone can mint an identity (E17), so do not rely on "being authenticated" as authorisation. Authorise through the enrolment/roster tables and role checks in RLS and in `apply_op`; enable CAPTCHA/Turnstile if the app is public (Supabase recommends it).

---

## 6. Demo plan: making "syncs correctly" convincing

Setup: two iPhones (one Android as a backup) with the PWA added to the home screen and enrolled; a laptop on a projector showing the **audit trail and sync console** (a read-only supervisor page fed by Supabase Realtime). Pre-warm all routes online once. Pre-create identities (anonymous sign-in limit is 30 per hour per IP, E17).

On each phone a persistent status badge: Online / Offline, N pending, last synced. A hidden "Sync log" drawer shows each op with outcome.

| Scene | Steps | What it proves |
|---|---|---|
| 1. Duplicate-proof | Phone A airplane mode. Log 3 patrol checkpoints. Reconnect and toggle airplane mode on/off mid-sync. Press "Replay last batch". Supervisor view shows exactly 3 events; sync log shows "2 duplicate ignored". | Idempotency keys, ordered outbox, exactly-once effect. |
| 2. Field merge | Both phones offline on the same checklist. A ticks items 1 and 2; B ticks 3 and 4. Reconnect both. All four ticked, with the author of each. | Field-level merge, no lost updates. |
| 3. Same-field conflict | Both phones offline; both set item 5 differently (for example "Pass" and "Fail - damaged seal"). Reconnect. Neither silently wins: the item shows "Conflict" with both claims and a "Resolve" button for the supervisor; resolution is itself an audit event. | Conflict detected and surfaced for a human. |
| 4. Impossible state | Key 7 is available. Both phones offline, both "Check out key 7". Reconnect. First op to arrive applies; the second becomes `conflict: key already held by A`; key 7 is `disputed`; supervisor resolves. | Server-side validation of state machine, no double checkout. |
| 5. Revoked while offline | Admin revokes B's access; B (still offline) logs an entry. Reconnect: op quarantined `access_revoked`, visible in security events. | Revocation handling. |
| 6. Clock tamper (optional) | Set Phone A's clock back 2 days, log an entry, reconnect. Entry accepted, stamped with server time, flagged "clock drift 48h". | Time not trusted. |
| 7. Tamper evidence (optional) | Dev button that edits a pending entry in IndexedDB. Sync: server reports "hash chain mismatch at seq N". | Tamper-evident log. |

Audit page columns: op id, device, user, `client_ts`, `server_received_at`, type, record, outcome (applied/duplicate/conflict/rejected), reason, hash, chain status. Include a one-click "Export audit as CSV".

Demo risks: iPhone Wi-Fi/cellular reconnection delay (have a manual Sync now button); Wi-Fi captive portal at the venue; screen-record a good run as a fallback; test the full flow on the actual phones the night before (a Chrome DevTools offline toggle does not reproduce iOS behaviour).

---

## 7. Recommendation

**Architecture: hand-rolled, server-authoritative outbox. Do not adopt PowerSync, Electric, RxDB, Zero or Triplit for the hackathon.** PowerSync is the only credible library and would be the fallback; its value is the pull/replication side, which a small team can replace with "pull changes since cursor" for a few small tables.

1. **Client store:** Dexie (IndexedDB). Tables: `snapshot_*` (read cache of the user's site/shift), `outbox` (pending ops), `meta` (cursor, device id, chain head, lease). Payloads AES-GCM encrypted under a PIN-wrapped key.
2. **Every action is an immutable op:** `{op_id (UUID), device_id, user_id, device_seq, type, record_id, base_version, payload, client_ts, prev_hash, hash}`. The UI renders snapshot + pending ops (optimistic).
3. **Sync loop (foreground only):** triggers on app open, `online`, `visibilitychange`, timer while pending > 0, and a manual button; a `/api/ping` probe decides reachability. Send the outbox in `device_seq` order to the Postgres function `apply_ops(jsonb)` via `supabase.rpc`; then pull `changes WHERE seq > cursor` and update the snapshot.
4. **Server (Postgres, `SECURITY DEFINER`):** `ops_log(op_id PK, ...)` makes delivery idempotent; `apply_op` validates auth, device and roster status, hash-chain continuity, then applies by record type: append-only insert, versioned field merge, or state-machine transition under `FOR UPDATE`. Outcome is always `applied | duplicate | conflict | rejected(reason)`. Domain tables allow no direct client writes (RLS).
5. **Conflicts:** `conflicts` table holds both claims; supervisor review screen resolves by issuing a new, attributed op. Nothing is deleted. Audit page reads `ops_log` (append-only; no update/delete grants).
6. **PWA shell:** Serwist service worker, webpack build pinned, precache all routes plus an offline page; cache only static assets and never API or Supabase responses. `navigator.storage.persist()` after login; required "Add to Home Screen" onboarding; `navigator.onLine` is a hint only.
7. **Security:** PIN to PBKDF2 KEK wraps a random DEK; auto-lock; offline lease; device revocation; per-device hash chain (ECDSA signing is a stretch).
8. **Realtime:** Supabase Realtime only to refresh the supervisor view; the pull-on-foreground path is the source of truth.
9. **Estimated build:** schema + `apply_op`: 3 h; outbox + sync loop + hash chain: 3 h; shell/SW/PWA + offline test on phones: 2 h; conflict UI + audit page: 3 h; encryption + lock: 2 h; demo hardening: 2 h [inference].

### Replacement wording for the coding rule

Replace "no service worker, no offline caching" with:

> **Offline is a core feature, with strict limits.** (1) One service worker, generated by Serwist (`@serwist/next`, built with webpack), caches only the static app shell (JS, CSS, icons, the offline page, precached routes). Never cache API, Supabase or user-data responses in Cache Storage. (2) All offline data lives in IndexedDB through Dexie, with sensitive fields encrypted (WebCrypto AES-GCM under a PIN-wrapped key). Nothing sensitive in localStorage, sessionStorage or cookies. (3) The UI never writes directly to Supabase tables. Every change is an immutable operation with a client-generated UUID idempotency key, queued in the outbox, and applied by the server-side `apply_op` function, which is idempotent and validates state transitions. (4) Sync runs in the foreground only (on open, `online`, `visibilitychange`, timer, manual button); do not use the Background Sync API (not supported on iOS Safari). (5) Never decide online or offline from `navigator.onLine` alone; probe, and treat a failed request as offline. (6) Every offline-capable feature must declare its conflict type (append-only, field-merge, or state-machine) and ship a test for duplicate delivery and conflicting edits. Conflicts are surfaced to a human, never silently overwritten. (7) Device clocks are claims, not truth: store server receive time on every op.

---

## 8. Evidence table

All "Date" values are the date the page was fetched (2026-11-06) unless a publication date is given.

| ID | Claim | Tag | Source URL | Date |
|---|---|---|---|---|
| E1 | Supabase free: 2 projects, 500 MB DB, 5 GB egress, 50k MAU, 200 peak realtime connections, 2M realtime messages, 1 GB storage. Pause-after-7-days-inactivity not on this page. | fact | https://supabase.com/docs/guides/platform/billing-on-supabase | 2026-11-06 |
| E1b | Supabase free projects pause after 7 days of inactivity | reported | https://www.jetadmin.io/blog/supabase-pricing-2026-guide-to-plans-limits-and-real-world-costs/ (search result summary) | 2026-11-06 |
| E2 | PowerSync Cloud Free: 2 GB synced/month, 500 MB hosted, 50 peak concurrent clients, 2 instances; deactivated after 1 week inactivity (feature table also says 2 weeks); Pro from $49/mo; Open Edition self-host free | fact | https://www.powersync.com/pricing | 2026-11-06 |
| E3 | PowerSync + Supabase: trusts Supabase Auth JWT; local writes go to SQLite and an upload queue drained by `uploadData()` through the Supabase client; web SDK: default `IDBBatchAtomicVFS`, `OPFSCoopSyncVFS` recommended for Safari, shared workers disabled by default on Android/iOS/Safari, `useWebWorker` can be disabled; SSR mode returns empty results | fact | https://docs.powersync.com/integration-guides/supabase-+-powersync ; https://docs.powersync.com/client-sdks/reference/javascript-web | 2026-11-06 |
| E5 | PowerSync conflict handling: server-authoritative; simplest is per-field LWW; ops PUT/PATCH/DELETE in ordered queue; deletes win; ops must be idempotent (dedupe by per-client op id); server may reject writes on business rules; CRDTs (Yjs) as an alternative | fact | https://docs.powersync.com/handling-writes/handling-update-conflicts | 2026-11-06 |
| E16 | PowerSync issue #785: deadlocks with OPFSCoopSyncVFS on Next.js 16 + Turbopack in Chrome and Safari, avoided with IDBBatchAtomicVFS; troubleshooting: Safari/iOS "Maximum call stack size exceeded" fixed by `useWebWorker:false` (disables multi-tab). Status/resolution not checked. | reported | https://github.com/powersync-ja/powersync-js/issues/785 ; https://docs.powersync.com/debugging/troubleshooting (via search summary) | 2026-11-06 |
| E14 | Electric provides read-path sync only; no built-in write path or offline queue; write patterns: online writes, optimistic state, shared persistent optimistic state, through-the-database (PGlite); docs moved from electric-sql.com to electric.ax | fact | https://electric.ax/docs/guides/writes (301 from https://electric-sql.com/docs/guides/writes) | 2026-11-06 |
| E14b | A listing dated Oct 2026 describes Electric as offering local writes; conflicts with E14, unresolved | reported | https://toolradar.com/tools/electric-sql (search summary) | 2026-11-06 |
| E15 | RxDB Supabase replication: text primary key, `_modified` and `_deleted` columns, soft deletes, realtime publication, RLS; sync engine handles conflicts with optimistic-concurrency push. Premium status not stated. | fact | https://rxdb.info/replication-supabase.html | 2026-11-06 |
| E7 | Dexie Cloud: hosted service, free plan 3 production users + 50k evaluation users, 100 MB, 10 simultaneous connections; on-prem Docker option; no mention of Supabase/Postgres | fact | https://dexie.org/cloud/pricing | 2026-11-06 |
| E12 | Replicache is in maintenance mode, no new features, migrate to Zero; Zero builds on Replicache internally and was described as public alpha (secondary source, may be dated) | reported | https://replicache.dev/ ; https://forums.basehub.com/rocicorp/mono/1 (search summaries) | 2026-11-06 |
| E13 | Triplit co-founder joined Supabase on 2025-10-08; Supabase has not decided how to deliver offline functionality | fact (vendor blog) | https://supabase.com/blog/triplit-joins-supabase | 2025-10-08 |
| E13b | Instant: offline caching and persistent write outbox flushed in order on reconnect (vendor description, own backend) | reported | https://www.instantdb.com/about | 2026-11-06 |
| E4 | WebKit: 7-day cap deletes all script-writable storage (IndexedDB, localStorage, media keys, SessionStorage, service worker registrations/cache) after 7 days of Safari use without interaction; home-screen web apps have their own days-of-use counter; deletion there "would be a serious bug" | fact | https://webkit.org/blog/10218/full-third-party-cookie-blocking-and-more/ | 2020-03-24 |
| E4b | WebKit bug 211775: cookies deleted in a home-screen app while localStorage persisted; resolution not found | reported | https://lists.webkit.org/pipermail/webkit-unassigned/2020-May/924322.html | 2020-05 |
| E4c | A guide claims a 50 MB iOS PWA quota, no persistence guarantee | reported | https://www.magicbell.com/blog/pwa-ios-limitations-safari-support-complete-guide | 2026-11-06 |
| E4d | Jan and Mar 2026 posts: data purged after 7 days for non-installed sites; installed PWAs kept data longer (anecdotal) | reported | https://habr.com/en/articles/989862 ; https://reddit.sentinel-team.org/posts/1rpp4oh/snapshots/2026-03-10T20%3A04%3A16.679163Z | 2026-01 / 2026-03 |
| E8 | WebKit Safari 17 storage policy: non-browser apps get 15% origin/20% overall quota, browser apps 60%/80%; home-screen web app has the same quotas as a browser app; `persist()` granted on heuristics such as being a Home Screen Web App; LRU eviction. Post does not mention the 7-day cap. | fact | https://webkit.org/blog/14403/updates-to-storage-policy/ | 2023 (date not shown in fetch; refers to Safari 17.0) |
| E9 | Background Sync: iOS Safari and desktop Safari not supported in any listed version; Firefox not supported; Chrome and Chrome Android supported | fact | https://caniuse.com/background-sync | 2026-11-06 |
| E10 | MDN: Background Synchronization API is "Limited availability", not Baseline | fact | https://developer.mozilla.org/en-US/docs/Web/API/Background_Synchronization_API | 2026-11-06 |
| E11 | MDN: `navigator.onLine` is "inherently unreliable"; true can mean LAN-only; events share this; use only as a hint | fact | https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine | 2026-11-06 |
| E6 | Serwist for Next.js App Router: `@serwist/next` + `serwist`, `swSrc app/sw.ts`, `swDest public/sw.js`, `additionalPrecacheEntries` and `fallbacks` for offline page; guide targets webpack, Turbopack has a separate guide | fact | https://serwist.pages.dev/docs/next/getting-started ; https://serwist.pages.dev/docs/next/turbo | 2026-11-06 |
| E6b | Serwist 9.4 runs as external build step via `@serwist/cli`; prerendered routes precached automatically; Serwist described as successor to next-pwa; no official next-pwa deprecation notice found | reported | https://serwist.pages.dev/docs/next/config (search summary) ; https://skills.cat/skills/jakerains/agentskills/nextjs-pwa | 2026-11-06 |
| E17 | Supabase anonymous sign-in: `authenticated` role; JWT claim `is_anonymous`; IP rate limit 30 requests/hour (configurable); CAPTCHA recommended; no auto cleanup; loses access after clearing data or switching device; Next.js static rendering caveat | fact | https://supabase.com/docs/guides/auth/auth-anonymous | 2026-11-06 |
| E17b | Supabase sessions: access token default 1 hour; refresh tokens single-use with 10 s reuse window; timebox and inactivity timeout are Pro-only | fact | https://supabase.com/docs/guides/auth/sessions | 2026-11-06 |
| E19 | HLC pairs wall clock with a logical counter; does not remove skew; LWW danger is a fast clock winning every conflict; some systems reject far-future timestamps; no source found on HLC behaviour for offline reconnect | reported | https://ar5iv.arxiv.org/html/1612.05205 ; https://www.mintlify.com/estebanrfp/gdb/advanced/hybrid-logical-clock (search summaries) | 2026-11-06 |

---

## 9. Open questions and not found

- **Is Safari data persistence for home-screen apps still as WebKit described in 2020?** The 2023 storage post is silent on the 7-day cap (E8). I found no Safari 26/27 release note or bug confirming or changing it. Needs a real-device check, ideally leaving a test PWA with data for more than 7 days of use before the event (not possible now) or at least reading WebKit release notes.
- **Do iOS home-screen web apps use a data container separate from Safari?** I believe so from general knowledge (so enrol inside the installed app, not a Safari tab), but no fetched source confirmed it. Test on device.
- **RSC / App Router offline navigation with Serwist:** no source found; plan an early device test. Which bundler Next 16 uses by default and exactly how `next build --webpack` interacts with Serwist 9.4's CLI step was not verified.
- **PowerSync issue #785 resolution status** and the inactivity rule (1 vs 2 weeks) for free instances: not resolved.
- **RxDB Supabase plugin and OPFS storage: open core or premium?** Not found.
- **TinyBase, Zero offline writes, Instant free limits and Safari behaviour, Electric Cloud free tier:** not found / not researched.
- **Dexie Cloud conflict model:** not fetched.
- **WebCrypto details on Safari** (PBKDF2 iteration cost on an older iPhone, non-extractable key persistence in IndexedDB), and OWASP's current PBKDF2 iteration figure: unverified, benchmark on the target phones.
- **Realtime behaviour when the PWA is backgrounded on iOS:** no source read; assume sockets are dropped.
- **Hash-chain strength:** a chain proves internal consistency and that history was not edited after the server saw it; it does not prove an unsynced tail was not rewritten. Do not oversell this in front of the DSO judge.
- Web search results were US-only and several were secondary summaries; items tagged [reported] should be double-checked before being quoted to the judge.
