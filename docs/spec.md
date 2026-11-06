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

**Only enrolled staff get in, even offline.** This is how Tenet meets the hackathon's Sudden Development requirement (authentication and authorisation that still work offline):
- **Enrolment, once, online:** a phone joins the team with the person's staff number and a one-time enrolment code from the safety officer. The server checks the code, and until it does, the phone can read nothing.
- **Unlocking, offline:** after that, the app opens only for that person. The lock screen asks for their fingerprint, and the same hardware key that signs handovers signs a fresh challenge, which the phone checks with no network.
- **Authorisation, offline and at sync:** what a person may do comes from the roster: their role, and the date their authorisation to hold sealed sources lapses. The receiver's phone checks it offline, and the server checks it again at sync.
- **A 72-hour offline lease:** each successful sync confirms the phone is still enrolled. After 72 hours without one, the app opens read-only until it connects.

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
1. On each device: install the release, open the app, type your staff number and enrolment code (§8) on Enrol, tap **Create my signing key**, touch the fingerprint (PIN on the emulator). Each shows "Enrolled as …".
   - Press power and wake each phone: Tenet shows LOCKED with that person's name. Unlock it.
2. On A: Debug, **Sync now**. Store shows C-12, C-07 and C-15, all "In store".
3. On B: open Me, tap the version 5 times, turn on **Tampered phone**. Nothing on screen changes.
4. On D: open Alerts. It shows "All cameras accounted for".
5. Put the printed C-12 seal sticker on the prop camera. Ravi keeps the second copy of the C-12 seal in his pocket.
6. Mirror A, B, C with `scrcpy` and D's emulator window on the projector.

**The demo (about 3 minutes).**

1. **Sign-out, online (0:00–0:30).**
   - A (Siti): Store → **Issue** on C-12 → pick **Ravi Kumar** → due chip **+90 s** → **Sign** → fingerprint. A shows "Sent to Ravi's phone · waiting for Ravi to scan the seal and accept".
   - B (Ravi) is already showing the yellow banner: "Siti Aminah is handing you C-12". **Open** → the checklist turns green: signed by Siti, Siti holds C-12, made out to you, valid, not used before, authorised. **Scan the seal** → he scans the sticker → "Seal S-40117 matches C-12". The accept card says "This is sealed source IR-0457 (iridium-192). You're liable for it the moment you sign." → **Accept** → fingerprint.
   - A moves to Done by itself. Both show the parcel label with the stamp landing: HANDED OVER on A, RECEIVED on B. The presenter: "Online it's two fingerprints and the seal, and both phones check both signatures. Now watch what happens when the network goes."
   - Wait until A's and B's sync bars show "All synced". **Airplane mode on: A, B and C.**
2. **Shift change, offline (0:30–1:15).**
   - C (Wei Ming) picks up his phone. Its screen went off, so Tenet locked itself: "LOCKED · TAN WEI MING · R-2234". **Unlock** → fingerprint → it opens, still in airplane mode. The presenter: "No network and no login server. His fingerprint unlocks a key in the phone's secure chip, and the app checks that key's signature against his enrolment."
   - B (Ravi): My custody → C-12 card → **Hand over**. The picker shows **Jun Lim greyed out: "Authorisation lapsed 3 Nov"**. The presenter: "Jun's authorisation lapsed on Tuesday, so even offline Ravi can't hand him a source." → **Tan Wei Ming** → **Sign** → fingerprint. B shows the pulsing tap target: "HOLD TO WEI MING'S PHONE" and "For Wei Ming only · single use · valid for 0:58".
   - **The presenter invites a judge to photograph B's offer QR** with their own phone: B → **SHOW QR CODE INSTEAD**, the judge snaps it, then **USE TAP INSTEAD**.
   - C (Wei Ming): **RECEIVE** tab → Ravi holds B to C (**tap**). Both screens punch, flood amber and buzz. C's checklist turns green with no network → scans the seal → accept card → fingerprint. B moves to Done by itself over the phone-to-phone link, and both stamps land. The presenter: "No network, no codes: one tap and two fingerprints." C's home: "You hold C-12 · return by 14:31 · 1:02 left" (example times).
   - B's home **still says "You hold C-12"**. The presenter: "Ravi's phone has been tampered with. It kept a copy of the token."
