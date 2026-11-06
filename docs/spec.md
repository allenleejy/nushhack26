# Spec: Tenet

6 Nov 2026. The app is **Tenet**, the name the design system in `design/` uses. The idea is in `docs/concept.md`, the setting in `docs/setting.md`, and the exact record formats and sync rules in `docs/sync.md`. Where this spec and `sync.md` disagree, `sync.md` wins on formats and server rules, and this spec wins on screens and behaviour.

---

## 1. One-liner and target user

**One-liner.** Every time a radiography camera changes hands, the giver and the receiver both sign the handover on their phones and swap it by QR code, with no network. The latest signed handover shows who is liable now, an overdue return raises an alarm on the phone with no network, and when phones reconnect the server rebuilds one checked chain and names anyone who handed the same camera over twice.

**Target users,** at a Singapore industrial radiography company:
- **Store officer** (Siti): issues cameras from the store and receives them back.
- **Radiographer**, a registered radiation worker (Ravi, Wei Ming, Jun): carries a camera to site, passes it to colleagues, returns it.
- **Radiation safety officer** (Dr Tan): watches every camera, gets the alerts, decides disputes.

**The QR code is signed, not secret.** A photo of it is useless: it's made out to one named person, works once, expires in 60 seconds, and still needs the camera's seal scanned and the receiver's own fingerprint. The demo invites a judge to try.

---

## 2. Demo script

**Devices.**

| Label | Device | Person | Role | Network during the demo |
|---|---|---|---|---|
| A | Android phone | Siti Aminah | Store officer | Online for sign-out, then airplane mode |
| B | Android phone | Ravi Kumar | Radiographer, **the tampered phone** | Online for sign-out, then airplane mode |
| C | Android phone | Tan Wei Ming | Radiographer | Online for sign-out, then airplane mode |
| D | Laptop emulator, mirrored | Dr Tan Mei Ling | Radiation safety officer | Online throughout |

**Before the pitch (online, about 10 minutes).**
1. On each device: install the release, open the app, tap your name on Enrol, tap **Create my signing key**, touch the fingerprint (PIN on the emulator). Each shows "Enrolled as …".
2. On A: Debug, **Sync now**. Store shows C-12, C-07 and C-15, all "In store".
3. On B: open Me, tap the version 5 times, turn on **Tampered phone**. Nothing on screen changes.
4. On D: open Alerts. It shows "All cameras accounted for".
5. Put the printed C-12 seal sticker on the prop camera. Ravi keeps the second copy of the C-12 seal in his pocket.
6. Mirror A, B, C with `scrcpy` and D's emulator window on the projector.

**The demo (about 3 minutes).**

1. **Sign-out, online (0:00–0:30).**
   - A (Siti): Store → **Issue** on C-12 → pick **Ravi Kumar** → due chip **+90 s** → **Sign** → fingerprint. A shows the offer card: "Siti Aminah → Ravi Kumar · C-12 · IR-0457 · For Ravi only · single use · valid for 0:58" with the QR inside it.
   - B (Ravi): My custody → **Receive** → camera scans A's QR. The checklist turns green: signed by Siti, Siti holds C-12, made out to you, valid, not used before. **Scan the seal** → he scans the sticker → "Seal S-40117 matches C-12". The accept card says "This is sealed source IR-0457 (iridium-192). You're liable for it the moment you sign." → **Accept** → fingerprint. B shows its acceptance QR and the match code.
   - A: **Scan acceptance** → scans B. Both A and B show "Signed by both · checked offline · 2 of 2 signatures valid" and the same match code, for example **48 21 07**. Siti and Ravi read it aloud.
   - Wait until A's and B's sync bars show "All synced". **Airplane mode on: A, B and C.**
2. **Shift change, offline (0:30–1:10).**
   - B (Ravi): My custody → C-12 card → **Hand over** → **Tan Wei Ming** → **Sign** → fingerprint. B shows the offer card with "For Wei Ming only · single use · valid for 0:58".
   - **The presenter invites a judge to photograph B's offer QR** with their own phone.
   - C (Wei Ming): **Receive** → scans B → checklist green → scans the seal → accept card → fingerprint → acceptance QR. B scans it. Both show the same match code. C's home: "You hold C-12 · return by 14:31 · 1:02 left" (example times).
   - B's home **still says "You hold C-12"**. The presenter: "Ravi's phone has been tampered with. It kept a copy of the token."
