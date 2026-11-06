# Sync and signed records

The exact formats and rules behind `docs/spec.md`. Card 01 (token core) implements A.1–A.6, card 02 (sync engine) implements §D and §E, the build account implements A.7–A.9 in `auth/` and §B and §C in the migrations (`custody_schema`, then `access`). If code and this file disagree, fix one of them in the same pull request.

---

## A. Signed records

### A.1 Keys

- **One key per phone:** EC P-256 in the Android Keystore, alias `countersign-device-key`, purpose SIGN, digest SHA-256.
- **Locked behind the user:**
  - API 30+: `setUserAuthenticationParameters(10, AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)`
  - API 26–29: `setUserAuthenticationRequired(true)` plus `setUserAuthenticationValidityDurationSeconds(10)`
- **Before each signature:** show `BiometricPrompt` (`ui/components/SignPrompt.kt`), then sign within 10 seconds.
  - Allowed authenticators: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` on API 30+, and `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` below 30. Check the current androidx.biometric docs.
  - A wrong finger is refused by the system, so nothing is signed.
- **Hardware:** try `setIsStrongBoxBacked(true)` first. On `StrongBoxUnavailableException`, create it without StrongBox.
  - `key_security` records the result: `strongbox`, `tee`, or `software`. Use `KeyInfo.securityLevel` on API 31+, and `isInsideSecureHardware` below.
- **Debug builds only:** if the phone has no secure lock screen (Devin's emulator), use a software EC P-256 key stored in app files, with `key_security = software`. Me shows "Unprotected test key". Release builds refuse to enrol without a lock screen.
- **Public key:** the X.509 SubjectPublicKeyInfo DER, base64url without padding, in `devices.public_key`.
- **Key fingerprint `key_fp`:** the first 4 bytes of SHA-256(SPKI DER), as uppercase hex pairs joined by `:`, for example `3F:A9:12:7C`.
- **The same key unlocks the app** (A.7). There's no separate password or login key.

### A.2 Canonical strings

These are UTF-8 strings with fields joined by `|`. No field ever contains `|`:
- ids are lowercase UUIDs with hyphens
- times are epoch milliseconds in decimal
- hashes are lowercase hex
- signatures are base64url without padding
- a missing value is the empty string

**The offer** (the giver signs this):
```
CS1-OFFER|id|item_id|kind|prev_id|prev_hash|from_person_id|from_device_id|to_person_id|due_ms|nonce|offered_ms|expires_ms
```

**The acceptance** (the receiver signs this):
```
CS1-ACCEPT|<the whole offer string>|giver_sig|to_device_id|seal_no|accepted_ms
```

**The record hash:**
```
record_hash = hex(SHA-256(<acceptance string> + "|" + receiver_sig))
```

**The register record** (made by the server, never signed):
```
record_hash = hex(SHA-256("CS1-REGISTER|" + id + "|" + item_id + "|" + to_person_id))
```

**A resolution** (the safety officer signs this):
```
CS1-RESOLVE|id|alert_id|item_id|chosen_handover_id|decided_by_person_id|decided_by_device_id|hex(SHA-256(note))|decided_ms
```

**An unlock challenge** (the phone signs it and checks it itself; it never leaves the phone):
```
CS1-UNLOCK|device_id|person_id|nonce|unlock_ms
```

**The values:**
- `kind` is `issue`, `transfer` or `return`.
- `nonce` is 16 bytes from `SecureRandom`, base64url (22 characters).
- `expires_ms = offered_ms + 60000`.
- `due_ms` is set on issue, copied unchanged on transfer, and empty on return.
- Signatures are `SHA256withECDSA` over the string's UTF-8 bytes: ASN.1 DER, base64url.

**The match code:**
1. Take the first 4 bytes of `record_hash` as an unsigned big-endian number.
2. Take it modulo 1,000,000 and pad it to 6 digits.
3. Show it as `48 21 07`.

### A.3 QR payloads

QR codes use byte mode with error correction L. The screens that show a QR keep the screen on at full brightness.

| Code | Format | Shown by |
|---|---|---|
| Offer | `CS1O:` + JSON | The giver (G3) |
| Acceptance | `CS1A:` + JSON | The receiver (R5) |
| Seal | `CS1S:<item code>:<seal no>`, for example `CS1S:C-12:S-40117` | The printed sticker |

- **The offer JSON** holds all the offer fields and `giver_sig`, plus `p`: the **proof record**.
  - The proof record is the whole handover whose id is `prev_id`, the one that made the giver the holder.
  - `p` is null when `prev_id` is a register record, because every phone gets register records from the server.
- **The acceptance JSON** holds `id`, `to_device_id`, `seal_no`, `accepted_ms` and `receiver_sig`.
- **Size target:** at most 1,100 characters for an offer with a proof record.
- **Ways to shrink it, all reversible:**
  - short JSON keys
  - UUIDs as base64url of their 16 bytes
  - leave out any hash the receiver can recompute (`prev_hash`, and the proof's `record_hash`)

  The canonical strings in A.2 always use the long forms.

### A.4 The receiver's checks (R2), in this order

Each check gives one line, ✓ or ✗, with the text from spec R2. The offer is refused if any check fails, and refused offers are never stored.

1. **Readable:** it starts with `CS1O:` and parses. Otherwise: "Not a handover code".
2. **Signed by the giver:** `from_device_id` is on this phone's roster (`devices`), belongs to `from_person_id`, and `giver_sig` verifies over the offer string. Otherwise: "Unknown signer. Sync this phone before accepting." or "Signature doesn't match".
3. **The giver holds it:** checked against the proof record `p`, or the local register record when `p` is null. The proof record must:
   - recompute to the offer's `prev_hash`
   - have both its signatures verify against this phone's roster
   - be for the same item
   - have `to_person_id` equal to the giver
   - if this phone already has the proof's own previous record, link to it
4. **Made out to you:** `to_person_id` is this phone's person.
5. **Valid:**
   - It passes if `offered_ms − 15000 ≤ now ≤ expires_ms + 15000`. The 15 seconds allow for phone clocks that disagree.
   - Show "Valid for m:ss" or "Expired m:ss ago", measured from `expires_ms`.
6. **Not used before:** no local handover has this `nonce` or this `id`.

**The seal check (R3):**
- The scanned code's item code must equal the item's `code`, and its seal number must equal the local `items.seal_no`.
- On a mismatch, the receiver can't go on.

**On Accept (R4)**, in one Room transaction:
1. Save the proof record `p` with origin `received_in_offer` and an outbox insert (op id = its id; a duplicate is harmless).
2. Save the complete new handover with origin `mine`, `sync_status = waiting`, and an outbox insert.

Then show the acceptance QR.

**The giver's checks on scanning the acceptance (G4):**
- The `id` matches the pending offer.
- `to_device_id` is on the roster and belongs to `to_person_id`.
- `receiver_sig` verifies over the acceptance string.

If they pass, in one transaction: save the complete handover (origin `mine`), insert it into the outbox, and delete the pending offer. A giver may scan an acceptance after the offer expired: the receiver's own check (A.4.5) already limited when they could accept.

**The authorisation line (R2, line 6)** comes from `AccessRules.checkReceiver` (A.8), not from `OfferChecker`. The receive screen shows it after the five offer lines, and a ✗ refuses the offer like any other check (header "This handover can't be accepted.").

### A.5 The tampered phone (demo only)

When `sync_state` has `tampered = true` and this phone completes a handover **as the giver**, in the same transaction:
- set `hidden_by_tamper = true` on that handover
- delete its outbox entry

The phone then still computes itself as the holder and can sign a new offer from the old previous record. Its receivers' phones still deliver those records.

### A.6 Who holds a camera: `deriveCustody`

This is a pure function in `crypto/Custody.kt`, used by every screen and by the tests.

**Inputs:** the item, its handovers, its resolutions, the current time, and a mode:
- `local`: everything on this phone
- `serverView`: only records with a `server_seq`, used by the safety officer's overdue card

**Leave out** any handover with `hidden_by_tamper`, with `sync_status = rejected`, or with server status `overruled` or `orphan`.

**The walk:**
1. Start at the item's register record. If it's missing, the result is `Unknown` ("Sync once").
2. At each record, look at its children: the records whose `prev_id` is this record.
   - **None:** this record is the head.
   - **One:** move to it.
   - **Two or more:** this is a fork.
     - If a resolution's `chosen_handover_id` is one of these children, or a descendant of one, follow that child.
     - Otherwise stop with status **Disputed**. The result lists the branches and the giver they share.

**The status:**

| Head record's kind | Status |
|---|---|
| `register` or `return` | **In store** |
| `issue` or `transfer` | **Out** |
| `issue` or `transfer`, with `due_ms < now` | **Overdue** |

**It returns:**
- the status and the holder (the head's `to_person_id`, except when Disputed)
- the head record, `due_ms`, and the time of the last signature (the head's `accepted_ms`)
- whether every record on the path has synced
- the fork branches, if there are any

### A.7 Unlocking the app offline (`auth/Unlocker.kt`, `auth/AppLock.kt`)

- **When it locks:** once the phone is enrolled, on a cold start and whenever the screen turns off (`ACTION_SCREEN_OFF`). Debug builds on a phone with no secure lock screen never lock, because they sign with the software key and there's nothing to authenticate with.
- **Unlocking:**
  1. Show `SignPrompt` ("Unlock Tenet", the person's name).
  2. Build `CS1-UNLOCK|device_id|person_id|nonce|unlock_ms`, with a fresh 16-byte nonce.
  3. Sign it with `DeviceKeys.sign`. The Keystore refuses unless the person has just authenticated.
  4. Verify the signature with `Signatures.verify` against this phone's enrolled public key, kept in `sync_state` (`device_public_key`), so the check works offline and after "Clear local data".
  5. The app unlocks only if the Keystore key's public key equals the enrolled one and the signature verifies against it.
- **What this proves:** the enrolled person's fingerprint or PIN opened this phone's hardware key just now, and it's the key the server accepted at enrolment. No network is needed.

### A.8 Who may do what (`auth/AccessRules.kt`, pure, used offline and mirrored on the server)

| Kind | Giver's role | Receiver's role | The receiver must also be |
|---|---|---|---|
| `issue` | store officer | radiographer | active, with `authorised_until_ms` empty or ≥ the time |
| `transfer` | radiographer | radiographer | active, with `authorised_until_ms` empty or ≥ the time |
| `return` | radiographer | store officer | active |

- **On the phone:**
  - G1 greys out receivers that fail, with the reason ("Jun Lim · authorisation lapsed 3 Nov").
  - R2 line 6 checks this phone's own person as the receiver, at the current time.
  - Only a safety officer sees Sign decision.
- **On the server:** `sync_push` rules C.3.4 and C.3.4b, and C.4, check the same rules, using `accepted_ms` as the time.
- **Texts:**
  - pass: "Authorised · radiographer · valid to 30 Jun 2027" (no expiry: "Authorised · radiographer"; a return: "Authorised · store officer")
  - lapsed, on the receiver's phone: "You aren't authorised to hold sealed sources (lapsed 3 Nov)"
  - lapsed, in G1: "Jun Lim · authorisation lapsed 3 Nov"
  - inactive: "Jun Lim is no longer on the active roster"
  - wrong role: "A radiographer can't issue cameras" (or the matching sentence for that kind)

### A.9 The offline access lease

- **Its start** is the later of `sync_state.last_sync_at` (written after every successful sync, which the server allows only for an enrolled session) and `sync_state.enrolled_at`.
- **It lasts 72 hours.** Until then, everything works offline.
- **After that,** the app still unlocks and shows every record, but Hand over, Receive and Sign decision are disabled with "Connect once to confirm your access. Until then you can look but not sign." The next successful sync renews it.
- "Clear local data" keeps `last_sync_at`, `enrolled_at` and `device_public_key`.

### A.10 The online handover (`RelayRepository`, `offer_relay`)

When both phones are online, nobody scans a QR code. The server carries the same two strings instead, the giver's `CS1O:` offer and the receiver's `CS1A:` acceptance, and both people only touch their fingerprint. The receiver still scans the seal on the camera. That's what proves the two people and the camera are together.

**What doesn't change:**
- the signed records (A.2), the receiver's checks (A.4), the seal check, and the giver's checks on the acceptance
- the Room saves (`savePendingOffer`, `saveAccepted`, `completeHandover`) and their outbox entries
- the record still reaches `handovers` only through `sync_push`, and the server checks it there (§C)

The relay is only a mailbox: the server never signs or checks anything in it. A string read from the relay goes through `QrPayloads.parse` exactly like a scanned one.

**Steps:**
1. **Giver, after G2 signs:**
   - if `isOnline`, post with `container.offerRelay.post(id, item_id, to_person_id, QrPayloads.offer(record, proof), expires_ms)`
   - a relayed offer is valid for 3 minutes: `expires_ms = offered_ms + RelayRepository.RELAYED_OFFER_VALID_MS`. A QR offer stays at 60 s.
   - the expiry is chosen before signing, because it's part of the signed string, so the giver decides online or QR at G2 from `isOnline`
   - on `Sent`, show G3-online and check `status(id)` every 2 s. Also watch the local `handovers` for the record id, since the receiver's record can arrive by sync first.
2. **Receiver:**
   - the app frame collects `offerRelay.incoming`, which calls `relay_inbox` every 2 s while it's on screen, online and enrolled
   - a banner (R0) shows each offer made out to me. Tapping it opens `ReceiveRelayedRoute(offerId)`, which starts at R2 with `QrPayloads.parse(offer)`.
   - from there it's the same checks (including line 6, authorisation), then R3 seal, then R4 fingerprint and `saveAccepted`
   - then `offerRelay.accept(id, QrPayloads.acceptance(record))`. On `Sent`, R5-online shows "Sent to <giver>" with no QR.
3. **Giver, when `status` is `accepted`:**
   - `QrPayloads.parse(acceptance)`, then the same giver checks, then `completeHandover(record)`, then G5
   - if the record arrived by sync first, delete the pending offer and go to G5 with that record
4. **Falling back.** Every relay call gives up after 6 s and returns `Unavailable` or `Refused`; then that step uses the QR path:
   - **the giver can't post:** show the normal G3 QR. If the offer was signed for 3 minutes, the QR simply shows that longer countdown.
   - **the giver while waiting:** G3-online always has **Show QR instead**, which shows the same offer as a QR, and **Scan acceptance**
   - **the receiver can't send the acceptance:** show the normal R5 acceptance QR, and the giver scans it (G4)
5. **Declined or cancelled:**
   - **Decline** on R2–R4 calls `close(id)`; the giver's status turns `declined` and G3-online says "<name> declined. Nothing was handed over."
   - the giver's **Cancel offer** calls `close(id)` and deletes the pending offer
   - an offer nobody answers simply lapses at `expires_ms`
6. **Security:**
   - `offer_relay` has RLS on with every grant revoked; only the `relay_*` functions touch it
   - only the giver and the named receiver can read a row, and only an enrolled session can call them
   - a stolen string is as useless as a photographed QR: it's made out to one person, single use, short-lived, and still needs the seal and the receiver's own key

---

## B. Tables

Every table has, as the kit requires:
- `version int default 1`
- `server_seq bigint` (one shared sequence, set by a trigger on every insert and update)
- `deleted_at timestamptz null` (always null here)
- `performed_at timestamptz`
- `received_at timestamptz default now()`
- `created_by uuid` (`auth.uid()` of the pushing phone)

| Table | Kind | Columns besides the standard ones |
|---|---|---|
| `people` | Read-only, seed | `id uuid PK`, `name text`, `role text` (`store_officer`, `radiographer` or `rso`), `staff_no text`, `nea_reg_no text null`, `enrol_code_hash text null` (hex SHA-256 of the code; never readable or pulled), `active boolean default true`, `authorised_until_ms bigint null` (when the person's authorisation to hold sealed sources lapses; null means no expiry) |
| `devices` | Append-only, written only by `enrol_device` | `id uuid PK` (made on the phone), `person_id uuid FK people` (set by the server from the staff number), `public_key text`, `key_fp text`, `key_security text` (`strongbox`, `tee` or `software`), `app_version text`. Its `created_by` is the enrolled session. |
| `items` | Read-only, seed | `id uuid PK`, `code text unique`, `source_serial text`, `isotope text`, `seal_no text`, `store_name text` |
| `handovers` | Append-only, with clash rules | See below |
| `alerts` | Written by the server only | `id uuid PK default gen_random_uuid()`, `kind text` (`fork`, `seal_mismatch`, `orphan` or `rejected`), `item_id uuid null FK items`, `person_id uuid null FK people` (the person named), `handover_ids uuid[] default '{}'`, `fork_prev_id uuid null` (for a fork, the record both branches follow), `reason text`, `op_table text null`, `op_payload jsonb null`, `resolution_id uuid null`, `closed_at timestamptz null` |
| `resolutions` | Append-only, with a clash rule | `id uuid PK`, `alert_id uuid FK alerts`, `item_id uuid FK items`, `chosen_handover_id uuid FK handovers`, `decided_by_person_id uuid FK people`, `decided_by_device_id uuid FK devices`, `note text default ''`, `decided_ms bigint`, `sig text` |
| `missing_reports` | Append-only | `id uuid PK`, `item_id uuid FK items`, `reported_by_person_id uuid FK people`, `reported_by_device_id uuid FK devices`, `last_handover_id uuid null`, `note text`, `reported_ms bigint` |
| `enrol_attempts` | Server only, never pulled | `id bigint identity PK`, `uid uuid`, `staff_no text`, `ok boolean`, `at timestamptz default now()` |
| `applied_ops` | Server only, never pulled | `op_id uuid PK`, `table_name text`, `result text`, `reason text null`, `status text null`, `record_hash text null`, `applied_at timestamptz default now()` |

**The `handovers` columns:**
- **Signed:**
  - `id uuid PK`, `item_id uuid FK items`, `kind text` (`register`, `issue`, `transfer` or `return`)
  - `prev_id uuid null` (indexed, with no foreign key, so an orphan can be stored), `prev_hash text null`
  - `from_person_id uuid null FK people`, `from_device_id uuid null FK devices`
  - `to_person_id uuid FK people`, `to_device_id uuid null FK devices`
  - `seal_no text null`, `due_ms bigint null`, `nonce text null unique`
  - `offered_ms bigint null`, `expires_ms bigint null`, `accepted_ms bigint null`
  - `giver_sig text null`, `receiver_sig text null`, `record_hash text unique`
- **Set by the server:**
  - `status text default 'accepted'` (`accepted`, `forked`, `overruled` or `orphan`)
  - `status_reason text null`
  - `performed_at = to_timestamp(accepted_ms / 1000.0)`
- **Indexes:** `(item_id, server_seq)`, `prev_id`.

**Phone-only columns:**
- on `handovers`:
  - `sync_status` (`waiting`, `synced` or `rejected`)
  - `sync_reason`
  - `origin` (`mine`, `received_in_offer` or `pulled`)
  - `hidden_by_tamper boolean`
- on `resolutions` and `missing_reports`: `sync_status`
- **Phone-only tables:**
  - `pending_offers`: `id`, `item_id`, `to_person_id`, the offer JSON, `expires_ms`, `created_at`
  - `outbox` and `sync_state`, from the kickoff

**RLS:**
- `select` is allowed to `authenticated` only when `is_enrolled()`: the session's `auth.uid()` is the `created_by` of a device whose person is active. That covers every table except `applied_ops` and `enrol_attempts`, which nobody can read.
- There are no insert, update or delete policies. All writes go through `sync_push` or `enrol_device` (both `security definer`).

---

## C. Server rules in `sync_push`

### C.0 Who may call

- `sync_push` and `sync_pull` raise `not enrolled` (SQLSTATE 42501) unless `is_enrolled()`. The phone treats it like a network error (§E).
- `enrol_device` needs only a signed-in session (D.4).

All of one call's ops run in **one transaction**. For each op, in order:

### C.1 Every op
- If `applied_ops` already has this `op_id`, return its stored result as `duplicate`.
- **Exception:** for `handovers`, if the payload's `record_hash` differs from the stored one, return `rejected` ("conflicting copy of the same record") and add a `rejected` alert.
- Ignore server-owned fields in any payload: `status`, `server_seq`, `received_at`, `created_by`, `performed_at`.

### C.2 `devices`
- A `devices` op in `sync_push` is always rejected: "devices are added by enrol_device only".
- **`enrol_device(p_staff_no, p_code, p_device)`** (D.4), in order:
  1. **Too many tries:** five failed tries in the last 10 minutes for this session or this staff number. Result: `locked`, "too many wrong codes".
  2. **Wrong code:** the staff number (trimmed, any case) isn't an active person, or SHA-256 of the code isn't their `enrol_code_hash`. The try is logged as failed. Result: `rejected`, "wrong code". The code is never stored.
  3. **Malformed device:** no `id`, `public_key` or `key_fp`, or a bad `key_security`. Result: `rejected`, "malformed device".
  4. **A retry:** the device id already exists for the same person, key and session. Result: `duplicate`. The same id in any other case is `rejected`, "device id already used".
  5. Otherwise the server inserts the device with `person_id` from the staff number and `created_by = auth.uid()`, and logs the try as successful. Result: `applied`.
- A person may enrol several devices. Enrolment doesn't check `authorised_until_ms`: a person whose authorisation lapsed can still sign in and look, but can't receive a source (C.3.4b).

### C.3 `handovers` (lock the `items` row with `FOR UPDATE` first)
1. **Hash:** recompute `record_hash` from the fields (A.2, using `pgcrypto`'s `digest`). If it doesn't match: rejected, "hash mismatch".
2. **One-time number:** another record already has this `nonce`. Rejected: "replayed offer".
3. **Roster:** `from_device_id` must belong to `from_person_id`, and `to_device_id` to `to_person_id`. Otherwise: rejected, "unknown signer".
4. **Roles:** the kind must fit the roles. Otherwise: rejected, "not authorised".
   - `issue`: from a store officer to a radiographer
   - `transfer`: from a radiographer to a radiographer
   - `return`: from a radiographer to a store officer
   - **4b.** On `issue` and `transfer`, the receiver must be active ("not authorised: inactive") and have `authorised_until_ms` null or ≥ `accepted_ms` ("not authorised: lapsed"). Both are rejected.
5. **Previous record:**
   - not found: store it with status `orphan` and add an `orphan` alert ("waiting for an earlier record"). Result: `conflict`.
   - its `to_person_id` isn't `from_person_id`: rejected, "giver was not the holder".
   - its `record_hash` isn't `prev_hash`: rejected, "broken link".
6. **A branch that's already disputed:** if the previous record's status is `forked` or `overruled`, store this one as `forked` and add its id to that item's open fork alert. Result: `conflict`.
7. **Fork:** if the previous record already has a child with status `accepted`, store this one as `forked`.
   - Create a `fork` alert, or add to the open one for that previous record, with:
     - `person_id = from_person_id`
     - `handover_ids` = both children
     - reason "<giver> signed two handovers of <item> after <previous id>"
   - Result: `conflict`.
8. **Seal:** if `seal_no` isn't `items.seal_no`, store it as `accepted` with `status_reason = 'seal mismatch'` and add a `seal_mismatch` alert. Result: `applied`.
9. Otherwise store it as `accepted`. Result: `applied`.
10. **Orphans:** after any record is stored, re-run rules 3–9 on every orphan whose `prev_id` is this record, repeating as needed. Close each orphan alert once its record is resolved.

Every **rejected** op writes an `alerts` row (`rejected`, with `op_table`, `op_payload` and the reason) and an `applied_ops` row. Nothing is dropped silently.

### C.4 `resolutions`
- `decided_by_person_id` must have role `rso`. Otherwise: rejected, "not a safety officer".
- The alert must be an open `fork`, and `chosen_handover_id` must be in its `handover_ids`. Otherwise: rejected, "already resolved" or "not part of this fork".
- **Applying it:**
  1. Set the chosen child and every descendant of it to `accepted`.
  2. Set every other child of the fork's previous record, and their descendants, to `overruled`.
  3. Set `alerts.resolution_id` and `closed_at`.

  The status updates bump `server_seq`, so phones pull them. Result: `applied`.

### C.5 `missing_reports`
- Always applied.

---

## D. Push and pull

### D.1 Push

The phone calls `sync_push(ops jsonb)` with up to 100 outbox entries, oldest first:
```json
[{"op_id": "<uuid>", "table": "handovers", "kind": "insert", "payload": { "<column>": "<value>", "...": "..." }}]
```

- `op_id` is the record's own id, so two phones delivering the same record send the same op.
- Payload keys are the server column names.

It returns one result per op, in order:
```json
[{"op_id": "<uuid>", "result": "applied | duplicate | conflict | rejected", "reason": "<text or null>", "status": "<handover status or null>", "server_seq": 123}]
```

### D.2 Pull

The phone calls `sync_pull(since bigint)`. It returns every row with `server_seq > since`, ordered by `server_seq`, at most 2,000 rows:
```json
{"max_seq": 456, "more": false, "rows": {"people": [], "devices": [], "items": [], "handovers": [], "alerts": [], "resolutions": [], "missing_reports": []}}
```

The phone saves `max_seq` as `sync_state.last_seq` and calls again while `more` is true.

### D.4 Enrol

The phone calls `enrol_device(p_staff_no text, p_code text, p_device jsonb)` once, online, through `EnrolApi`, not the outbox:
```json
{"p_staff_no": "R-2210", "p_code": "552917", "p_device": {"id": "<uuid>", "public_key": "<b64url SPKI>", "key_fp": "3F:A9:12:7C", "key_security": "tee", "app_version": "r07", "performed_at": "<ISO time>"}}
```
It returns:
```json
{"result": "applied | duplicate | rejected | locked", "reason": "<text or null>", "person": { "<people row without enrol_code_hash>": "..." }, "device": { "<devices row>": "..." }}
```
On `applied` or `duplicate`, in one Room transaction, the phone:
- saves the person and the device (`sync_status = synced`)
- sets the identity
- puts `enrolled_at` and `device_public_key` in `sync_state`
- marks the app unlocked

Then it syncs.

### D.5 Relay (online handover, A.10)

Online only, never queued, through `OfferRelayApi` (`data/remote/`) and `RelayRepository` (`data/repo/`). Each needs an enrolled session (error `42501 not enrolled` otherwise).

| Call | Who | Returns |
|---|---|---|
| `relay_offer(p_id, p_item_id, p_to_person_id, p_offer, p_expires_ms)` | giver | `{"result": "posted" \| "duplicate" \| "rejected", "reason"}`. A new offer for the same item cancels the giver's older open ones. |
| `relay_inbox()` | receiver | `[{"id", "item_id", "from_person_id", "offer", "expires_ms"}]`: open offers to me, up to 30 s past expiry |
| `relay_accept(p_id, p_acceptance)` | receiver | `{"result": "accepted" \| "duplicate" \| "rejected", "reason"}` |
| `relay_close(p_id)` | either | `{"result": "closed" \| "unchanged" \| "rejected", "status"}`: the receiver declines, the giver cancels |
| `relay_status(p_id)` | either | `{"status": "open" \| "accepted" \| "declined" \| "cancelled" \| "unknown", "acceptance"}` |

`offer_relay` rows are never pulled by `sync_pull` and never stored in Room.

### D.3 When sync runs
- after every local save
- when the network returns
- every 15 minutes
- on **Sync now**
- every 10 seconds while the Alerts screen is open and online

A push is always followed by a pull.

---

## E. What the phone does with each result

| Result | Local record | Outbox entry |
|---|---|---|
| `applied` | `sync_status = synced`, and `status` from the result | Deleted |
| `duplicate` | `sync_status = synced` | Deleted |
| `conflict` | `sync_status = synced`, `status` = `forked` or `orphan`. It shows as Disputed or waiting through `deriveCustody` and the alerts. | Deleted |
| `rejected` | `sync_status = rejected`, `sync_reason` = the reason. Kept on the phone, left out of `deriveCustody`, and shown in red on the timeline. | Deleted |
| Network error, timeout, 5xx, or no session yet | Unchanged | Kept: `attempts + 1` and `last_error`. WorkManager retries with backoff. |
| `not enrolled` raised by the server (C.0) | Unchanged | Kept, as for a network error. `last_error` shows it, and the lease (A.9) runs out unless the phone enrols again. |

**Merging pulled rows** (upsert by `id`):
- **Server-owned columns** overwrite the local ones: `status`, `status_reason`, `server_seq`, `received_at`, `created_by`, `performed_at`, and `closed_at` and `resolution_id` on alerts.
- **Local-only columns** are kept: `origin`, `hidden_by_tamper`, `sync_status`. A pulled row that's new to the phone gets `sync_status = synced` and `origin = pulled`.
- **Signed fields never change.** If a pulled handover's `record_hash` differs from the local copy, keep the server copy and set `sync_state.last_error` to "Server copy of <id> differs from this phone's".

**Debug, Clear local data:** deletes every row and resets `last_seq` to 0. It keeps the Keystore key and the identity keys in `sync_state` (`person_id`, `device_id`, name, role, `enrolled_at`, `device_public_key`, `last_sync_at`), so the phone stays enrolled and can still unlock offline, then it syncs.

---

## F. How conflicts appear and get resolved

| Situation | Safety officer | Every other phone, after it syncs |
|---|---|---|
| Fork | A red alert naming the giver. O2 shows both branches, with every signature checked on this phone. | The camera shows **Disputed** on Store, My custody and the timeline |
| Resolved fork | The alert collapses to "Resolved by Dr Tan" | The chosen branch counts. The other shows as "Overruled" on the timeline. |
| Seal mismatch | An amber alert | The record counts, with an amber mark on the timeline |
| Rejected record | A grey alert with the reason | A red entry on the sending phone's timeline |
| Orphan | Hidden until it waits more than 5 minutes, then a grey "Waiting for an earlier record" | Not shown |

The server never chooses where a camera is. Only a safety officer's signed resolution does.

There are **no editable tables**, so the kit's "two phones edit the same field" and "an edit to a deleted record" cases can't happen. The tests below cover the clash rules instead.

---

## G. Sync tests (`app/src/test/.../sync/`, against a fake server that follows §C)

1. A handover made offline, then synced: `applied`, `accepted`.
2. The same batch pushed twice after a dropped connection: the second time it's all `duplicate`, and there's one row.
3. The giver's and the receiver's phones both deliver the same handover: one row, and the second is `duplicate`.
4. Two handovers by the same giver after the same previous record: the second is `forked`, and a fork alert names the giver.
5. A new id with a reused `nonce`: rejected, "replayed offer", with a rejected alert.
6. A giver who wasn't the holder: rejected.
7. A child arrives before its parent: `orphan`. Then the parent arrives, both are `accepted`, and the alert is closed.
8. The wrong seal: `accepted`, plus a seal mismatch alert.
9. A phone clock a day fast: accepted, because order comes from the chain. `performed_at` keeps the phone's time, and `received_at` the server's.
10. A resolution choosing the forked branch: the statuses swap. A second resolution is rejected.
11. An unknown device: rejected.
12. A changed field with the old `record_hash`: rejected, "hash mismatch".
13. **Not covered by the fake server:** C.0, `enrol_device` and C.3.4b. They're checked live against Supabase (the `access` migration's checks) and in `test/.../auth/AccessRulesTest.kt`.
14. `deriveCustody`:
    - tampered records are hidden
    - a fork with no resolution is Disputed
    - a resolution picks the chosen branch
    - overdue appears once `due_ms` passes
    - `serverView` ignores unsynced records