3. **The cheat (1:15–1:40).**
   - B (Ravi): Kestrel → **Hand over** → **Siti Aminah (return to store)** → sign → tap B to A on A's **RECEIVE** tab: checklist green, because offline nobody can tell. Siti scans the **copied seal** → match → accept → fingerprint. B finishes by itself. A's Store shows Kestrel "In store".
   - The presenter: "Offline, nobody can tell. But Ravi has now signed two handovers of the same camera."
4. **The replay (1:40–1:55).** By now the judge's photo is over a minute old.
   - A (Siti): **RECEIVE** → **SCAN A QR CODE INSTEAD** → scans the judge's photo. Red screen: **"This is a copy. It can't be used."** ✗ Made out to Tan Wei Ming, not you · ✗ Expired 0:10 ago (example) · ✗ Ravi Kumar already handed C-12 to Siti Aminah at 14:31. This would be a second handover. · "Nothing was signed." The presenter: "Her phone also knows Ravi already handed this camera to her, so it would refuse even a fresh code."
5. **Overdue (1:55–2:10).**
   - C (Wei Ming) alarms on its own with no network: red banner **"C-12 return overdue"**, sound, vibration and a notification.
   - D (Dr Tan) Alerts, amber card: "C-12 overdue · last holder the server knows: Ravi Kumar, signed 14:29 · newer handovers may not have synced."
6. **Reconnect (2:10–2:40).**
   - Airplane mode off on **C first**, tap **Sync now**. Then A, Sync now. Then B.
   - D Alerts within 10 seconds: red **Fork** card: "Ravi Kumar signed two handovers of C-12 after H1 · both signatures valid on this phone". The overdue card changes to "Last signed holder: Tan Wei Ming".
   - D: tap the fork card. Two branches side by side: "Ravi → Wei Ming · 14:30 · delivered by Wei Ming's phone" and "Ravi → Siti · 14:31 · delivered by Siti's phone", both signed by Ravi's key 3F:A9:12:7C. The presenter: "Ravi's phone hid both handovers. The receivers' phones delivered them."
7. **The decision (2:40–3:00).**
   - D: **C-12 is with Tan Wei Ming** → note "Seal on camera checked by store" → **Sign decision** → fingerprint. The alert shows "Resolved by Dr Tan". After A syncs, A's Store shows C-12 "Out · Tan Wei Ming · overdue".

**Optional extras if time allows:** the wrong finger on B is refused by the system prompt, so nothing is signed (10 s); a judge's finger on C's lock screen is refused, and Tenet stays locked (5 s).

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
- Enrolled but locked: Lock (L1), full screen over whatever was open, no tabs.
- Store officer: **ITEMS** (the Store screen), **RECEIVE**, **OUTBOX**.
- Radiographer: **CUSTODY** (My custody), **RECEIVE**, **OUTBOX**.
- Radiation safety officer: **ITEMS** (the Store screen, read-only), **ALERTS** (where the app opens), **DEVICES**.
- The tabs sit side by side in a pager: swipe left and right, or tap the bar. The bar's amber cell slides with the swipe.
- **RECEIVE** (icon `contactless`) is a tab with the bar while it waits for a tap. Once an offer arrives (R2 on), the bar hides, the swipe stops and the hazard band shows until DONE or close, which return to the first tab.
- **Top app bar:** the Junction mark at the left on the tabs (the back arrow on pushed screens, a close icon in a handover), the title, then the `SyncChip` and a 36 dp square with the person's initials ("RK"), which opens **Me**. Debug opens from Me.

**Motion** (`ui/theme/Motion.kt`). Nothing fades.
- Tabs: the pager follows the finger; a tap on the bar scrolls in 280 ms, emphasized easing (0.2, 0, 0, 1).
- A pushed screen slides in from the right in 260 ms while the one under it moves a quarter to the left; back slides it out to the right in 220 ms.
- Steps inside Give and Receive slide left (forward) or right (back) in 220 ms.
- Small things (chips, stepper, scanner brackets) take 150 ms.
- With the phone's "Remove animations" on, everything jumps to its end state, but the haptics still fire.
- This overrides `design/brand.md`'s 150–200 ms for whole screens, and the Receipt README's single 150 ms stamp: the stamp now falls (see Done below).