3. **The cheat (1:10–1:40).**
   - B (Ravi): C-12 → **Hand over** → **Siti Aminah (return to store)** → sign. A scans it: checklist green, because offline nobody can tell. Siti scans the **copied seal** → match → accept → fingerprint. B scans A's acceptance. A's Store shows C-12 "In store".
   - The presenter: "Offline, nobody can tell. But Ravi has now signed two handovers of the same camera."
4. **The replay (1:40–2:00).** By now the judge's photo is over a minute old.
   - A (Siti): Store → **Receive return** → scans the judge's photo. Red screen: **"This is a copy. It can't be used."** ✗ Made out to Tan Wei Ming, not you · ✗ Expired 0:10 ago (example) · "Nothing was signed."
5. **Overdue (2:00–2:20).**
   - C (Wei Ming) alarms on its own with no network: red banner **"C-12 return overdue"**, sound, vibration and a notification.
   - D (Dr Tan) Alerts, amber card: "C-12 overdue · last holder the server knows: Ravi Kumar, signed 14:29 · newer handovers may not have synced."
6. **Reconnect (2:20–2:50).**
   - Airplane mode off on **C first**, tap **Sync now**. Then A, Sync now. Then B.
   - D Alerts within 10 seconds: red **Fork** card: "Ravi Kumar signed two handovers of C-12 after H1 · both signatures valid on this phone". The overdue card changes to "Last signed holder: Tan Wei Ming".
   - D: tap the fork card. Two branches side by side: "Ravi → Wei Ming · 14:30 · delivered by Wei Ming's phone" and "Ravi → Siti · 14:31 · delivered by Siti's phone", both signed by Ravi's key 3F:A9:12:7C. The presenter: "Ravi's phone hid both handovers. The receivers' phones delivered them."
7. **The decision (2:50–3:10).**
   - D: **C-12 is with Tan Wei Ming** → note "Seal on camera checked by store" → **Sign decision** → fingerprint. The alert shows "Resolved by Dr Tan". After A syncs, A's Store shows C-12 "Out · Tan Wei Ming · overdue".

**Optional extras if time allows:** the wrong finger on B is refused by the system prompt, so nothing is signed (10 s); Wei Ming returns C-12 to Siti online and the server checks it straight away (20 s).

---

## 3. Screens

Designed for a 412×915 dp Android phone, portrait. Times on screen are phone times.

**Visual design: `design/`, the Tenet design system.** Build every screen from it:
- **Rules:** read `design/brand.md` first.
- **Components:** follow `design/components/<Name>/README.md` for each one a screen uses. Open `design/index.html` to see them live.
- **Theme:** it's already in `ui/theme/`: `TenetTheme`, `Tenet.colors` for the extra roles, `TenetType`, `TenetDimens`, `PrimaryButtonShape`, `ChipCutShape`, and the `hazardStripes`, `hatching` and `verticalText` modifiers.
- **Icons:** `Icons.Sharp.*` (material-icons-extended is a dependency).
- **The text in this section is the content; `brand.md` sets its voice and casing:**
  - Space Mono UPPERCASE for titles, buttons, labels, names, IDs and times
  - Space Grotesk sentence case for anything read as a sentence
  - buttons are a verb and its object (SIGN AND SHOW QR, SCAN ACCEPTANCE, SCAN SEAL)
  - no exclamation marks

  Where this spec and a component README disagree on wording, use the README.