**The tap moment** (`ui/components/TapPulse.kt`, `Flood.kt`), the same on both phones. Square, never round.
- Waiting: a 160 dp amber square with the contactless icon breathes (1.00 to 1.04, 1400 ms) and sends out square rings (to 1.8×, 1400 ms). "HOLD TO WEI MING'S PHONE" / "Back to back, until both buzz."
- Contact: a punch (spring to 1.18), a click, the rings turn to 45°, "LINKING…", then the amber floods the whole screen (380 ms), thuds, holds 100 ms and shrinks up into the hazard band (240 ms), revealing the next step.
- Linked (the giver, waiting on the receiver): the same square in cyan, calmer: "LINKED" / "Wei Ming is scanning the seal and signing."
- When tap can't work: a panel says why (NFC IS OFF, BLUETOOTH IS OFF, NEARBY DEVICES NOT ALLOWED, NO NFC ON THIS PHONE) with one button to fix it. The QR button stays below.

**The scanner** (`ui/components/QrScanner.kt`) is an instrument: a bezel with a header ("SCANNER · SEAL", a status light, SEARCHING / LOCKED / REJECTED, the torch), amber corner brackets, a scan line sweeping every 1800 ms and a hazard band at its foot. A good code closes the brackets in cyan with a tick; a wrong one flashes them orange with a reject buzz.

**Done: the parcel label** (`ui/components/ParcelLabel.kt`), on both phones at the end of every handover: warning tape top and bottom, the camera's name and code, an empty cell where the stamp lands, and FROM, TO, SEAL and RETURN BY cells. The stamp (RECEIVED / 13:02 / FROM RAVI on the receiver, HANDED OVER / 13:02 / TO WEI MING on the giver) falls from 2.2× in 260 ms, lands with a thud and sends a 6 dp ripple through the cells. Under the label: the sync line, then "VERIFY CODE 482 107 · same on both phones" in small muted type, then DONE. Nobody has to read the code aloud: both phones have already checked both signatures.

**Camera names.** Each camera has a name people say (`items.name`: Kestrel for C-12, Osprey for C-07, Heron for C-15). In titles, the name leads and the codes follow with their words: "KESTREL", then "CAMERA C-12 · SOURCE IR-0457 · SEAL S-40117". In sentences: "Kestrel (C-12)". With no name, the code alone.

**Dates** read DD.MM.YYYY everywhere ("valid to 30.06.2027", "lapsed 03.11.2026"). Times stay 24-hour HH:mm.

Route names for `Routes.kt`: `Store`, `Cameras`, `MyCustody`, `Give(itemId)`, `Receive`, `Timeline(itemId)`, `Alerts`, `Fork(alertId)`, `Outbox`, `Me`, `Debug`, `DailyRecord`, `ReportMissing(itemId)`. Enrol and Lock aren't routes: `AppScaffold` shows Enrol until the phone has an identity, and Lock over the app while it's locked.

### E1 Enrol (`ui/enrol/`)
- **Purpose:** link this phone to one person on the roster, proven by their enrolment code, and create its signing key. Needs the network once.
- **Components:** title "Join your team"; a staff number field ("R-2210"); a 6-digit enrolment code field with the hint "From your safety officer"; the primary button **Create my signing key**; a result card.
- **Data:** nothing local before enrolment: the roster downloads only after the server accepts the code. The `enrol_device` call (`sync.md` D.4), then this phone's `devices` and `people` rows from its answer.
- **Actions:** type the staff number and code → **Create my signing key** → the phone creates its key → system fingerprint or PIN prompt → `enrol_device` → on success the phone saves its identity, opens unlocked and syncs at once.
- **Empty:** the two empty fields; the button stays disabled until both are filled.
- **Loading:** "Checking with the server…" on the button.
- **Error:**
  - "That code doesn't match R-2210. Ask your safety officer." (wrong code, or no such staff number)
  - "Too many wrong codes. Try again in 10 minutes."
  - "No connection. Enrolment needs the network once."
  - After a withdrawal (`sync.md` A.12), a red notice above the form: "This phone was withdrawn by your safety officer. Ask them for a new enrolment code."
  - "Set a fingerprint or PIN on this phone to sign handovers." (no secure lock screen, release builds)
  - "Fingerprint cancelled. Nothing was created."
- **Done:** "Enrolled as Ravi Kumar · radiographer · key 3F:A9:12:7C", then the role's home.

### L1 Lock (`ui/lock/`)
- **Purpose:** only the enrolled person can open the app, with no network.
- **When:** once the phone is enrolled, on every cold start and whenever the screen turns off. Debug builds on a phone with no screen lock skip it, because they sign with the labelled software key.
- **Components:** the hazard diamond; "LOCKED"; the person's name and staff number ("TAN WEI MING · R-2234"); the access line "Access confirmed with the server 14:02 · valid offline until Mon 9 Nov 14:02"; the primary button **UNLOCK** with "Place your finger to unlock".
- **Actions:** **UNLOCK** → system fingerprint or PIN prompt → the phone signs a fresh `CS1-UNLOCK` challenge with its key and checks it against its own enrolled public key (`sync.md` A.7) → the app opens where it was.
- **Error:** "Fingerprint cancelled. Tenet stays locked." / "This phone's key doesn't match its enrolment. Clear the app's storage and enrol again."
- **Lease expired** (no successful sync for 72 hours, `sync.md` A.9): it still unlocks, but a red banner under the top app bar says "Connect once to confirm your access. Until then you can look but not sign.", and Hand over, Receive and Sign decision are disabled.
- Back does nothing, and nothing behind the lock is visible.

### E2 Me (`ui/me/`)
- **Purpose:** who this phone is and how sync stands.
- **Components:** the name, then one line with the role, staff number and authorisation ("RADIOGRAPHER · R-2210 · VALID TO 30.06.2027", or "· LAPSED 03.11.2026" in orange); the access card ("Access confirmed 14:02 · valid offline until Mon 14:02") with **LOCK NOW**; the key card ("Device key 3F:A9:12:7C · hardware-backed (TEE)"); the sync card (waiting count, last sync, last error, **Sync now**); the release version ("Release r07"), which opens Debug after 5 taps.
- **Empty / loading / error:** "Server not configured" when there's no Supabase URL; "Will sign in when online"; the last sync error in red text.
- **Waiting to sync:** the waiting count.

### E4 Outbox (`ui/outbox/`)
- **Purpose:** what this phone hasn't delivered yet. The top app bar's `SyncChip` opens it.
- **Components:**
  - the outbox count and the last sync time
  - one `ListRow` per waiting record, for example "HANDOVER · C-12 · RAVI → WEI MING · SIGNED 14:30" with a WAITING `SyncChip`, and its last error, if any, in `alert-text`
  - a **SYNC NOW** primary button