**Already built in the shared files** (cards don't rebuild them):
- the top app bar, with the phone's `SyncChip`, which syncs and opens Outbox when tapped
- the 2dp rule under the bar, replaced by a hazard band inside Give and Receive
- the offline banner, the bottom nav, and `ui/components/SyncChip.kt` for per-record sync marks

**Navigation by role** (three bottom tabs, as in `design/components/BottomNav`):
- Not enrolled: Enrol, full screen, no tabs.
- Store officer: **ITEMS** (the Store screen), **SCAN** (Receive, full screen), **OUTBOX**.
- Radiographer: **CUSTODY** (My custody), **SCAN** (Receive, full screen), **OUTBOX**.
- Radiation safety officer: **ITEMS** (the Store screen, read-only), **ALERTS** (where the app opens), **RECORDS** (the daily record).
- **Me** opens from the menu icon at the left of the top app bar. Debug opens from Me.

Route names for `Routes.kt`: `Store`, `Cameras`, `MyCustody`, `Give(itemId)`, `Receive`, `Timeline(itemId)`, `Alerts`, `Fork(alertId)`, `Outbox`, `Me`, `Debug`, `DailyRecord`, `ReportMissing(itemId)`. Enrol isn't a route: `AppScaffold` shows it until the phone has an identity.

### E1 Enrol (`ui/enrol/`)
- **Purpose:** link this phone to one person and create its signing key. Needs the network once.
- **Components:** title "Who are you?"; a list of roster rows (name, role, staff number); after picking, a card "Create my signing key" with a primary button; a result card.
- **Data:** `people` (active), this phone's `devices` row.
- **Actions:** pick a name → **Create my signing key** → system fingerprint or PIN prompt → saves the `devices` row through the outbox and syncs at once.
- **Empty:** "Connect once to download your team." with a **Try again** button.
- **Loading:** three skeleton rows while the roster downloads.
- **Error:** "Couldn't reach the server. Enrolment needs a connection once." / "Set a fingerprint or PIN on this phone to sign handovers." (no secure lock screen, release builds) / "Fingerprint cancelled. Nothing was created."
- **Waiting to sync:** "Enrolled as Ravi Kumar · key 3F:A9:12:7C. Others can check your signatures once this phone syncs." with a spinner chip.

### E2 Me (`ui/me/`)
- **Purpose:** who this phone is and how sync stands.
- **Components:** name, role and staff number; the key card ("Device key 3F:A9:12:7C · hardware-backed (TEE)"); the sync card (waiting count, last sync, last error, **Sync now**); the release version ("Release r07"), which opens Debug after 5 taps.
- **Empty / loading / error:** "Server not configured" when there's no Supabase URL; "Will sign in when online"; the last sync error in red text.
- **Waiting to sync:** the waiting count.

### E4 Outbox (`ui/outbox/`)
- **Purpose:** what this phone hasn't delivered yet. The top app bar's `SyncChip` opens it.
- **Components:**
  - the outbox count and the last sync time
  - one `ListRow` per waiting record, for example "HANDOVER · C-12 · RAVI → WEI MING · SIGNED 14:30" with a WAITING `SyncChip`, and its last error, if any, in `alert-text`
  - a **SYNC NOW** primary button
- **Data:** `outbox` joined to the record it carries; `sync_state`.
- **Empty:** "Nothing waiting. Every record on this phone has reached the server." with the last sync time.
- **Error:** the last sync error in words, for example "Can't reach the server. Records stay on this phone."

### E3 Debug (`ui/debug/`, hidden)
- The outbox entries, last sync time and error, **Sync now**, **Clear local data**.
- **Tampered phone** switch: "Keep an old copy of the token and hide my handovers (demo)".
- **Test signing:** signs and checks a test string, then shows "OK · TEE" or the error.
- Debug builds only: **Paste code** (feeds clipboard text to the open scanner).
- **Empty:** "Outbox empty".

### S1 Store (`ui/store/`; also the safety officer's ITEMS tab, read-only)
- **Purpose:** every camera, who holds it per the latest signature this phone knows, and when it's due back.
- **Components:** one row per camera: code and source ("C-12 · IR-0457 · Ir-192"), status chip (**In store**, **Out**, **Overdue** in red, **Disputed** striped red), holder line ("Ravi Kumar · signed 14:29"), due line ("Due 14:31 · 1:02 left"), and a "Not synced yet" mark. A primary **Issue** button on in-store rows and **Receive return** at the top. Tapping a row opens the timeline.
- **Data:** `items`, `handovers`, `resolutions`, `alerts`, computed with `deriveCustody` (see `sync.md`).
- **Actions:** Issue → G1 with the camera picked; Receive return → R1; row → T.
- **Empty:** "No cameras yet. Sync once to download them."
- **Loading:** three skeleton rows.
- **Error:** "Couldn't read this phone's records. Restart the app." (local database error).
- **Waiting to sync:** the "Not synced yet" mark on rows whose latest record hasn't synced.
- **The safety officer's ITEMS tab:** no Issue or Receive buttons, and the holder line reads "As the server knows it · signed 14:29".

### P1 My custody (`ui/mycustody/`)
- **Purpose:** what I hold, when it's due, and my receipts.
- **Components:**
  - A large card for each camera I hold: "You hold C-12 · sealed source IR-0457 · return by 14:31", a live countdown, and a **Hand over** button.
  - When overdue, a full-width red banner: "C-12 return overdue · 4 min" with **Hand back to store**.
  - A "Handover not confirmed: scan Wei Ming's acceptance" banner when an offer was left without its acceptance scanned. It opens G4.
  - A **Receipts** list: "Handed C-12 to Tan Wei Ming · 14:30 · signed by Wei Ming ✓".
  - The main **Receive** button.
- **Data:** `handovers` and `items` via `deriveCustody`, plus the local pending offer.
- **Empty:** "You don't hold anything. Tap Receive when someone hands you a camera."
- **Loading:** skeleton card.
- **Error:** as S1.
- **Waiting to sync:** "Not synced yet" on cards and receipts.

### G Hand over (`ui/give/`, one route with steps)
- **G1 Choose.** The camera (preselected), and the receiver from the roster. A store officer sees radiographers ("Issue to"). A radiographer sees "Hand to a colleague" (radiographers) and "Return to store" (store officers). On issue only, the due time chips **+90 s**, **+5 min**, **17:00**, **19:00**.
  - **Empty:** "You don't hold this camera" (it isn't yours to give) / "No one on the roster can receive it. Sync once."
- **G2 Review and sign.** The who-and-what card: giver → receiver with names and roles; "C-12 · sealed source IR-0457 (Ir-192) · seal S-40117"; due time. The primary button **Sign** with the line "Place your finger to sign this handover".
  - **Error:** "Fingerprint cancelled. Nothing was handed over."
- **G3 Offer QR.** The same card with the QR inside it, the line **"For Wei Ming only · single use · valid for 0:58"** counting down, "Waiting for Wei Ming to accept", the primary button **Scan acceptance**, and the text button **Cancel offer**. The screen stays awake at full brightness.
  - **Expired:** "Offer expired. If Wei Ming already accepted, scan their code. Otherwise sign a new offer."
  - Debug builds: **Copy code**.
- **G4 Scan acceptance.** The viewfinder.
  - **Error:** "This isn't Wei Ming's acceptance of this offer." / "Not a handover code." / "Allow the camera to scan codes." with **Open settings**.
- **G5 Done.** A green check, "Signed by both · checked offline · 2 of 2 signatures valid", the match code in large digits, "**48 21 07** · check that Wei Ming's screen shows the same", and "Receipt saved · not synced yet" (or "Synced").

### R Receive (`ui/receive/`, one route with steps)
- **R1 Scan the offer.** A viewfinder, a torch toggle, and "Point at the giver's code".
  - **Error:** "Not a handover code." / camera permission, as G4.
- **R2 Checks.** "Checking offline…" (under a second), then one line per check, each ✓ or ✗:
  1. "Signed by Ravi Kumar · key 3F:A9:12:7C"
  2. "Ravi Kumar holds C-12 · signed by Siti and Ravi at 14:29"
  3. "Made out to you"
  4. "Valid for 0:41"
  5. "Not used before"

  If all pass, the button is **Scan the seal**.
  - **Refused (any ✗):** a red header **"This is a copy. It can't be used."** (or "This handover can't be accepted." when the code isn't a copy), the failed lines in red with reasons ("Made out to Tan Wei Ming, not you", "Expired 0:40 ago", "Already used on this phone", "Unknown signer. Sync this phone before accepting."), and "Nothing was signed."
- **R3 Scan the seal.** "Scan the seal on camera C-12".
  - **Match:** "Seal S-40117 matches C-12" → **Next**.
  - **Mismatch:** "Seal S-40999 doesn't match C-12 (S-40117). Don't accept this camera." with no way on.
- **R4 Accept.** The who-and-what card plus the statement "This is sealed source IR-0457 (iridium-192). You're liable for it the moment you sign." and the due time. The primary button **Accept**, with "Place your finger to accept".
  - **Error:** "Fingerprint cancelled. Nothing was signed. Ravi still holds C-12."
- **R5 Acceptance QR.** The QR in the card, "Show this to Ravi", and the match code. After Ravi scans it, or on **Done**: "You hold C-12. Return by 14:31." and "Not synced yet".

### T Timeline (`ui/timeline/`)
- **Purpose:** the custody chain of one camera.
- **Header:** "C-12 · IR-0457 · Ir-192 · seal S-40117", the status chip, the holder and the due time.
- **List:** one entry per record, oldest first.
  - "Registered · in store with Siti Aminah"
  - "H1 · Siti Aminah → Ravi Kumar · issue · 14:29 · both signed ✓ · synced"
  - "H2 · Ravi Kumar → Tan Wei Ming · 14:30 · both signed ✓ · not synced yet"
  - A fork shows as two entries side by side with a red "Fork" marker.
  - A resolution entry: "Dr Tan: C-12 is with Tan Wei Ming · 14:34 · signed ✓".
- **Action:** **Report missing** (wave 2).
- **Empty:** "In store since registration. No handovers yet."
- **Loading:** skeleton entries.
- **Error:** an entry in red: "Signature check failed on H2b" / "Refused by server: replayed offer".
- **Waiting to sync:** "Not synced yet" on each unsynced entry.

### O1 Alerts (`ui/alerts/`)
- **Purpose:** what needs the safety officer, most serious first.
- **Cards:**
  - **Fork (red):** "Ravi Kumar signed two handovers of C-12 after H1 · both signatures valid on this phone" → O2. If a signature fails: "One record has an invalid signature. It isn't proof against Ravi Kumar."
  - **Overdue (amber):** "C-12 overdue 4 min · last holder the server knows: Ravi Kumar, signed 14:29 · newer handovers may not have synced", worked out from synced records only.
  - **Seal mismatch (amber):** "C-12 accepted by Jun Lim with seal S-40999 (expected S-40117)".
  - **Refused (grey):** "Refused: replayed offer for C-12 from Siti's phone".
  - **Resolved:** cards collapse to "Resolved by Dr Tan · 14:34".
- **Behaviour:** while the screen is open and online, it syncs every 10 seconds.
- **Empty:** a green check, "All cameras accounted for · last sync 14:02".
- **Loading:** skeleton cards.
- **Error:** "Can't reach the server. Showing what synced at 14:02."
- **Waiting to sync:** "Decision not synced yet" on a resolved card.

### O2 Fork (`ui/fork/`)
- **Purpose:** see both branches and decide where the camera is.
- **Components:**
  - A header: "C-12 · fork after H1".
  - Two columns, one per branch. Each shows the giver → receiver, the time, "delivered by Wei Ming's phone at 14:33", and "Signed by Ravi Kumar · key 3F:A9:12:7C ✓ · signed by Tan Wei Ming ✓".
  - The line "Both follow H1. Same giver: Ravi Kumar."
  - One choice per branch, for example "C-12 is with Tan Wei Ming".
  - A note field, and **Sign decision** with a fingerprint.
- **Data:** the alert, its handovers, and `devices` for the keys. The phone checks every signature itself.
- **Empty:** "This alert has no records. Sync again."
- **Error:** a branch with a ✗ signature, in red.
- **Waiting to sync:** "Decision signed · not synced yet".

### O4 Daily record (`ui/dailyrecord/`, wave 2)
- **Purpose:** the reg 26 records, produced from the chain.
- **Content:** for a chosen day, (1) "Movements out of and into the store", one line per issue and return; (2) "Location at end of day", one line per camera with its last signed holder. A **Share** button sends it as plain text.
- **Empty:** "No movements on this day."

### Report missing (`ui/missing/`, wave 2)
- **Purpose:** reg 26(3). A sheet from the timeline: "Report C-12 missing" with the last signed holder prefilled, a note, and **Send report**.
- **Offline:** it saves at once and shows "Report queued · sends when online".

---

## 4. Data model

Full column lists, SQL types and the server's rules are in `sync.md` §B. In short, the server and the phone hold the same tables:

| Table | Kind | Who writes | Key columns |
|---|---|---|---|
| `people` | Read-only (seed) | Seed only | `id uuid PK`, `name`, `role` (`store_officer`, `radiographer` or `rso`), `staff_no`, `nea_reg_no`, `enrol_code_hash`, `active` |
| `devices` | Append-only | The phone, at enrolment | `id uuid PK`, `person_id FK people`, `public_key`, `key_fp`, `key_security` |
| `items` | Read-only (seed) | Seed only | `id uuid PK`, `code` (C-12), `source_serial`, `isotope`, `seal_no`, `store_name` |
| `handovers` | Append-only, **with clash rules** | Phones (the giver makes the id) | Signed fields: item, kind, previous record, giver and receiver (person and device), seal, due, one-time number, times, both signatures, record hash. Server fields: `status`, `status_reason`. |
| `alerts` | Server-written only | `sync_push` | `id`, `kind` (fork, seal_mismatch, orphan or rejected), `item_id`, `person_id` (the person named), `handover_ids`, `reason`, `resolution_id` |
| `resolutions` | Append-only, with a clash rule | The safety officer's phone | `alert_id`, `chosen_handover_id`, `note`, signature |
| `missing_reports` | Append-only | Phones | `item_id`, `last_handover_id`, `note` |

Every server table also has `version`, `server_seq`, `deleted_at`, `performed_at`, `received_at` and `created_by`, as the kit requires. Because every table is append-only or server-written, nothing is ever edited by a phone, so there are no base-version conflicts and no deletes.

**Conflict rules for editable tables:** none. There are no editable tables.

**Clash rules the server checks** (`sync.md` §C):
- **`handovers`:**
  - a camera has one holder at a time
  - the giver must be the holder of the previous record
  - a one-time number is used once
  - both devices must be on the roster and belong to the named people
  - the roles must fit the kind
  - the hashes must link

  A second handover from the same previous record is stored as **forked**, with an alert naming the giver. A wrong seal is accepted with an alert. A record whose previous record hasn't arrived is held as an **orphan** and checked again later. Everything else that breaks a rule is **rejected** with a reason, logged as an alert, never dropped silently.
- **`resolutions`:** only a safety officer can decide, and only once per alert. The decision makes the chosen branch accepted and the other overruled.
- **`devices`:** the person must exist and be active. In wave 2, the enrolment code must match.

**Who may read and write:** every signed-in phone may read every table (it's one team, and phones need the roster and the chains). No phone may insert, update or delete directly. Every write goes through `sync_push` (security definer), which checks the rules and sets `created_by = auth.uid()`. `people` and `items` change only through migrations and seed.

**Sign-in:** each phone signs in anonymously once while online and keeps its session. The officer picks their name on Enrol. The phone's public key on the roster, not the Supabase account, is what identifies a person in a signature.

**Phone-only tables:** `outbox` and `sync_state` (from the kickoff); `pending_offers` (the giver's offer waiting for its acceptance scan); and on `handovers` the local columns `sync_status`, `sync_reason`, `origin` and `hidden_by_tamper`.

**Storage buckets:** none.

---

## 5. Offline and sync behaviour

**Works offline:**
- issue, hand over, return and receive (both QR scans, the seal scan, both fingerprints, the match code)
- every check on the receiver's phone
- the homes, the timeline and the countdown
- the overdue alarm
- the safety officer's decision (queued)
- reports

**Needs the network:**
- the first enrolment (download the roster, upload the public key)
- seeing other phones' records
- the server's checks, including fork detection
- the safety officer's alerts, which are as fresh as the last sync

**How the officer sees what's waiting:**
- the sync bar (offline banner, "N waiting", last sync)
- "Not synced yet" on each record, card and receipt
- the Debug outbox

**How conflicts appear and get resolved:**
- **A fork:** a red alert for the safety officer, and "Disputed" on Store, My custody and the timeline on every phone after it syncs. The safety officer resolves it on O2 with a signed decision, which then shows on every phone.
- **A seal mismatch:** an amber alert. The record still counts, and the safety officer follows up.
- **A refused record:** a grey alert, plus a red entry on the timeline of the phone that sent it.
- **An orphan:** held, and accepted on its own once its previous record arrives.
- The server never guesses where a camera is. A person decides, and the decision is signed.

**Overdue:**
- **On every phone:** worked out from that phone's own records. The holder's phone alarms while the app is open (banner, sound, vibration) and posts a notification.
- **On the safety officer's phone:** overdue uses synced records only, so it says what the server knows and that newer handovers may not have synced.

---

## 6. AI features

None.

---

## 7. Build plan

**Before wave 1, the plan job (the build account) creates:**
- the server migration, `sync_push` and `sync_pull`, and the seed
- every Room entity and DAO, the server classes, and one repository per feature with `save`
- every route with a placeholder screen
- the dependencies and permissions
- the `crypto/` API as interfaces and data classes with `TODO()` bodies
- the shared QR pieces: `ui/components/QrCode.kt` and `ui/components/QrScanner.kt`
- the sign prompt wrapper `ui/components/SignPrompt.kt`
- in Debug, the Tampered phone switch and Test signing
- the printable seal codes in `docs/demo/`
- the sync tests, marked `@Ignore`

| # | Card | Owns | Depends on | Wave | Devin mode |
|---|---|---|---|---|---|
| 01 | Token core: keys, signing, checking, record hashes, match code, QR payloads, offer checks, `deriveCustody` | `crypto/`, `test/.../crypto/` | Plan | 1 | Fusion |
| 02 | Sync engine: push, pull, result handling, retries; un-ignores the sync tests | `sync/` (except the kickoff's monitor and scheduler), `test/.../sync/` | Plan | 1 | Fusion |
| 03 | Hand over, G1–G5 | `ui/give/`, `data/repo/GiveRepository.kt`, `GiveScreenshotTest.kt` | Plan (01 for real signing) | 1 | Normal |
| 04 | Receive, R1–R5, including the refused screen | `ui/receive/`, `data/repo/ReceiveRepository.kt`, `ReceiveScreenshotTest.kt` | Plan (01) | 1 | Normal |
| 05 | Store, My custody, timeline, overdue alarm | `ui/store/`, `ui/mycustody/`, `ui/timeline/`, `alarm/`, `data/repo/CustodyRepository.kt`, three screenshot tests | Plan (01 for `deriveCustody`) | 1 | Normal |
| 06 | Enrol, Me, Outbox, Alerts, Fork and the signed decision | `ui/enrol/`, `ui/me/`, `ui/outbox/`, `ui/alerts/`, `ui/fork/`, `data/repo/EnrolRepository.kt`, `data/repo/SafetyRepository.kt`, three screenshot tests | Plan (01, 02) | 1 | Normal |
| 07 | Daily record (reg 26) and Report missing | `ui/dailyrecord/`, `ui/missing/`, their repositories and tests | 05 | 2 | Normal |
| 08 | Enrolment code, and the key details sheet on Me (fingerprint, security level) | `ui/enrol/`, `ui/me/` | 06 | 2 | Lite |
| 09 | Offline fork detection: a phone holding both branches warns, and R2 refuses "already handed over" | `crypto/OfferChecker.kt`, `crypto/Custody.kt`, `ui/timeline/` fork marker | 01, 05 | 2 | Normal |
| 10 | Maestro flows for the demo path, and demo polish of copy and seed | `.maestro/`, `supabase/seed.sql` (via a pull request) | 03–06 | 2 | Lite |
| 11 | Offers encrypted for the receiver, so a photo shows nothing | `crypto/` codec, a migration adding `devices.agreement_key` | 01, 09 | 3 | Normal |
| 12 | Radio check: a 4-digit challenge, a 6-digit answer | `ui/radiocheck/`, `crypto/RadioCheck.kt` | 01 | 3 | Normal |
| 13 | Handover over Nearby Connections, the same signed records | `transport/nearby/`, a "Send by radio" option in G3 and R1 | 03, 04 | 3 | Fusion |
| 14 | Signature check on the server, in an Edge Function before `sync_push` | `supabase/functions/` | 02 | 3 | Normal |

Overnight wave: polish and the QA sweep (`role-build.md`).

---

## 8. Demo data

**People** (seed; enrolment codes for wave 2, stored as SHA-256 hashes):

| Name | Role | Staff no | NEA reg (dummy) | Enrolment code |
|---|---|---|---|---|
| Siti Aminah | Store officer | S-1021 | (none) | 418203 |
| Ravi Kumar | Radiographer | R-2210 | RW-DEMO-1452 | 552917 |
| Tan Wei Ming | Radiographer | R-2234 | RW-DEMO-1478 | 730164 |
| Jun Lim | Radiographer | R-2251 | RW-DEMO-1503 | 286540 |
| Dr Tan Mei Ling | Radiation safety officer | O-3001 | (none) | 904372 |

**Cameras** (seed; all dummy, at "Tuas Store 1"):

| Code | Source serial | Isotope | Seal |
|---|---|---|---|
| C-12 | IR-0457 | Ir-192 | S-40117 |
| C-07 | IR-0391 | Ir-192 | S-40102 |
| C-15 | SE-0112 | Se-75 | S-40125 |

Each camera has a server-made **register** record: "in store with Siti Aminah".

**Seal stickers** (printed from `docs/demo/`):
- C-12 twice: one for the prop camera, one "copy" for the cheat
- C-07 once
- C-15 once

**History that looks lived-in:** made on real phones during rehearsal, because signed records can't be seeded. Siti issues C-07 to Wei Ming, and Wei Ming returns it.

**Due chips:** +90 s (demo), +5 min, 17:00, 19:00.

**The prop:** any small box labelled "C-12 · RADIOGRAPHY CAMERA · DUMMY", with the C-12 seal sticker on it.

---

## 9. Out of scope

- server-signed checkpoints (the offer carries the giver's proof record instead; `sync.md` §A)
- signature checks on the server (card 14 is a stretch; phones check every signature)
- NFC tags with one-time codes, NFC phone-to-phone handover, and hardware key attestation
- an encrypted local database (SQLCipher), an app lock, `FLAG_SECURE` and remote wipe (named in the pitch as production settings)
- device revocation
- any AI feature
- more than one site, item types other than radiography cameras, photos, and editing or deleting records
- real sources, real NEA numbers and real seal hardware

---

## 10. Risks and fallbacks

| Risk | Fallback |
|---|---|
| The offer QR (about 1 KB) scans badly from a phone screen | Full brightness on QR screens, low error correction, short JSON keys. If it's still slow, drop the proof record from the QR and rely on the receiver's last sync. |
| A phone has no fingerprint or no secure lock screen | The system prompt accepts the PIN. Debug builds fall back to a labelled software key. |
| Only two Android phones | Wei Ming runs on a second emulator using the laptop webcam. Test it in round 1, and borrow a phone if it fails. |
| A judge's photo of a screen won't scan | P photographs it as well. The refusal works on either photo. |
| Background alarms arrive late (Android 14 limits exact alarms) | The in-app alarm covers the demo, because the app is open. The notification is a bonus. |
| Phone clocks disagree | Demo phones use automatic time. The receiver allows 15 seconds either side of the 60-second window. Order comes from the chain, never the clock. |
| Venue Wi-Fi blocks Supabase | Phone hotspot. The safety officer's emulator uses the laptop's connection. |
| Sync is slow on reconnect | **Sync now** on each phone, and the Alerts screen syncs every 10 seconds. |
| Devin's cards drift apart | Every card codes against the `crypto/` and repository interfaces from the plan job. Merge 01 and 02 first. |
| The fork names someone falsely through a forged record | The safety officer's phone checks both signatures before naming anyone, and says so when one fails. |
| A "Sudden Dev" twist | New rules (for example a witness signature for issue) fit as one more signed field plus one more clash rule in `sync_push`. Keep a few hours of Devin capacity free on Saturday. |