- **Data:** `outbox` joined to the record it carries; `sync_state`.
- **Empty:** "Every record has been synced." with the last sync time.
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
  - **Authorisation:** receivers who may not take the camera (`sync.md` A.8) stay in the list, greyed out and not selectable, with the reason: "Jun Lim · authorisation lapsed 03.11.2026".
  - **Empty:** "You don't hold this camera" (it isn't yours to give) / "No one on the roster can receive it. Sync once."
- **G2 Review and sign.** The who-and-what card: giver → receiver with names and roles; "C-12 · sealed source IR-0457 (Ir-192) · seal S-40117"; due time. The primary button **Sign** with the line "Place your finger to sign this handover".
  - **Online** (`sync.md` A.10): the line reads "Online · Wei Ming accepts on his phone, no codes to scan"; offline: "Offline · hold your phone to Wei Ming's".
  - **Error:** "Fingerprint cancelled. Nothing was handed over."
- **G3-online Sent.** The same card without a QR: "Sent to Wei Ming's phone", "Waiting for Wei Ming to scan the seal and accept · 2:58" counting down, a pulsing waiting mark, the text buttons **Show QR instead** (→ G3 with the same offer) and **Cancel offer**.
  - **Accepted:** goes straight to G5 by itself.
  - **Declined:** "Wei Ming declined. Nothing was handed over." with **Choose someone else**.
  - **Lost the network:** "Lost the network. Show the QR instead." and G3 opens by itself.
- **G3 Tap** (offline, the default; stepper CHOOSE · TAP). The tap moment above, with "For Wei Ming only · single use · valid for 0:58" and "KESTREL · C-12 → WEI MING" under it. At the bottom **SHOW QR CODE INSTEAD** and **CANCEL OFFER**. After the tap, LINKED in cyan until the acceptance arrives over the link, then G5 by itself.
- **G3 Offer QR** (the fallback, stepper CHOOSE · SHOW OFFER · SCAN ACCEPTANCE; **USE TAP INSTEAD** goes back). The same card with the QR inside it, the line **"For Wei Ming only · single use · valid for 0:58"** counting down, "Waiting for Wei Ming to accept", the primary button **Scan acceptance**, and the text button **Cancel offer**. The screen stays awake at full brightness.
  - **Expired:** "Offer expired. If Wei Ming already accepted, scan their code. Otherwise sign a new offer."
  - Debug builds: **Copy code**.
  - **Tap** (`sync.md` A.11, when NFC and Bluetooth are on): under the QR, "Or hold your phone to Wei Ming's" with a phone-to-phone icon. After the tap: "Tapped · waiting for Wei Ming to scan the seal and accept", and the phone moves to G5 by itself when the acceptance arrives.
- **G4 Scan acceptance.** The viewfinder.
  - **Error:** "This isn't Wei Ming's acceptance of this offer." / "Not a handover code." / "Allow the camera to scan codes." with **Open settings**.
- **G5 Done.** The parcel label stamped HANDED OVER / 13:02 / TO WEI MING, "Receipt saved · not synced yet" (or "synced"), and the small verify code.

### R Receive (`ui/receive/`, one route with steps)
- **R0 Incoming offer** (`ui/incoming/`, online only, `sync.md` A.10). A signal-yellow banner under the top bar on every screen except a handover: "**Siti Aminah is handing you C-12** · Scan its seal to accept" with **Open** and a countdown. The phone vibrates once when a new one arrives. Tapping it opens R2 for that offer (R1 is skipped).
  - **R2–R4** are the same, plus a text button **Decline** ("Declined. Siti still holds C-12.").
- **R1 Tap** (the RECEIVE tab, stepper TAP · SCAN SEAL · SIGN). The tap moment above: "HOLD TO THE GIVER'S PHONE". At the bottom **SCAN A QR CODE INSTEAD**. No camera runs until that's pressed.
- **R1 Scan the offer** (the fallback, stepper SCAN OFFER · SCAN SEAL · SIGN AND SHOW; **USE TAP INSTEAD** goes back). The scanner ("SCANNER · OFFER", the torch in its header) and "Point at the giver's code".
  - **Error:** "Not a handover code." / camera permission, as G4.
  - **Tap:** while R1 is open, "Or hold your phone to the giver's". A tap opens R2 at once with the same checks, and a "Linked to Siti's phone" mark appears when the Nearby link is up.
- **R2 Checks.** "Checking offline…" (under a second), then one line per check, each ✓ or ✗:
  1. "Signed by Ravi Kumar · key 3F:A9:12:7C"
  2. "Ravi Kumar holds C-12 · signed by Siti and Ravi at 14:29"
  3. "Made out to you"
  4. "Valid for 0:41"
  5. "Not used before"
  6. "Authorised · radiographer · valid to 30.06.2027" (for a return: "Authorised · store officer")

  If all pass, the button is **Scan the seal**.
  - **Refused (any ✗):** a red header **"This is a copy. It can't be used."** (or "This handover can't be accepted." when the code isn't a copy), the failed lines in red with reasons ("Made out to Tan Wei Ming, not you", "Expired 0:40 ago", "Already used on this phone", "Unknown signer. Sync this phone before accepting.", "You aren't authorised to hold sealed sources (lapsed 03.11.2026)", "Ravi Kumar already handed C-12 to Tan Wei Ming at 14:30. This would be a second handover.", "Ravi Kumar's phone was withdrawn by the safety officer at 14:40."), and "Nothing was signed."
- **R3 Scan the seal.** "Scan the seal on camera C-12".
  - **Match:** "Seal S-40117 matches C-12" → **Next**.
  - **Mismatch:** "Seal S-40999 doesn't match C-12 (S-40117). Don't accept this camera." with no way on.
- **R4 Accept.** The who-and-what card plus the statement "This is sealed source IR-0457 (iridium-192). You're liable for it the moment you sign." and the due time. The primary button **Accept**, with "Place your finger to accept".
  - **Error:** "Fingerprint cancelled. Nothing was signed. Ravi still holds C-12."
- **R5 Acceptance QR** (QR fallback only). The QR in the card, "Show this to Ravi", and the small verify code. After Ravi scans it, or on **Done**: "You hold C-12. Return by 14:31." and "Not synced yet".
  - **Online:** no QR. "Sent to Ravi", then Done. If the acceptance can't be sent: "Couldn't reach the server. Show this to Ravi instead." with the acceptance QR.
  - **Tapped:** no QR, straight to Done. If the link isn't up within 10 seconds: the acceptance QR with "Couldn't reach Siti's phone. Show this instead."

- **Done** (every path): the parcel label stamped RECEIVED / 13:02 / FROM RAVI, the sync line, the small verify code, and **DONE**, which returns to the first tab.

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

### O3 Devices (`ui/devices/`, the safety officer's DEVICES tab)
- **Purpose:** see every enrolled phone and withdraw a lost one (`sync.md` A.12).
- **Components:** one row per device: the person's name and role, "Key 3F:A9:12:7C · hardware-backed", "enrolled 6 Nov 23:51", and a status chip **In use** or **Withdrawn 14:40** (striped red). This phone's own row says "This phone" and has no action.
- **Action:** **Withdraw** on a row → a bottom sheet "Withdraw Ravi Kumar's phone? It stops working at once, and other phones refuse anything it signs from now on." → **Withdraw phone** (red) → `revoke_device` → the row turns Withdrawn.
- **Offline:** the list shows what last synced; **Withdraw** is disabled with "Withdrawing a phone needs the network."
- **Errors:** the server's reason in words ("Only the safety officer can withdraw a phone").

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
| `people` | Read-only (seed) | Seed only | `id uuid PK`, `name`, `role` (`store_officer`, `radiographer` or `rso`), `staff_no`, `nea_reg_no`, `enrol_code_hash` (never readable), `active`, `authorised_until_ms` |
| `devices` | Append-only | The server's `enrol_device`, at enrolment | `id uuid PK`, `person_id FK people`, `public_key`, `key_fp`, `key_security` |
| `items` | Read-only (seed) | Seed only | `id uuid PK`, `code` (C-12), `source_serial`, `isotope`, `seal_no`, `store_name` |
| `handovers` | Append-only, **with clash rules** | Phones (the giver makes the id) | Signed fields: item, kind, previous record, giver and receiver (person and device), seal, due, one-time number, times, both signatures, record hash. Server fields: `status`, `status_reason`. |
| `alerts` | Server-written only | `sync_push` | `id`, `kind` (fork, seal_mismatch, orphan or rejected), `item_id`, `person_id` (the person named), `handover_ids`, `reason`, `resolution_id` |
| `resolutions` | Append-only, with a clash rule | The safety officer's phone | `alert_id`, `chosen_handover_id`, `note`, signature |
| `missing_reports` | Append-only | Phones | `item_id`, `last_handover_id`, `note` |
| `enrol_attempts` | Server only, never pulled | `enrol_device` | `uid`, `staff_no`, `ok`, `at` |

Every server table also has `version`, `server_seq`, `deleted_at`, `performed_at`, `received_at` and `created_by`, as the kit requires. Because every table is append-only or server-written, nothing is ever edited by a phone, so there are no base-version conflicts and no deletes.

**Conflict rules for editable tables:** none. There are no editable tables.

**Clash rules the server checks** (`sync.md` §C):
- **`handovers`:**
  - a camera has one holder at a time
  - the giver must be the holder of the previous record
  - a one-time number is used once
  - both devices must be on the roster and belong to the named people
  - the roles must fit the kind
  - whoever receives an issue or transfer must be active and authorised when they accept
  - the hashes must link

  A second handover from the same previous record is stored as **forked**, with an alert naming the giver. A wrong seal is accepted with an alert. A record whose previous record hasn't arrived is held as an **orphan** and checked again later. Everything else that breaks a rule is **rejected** with a reason, logged as an alert, never dropped silently.
- **`resolutions`:** only a safety officer can decide, and only once per alert. The decision makes the chosen branch accepted and the other overruled.
- **`devices`:** only `enrol_device` adds them. The staff number must belong to an active person and the enrolment code must match. Five wrong codes in 10 minutes, for one staff number or one session, lock enrolment for 10 minutes.

**Who may read and write:** only an enrolled session may read, and then it may read every table (it's one team, and phones need the roster and the chains). An enrolled session is a Supabase session that has enrolled a device for an active person. A session that hasn't enrolled reads nothing, and `sync_push` and `sync_pull` refuse it with "not enrolled". No phone may insert, update or delete directly. Every write goes through `sync_push` or `enrol_device` (both security definer), which check the rules and set `created_by = auth.uid()`. `people` and `items` change only through migrations and seed.

**Sign-in:** each phone signs in anonymously once while online and keeps its session. That session is only a pipe: it gets access by enrolling with a staff number and enrolment code. The phone's public key on the roster, not the Supabase account, is what identifies a person in a signature, and the same key unlocks the app offline.

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
- unlocking the app, and every authorisation check, for 72 hours after the last successful sync

**Needs the network:**
- the first enrolment (the code check, the public key upload, then the roster download)
- renewing the 72-hour offline lease (any successful sync)
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
- after the Sudden Dev change: the access migration, `auth/AccessRules.kt`, `auth/AppLock.kt`, `auth/Unlocker.kt`, `data/remote/EnrolApi.kt`, `EnrolRepository.enrol`, and the Lock gate in `AppScaffold` with a placeholder `LockScreen`

| # | Card | Owns | Depends on | Wave | Devin mode |
|---|---|---|---|---|---|
| 01 | Token core: keys, signing, checking, record hashes, match code, QR payloads, offer checks, `deriveCustody` | `crypto/`, `test/.../crypto/` | Plan | 1 | Fusion |
| 02 | Sync engine: push, pull, result handling, retries; un-ignores the sync tests | `sync/` (except the kickoff's monitor and scheduler), `test/.../sync/` | Plan | 1 | Fusion |
| 03 | Hand over, G1–G5 | `ui/give/`, `data/repo/GiveRepository.kt`, `GiveScreenshotTest.kt` | Plan (01 for real signing) | 1 | Normal |
| 04 | Receive, R1–R5, including the refused screen | `ui/receive/`, `data/repo/ReceiveRepository.kt`, `ReceiveScreenshotTest.kt` | Plan (01) | 1 | Normal |
| 05 | Store, My custody, timeline, overdue alarm | `ui/store/`, `ui/mycustody/`, `ui/timeline/`, `alarm/`, `data/repo/CustodyRepository.kt`, three screenshot tests | Plan (01 for `deriveCustody`) | 1 | Normal |
| 06 | Outbox, Alerts, Fork and the signed decision | `ui/outbox/`, `ui/alerts/`, `ui/fork/`, `data/repo/SafetyRepository.kt`, three screenshot tests | Plan (01, 02) | 1 | Normal |
| 07 | Access: Enrol with a code, Lock and unlock, Me with the access and key cards | `ui/enrol/`, `ui/lock/`, `ui/me/`, `EnrolScreenshotTest.kt`, `LockScreenshotTest.kt`, `MeScreenshotTest.kt` | Plan (01 for real keys) | 1 | Normal |
| 08 | Daily record (reg 26) and Report missing | `ui/dailyrecord/`, `ui/missing/`, their repositories and tests | 05 | 2 | Normal |
| 09 | Offline checks: a phone holding both branches warns, R2 refuses "already handed over", and withdrawn keys are refused | `crypto/OfferChecker.kt`, `crypto/Custody.kt`, `crypto/Roster.kt`, `ui/timeline/` | 01, 05 | overnight | Normal |
| 10 | Maestro flows for the demo path, and demo polish of copy and seed | `.maestro/`, `supabase/seed.sql` (via a pull request) | 03–07 | 2 | Lite |
| 11 | Radio check: a 4-digit challenge, a 6-digit answer | `ui/radiocheck/`, `crypto/RadioCheck.kt` | 01 | 3 | Normal |
| 12 | Signature check on the server, in an Edge Function before `sync_push` | `supabase/functions/` | 02 | 3 | Normal |
| 13 | Device revocation: the Devices tab, Withdraw, and wiping a withdrawn phone (server, Room and wiring by the build account) | `ui/devices/`, `data/repo/RevocationRepository.kt`, `ui/enrol/`, `ui/alerts/` reason text | 07 | overnight | Normal |
| 14 | Online hand over: post the offer through the relay, G3-online waiting, finish on the relayed acceptance, QR fallback | `ui/give/`, `data/repo/GiveRepository.kt`, `GiveScreenshotTest.kt` | relay (plan job, `sync.md` A.10), 03 | 2 | Normal |
| 15 | Online receive: the R0 banner, R2–R4 from a relayed offer, Decline, send the acceptance, R5-online | `ui/incoming/`, `ui/receive/`, `data/repo/ReceiveRepository.kt`, `ReceiveScreenshotTest.kt` | relay, 04 | 2 | Normal |
| 16 | Tap link: NFC card emulation and reader, the `CS1T:` payload, Nearby Connections, a Debug tap test | `nearby/`, `ui/debug/`, `test/.../nearby/` | plan (A.11) | 2 | Fusion |
| 17 | Tap in the handover: G3 and R1 offer the tap, R5 and G5 finish over the link, QR fallback | `ui/give/`, `ui/receive/`, their tests | 14, 15, 16 | 3 | Normal |

Cut to make room for the Sudden Dev change: the old card 08 (enrolment code, key details) is folded into 07; encrypted offers are a stretch idea only. Handover over NFC and Nearby Connections is cards 16 and 17.

Overnight wave: polish and the QA sweep (`role-build.md`).

---

## 8. Demo data

**People** (seed; enrolment codes are typed on Enrol and stored as SHA-256 hashes; "authorised until" is end of day, Singapore time):

| Name | Role | Staff no | NEA reg (dummy) | Enrolment code | Authorised until |
|---|---|---|---|---|---|
| Siti Aminah | Store officer | S-1021 | (none) | 418203 | no expiry |
| Ravi Kumar | Radiographer | R-2210 | RW-DEMO-1452 | 552917 | 30 Jun 2027 |
| Tan Wei Ming | Radiographer | R-2234 | RW-DEMO-1478 | 730164 | 30 Jun 2027 |
| Jun Lim | Radiographer | R-2251 | RW-DEMO-1503 | 286540 | **3 Nov 2026 (lapsed)** |
| Dr Tan Mei Ling | Radiation safety officer | O-3001 | (none) | 904372 | no expiry |

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
- an encrypted local database (SQLCipher), `FLAG_SECURE` and remote wipe (named in the pitch as production settings)
- device revocation (card 13 is a stretch)
- several people sharing one phone, each with their own sign-in
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
| The lock gets in the way during the demo | It locks only on a cold start or when the screen turns off, and the QR screens keep the screen on. The safety officer's emulator has a PIN and stays awake. |
| A reinstalled app can't sync ("not enrolled") | A reinstall gets a new anonymous session, so enrol again with the code. The server allows one person several devices. |
| A phone hasn't synced for 72 hours | It opens read-only. Connect once: any successful sync renews the lease. |
