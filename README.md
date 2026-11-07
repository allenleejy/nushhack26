# Tenet

**Signed chain of custody for radiography cameras, done on the phone with no network needed.**

At an industrial radiography company, each camera holds a sealed radioactive source, and someone has to be liable for it at all times. Additionally, the movement of the camera needs to be tracked so that there is traceability when the cameras are lost. When a camera changes hands, the giver and the receiver both sign the handover on their phones. The phones swap the handover with a tap (NFC, then Bluetooth) or a QR code when offline, or via the internet when both are online. The receiver's phone checks every signature, the seal and the receiver's authorisation itself, with a two-way handshake to ensure both phones acknowledge successful transfer. When the phones reconnect, the server rebuilds one checked chain and is able to identify invalid transfers such as if someone has a hacked phone, and transfers a camera they do not have.

- Native Android: Kotlin, Jetpack Compose, Material 3
- Room holds the phone's copy of the data. Once online, WorkManager syncs every change in the outbox.
- Supabase (Postgres and anonymous sign-in) is the server. Every write goes through checked `security definer` functions (`sync_push`, `enrol_device`).
- Each phone has one EC P-256 key in the Android Keystore, locked behind the person's fingerprint or PIN. That key signs handovers and allows the app to be logged into offline.

The design docs: `docs/concept.md` (the idea), `docs/spec.md` (screens and the demo), `docs/sync.md` (record formats and server rules).

---

## Contents

1. [What you need](#1-what-you-need)
2. [Server setup (Supabase)](#2-server-setup-supabase)
3. [Build and install the app](#3-build-and-install-the-app)
4. [Prepare the phones](#4-prepare-the-phones)
5. [Demo](#5-demo)
6. [More things to try](#6-more-things-to-try)
7. [Reset between runs](#7-reset-between-runs)
8. [Troubleshooting](#8-troubleshooting)
9. [For developers](#9-for-developers)

---

## 1. What you need

**To run the demo:**
- **Three Android phones**, Android 8.0 (API 26) or newer. Each one needs:
  - a secure lock screen (fingerprint, or at least a PIN): release builds won't enrol without one
  - a camera, to scan the seal and the QR fallback
  - NFC and Bluetooth for the tap handover (QR code transfer is available for phones without NFC)
- **Internet** for setup and for the online steps. 
- **Seal QR codes sheet:** open `docs/demo/seals.html` in a browser for the seal QR codes.

**To build from source:**
- JDK 17 or newer (Android Studio's bundled `jbr` works)
- The Android SDK with **platform 37** (`compileSdk 37`)
- A Supabase project, set up as in section 2

---

## 2. Server setup (Supabase)

| Person | Role | Staff no | Enrolment code | Authorised until |
|---|---|---|---|---|
| Siti Aminah | Store officer | `S-1021` | `418203` | no expiry |
| Ravi Kumar | Radiographer | `R-2210` | `552917` | 30.06.2027 |
| Tan Wei Ming | Radiographer | `R-2234` | `730164` | 30.06.2027 |
| Jun Lim | Radiographer | `R-2251` | `286540` | **03.11.2026 (lapsed)** |
| Dr Tan Mei Ling | Radiation safety officer | `O-3001` | `904372` | no expiry |

| Camera | Code | Source | Isotope | Seal |
|---|---|---|---|---|
| Kestrel | C-12 | IR-0457 | Ir-192 | S-40117 |
| Osprey | C-07 | IR-0391 | Ir-192 | S-40102 |
| Heron | C-15 | SE-0112 | Se-75 | S-40125 |

Jun Lim's authorisation is lapsed.

---

## 3. Build and install the app

Download the newest `app-rNN.apk` from the repo's **Releases** page (`r01`, `r02`, …) and install it on each phone, either by opening it on the phone (allow installs from that source when asked) or over USB:

```bash
adb install -r app-rNN.apk
```

## 4. Prepare the phones

For each phone:
1. **Lock screen:** set a fingerprint (preferred) or a PIN in the phone's Settings.
2. **USB debugging** (for `adb`, scrcpy and the prep script): Settings → About phone → tap **Build number** 7 times, then Developer options → **USB debugging** on. Plug in the phone and accept the prompt.
4. **Turn on NFC and Bluetooth** in quick settings.
5. **Check every phone at once:** with all phones plugged in, run
   ```bash
   bash scripts/demo-prep.sh                    # one line per phone
   bash scripts/demo-prep.sh --install app.apk  # also installs that APK on each phone
   ```
   Each line should show Tenet's version, `airplane keeps BT/NFC`, `NFC on` and `BT on`.
6. **Enrol each phone as its person** (the phone must be online):
   - Open Tenet. It shows **JOIN YOUR TEAM**.
   - Type the staff number and enrolment code from the table in section 2, then tap **Create my signing key** and confirm with the fingerprint.
   - The phone creates its hardware key, the server checks the code, and the phone opens on that person's home screen. **Me** shows "Enrolled as … · key 3F:A9:…".
7. **Sync every phone once while online**, so each phone has everyone's public key on its roster: tap the sync chip in the top bar, or Me → **Sync now**.

---

## 5. Demo

One camera, **Kestrel (C-12)**, goes from the store to a radiographer, from that radiographer to a colleague **with no network at all**, and back to the store. At the end, the store officer's phone shows the whole chain of custody, from the radiographer to his colleague and back to the store. It takes about 3 minutes.

### The phones

| Phone | Person | Enrol with | Role | Network during the demo |
|---|---|---|---|---|
| **A** | Siti Aminah | `S-1021` / `418203` | Store officer | Online throughout |
| **B** | Ravi Kumar | `R-2210` / `552917` | Radiographer | Online for step 1, then airplane mode until the very end |
| **C** | Tan Wei Ming | `R-2234` / `730164` | Radiographer | Online at the start, airplane mode for step 2, then back online |

If phone B or C has no NFC, use QR codes instead of tap for step 2 (see the note).

### Before you start (about 5 minutes, all online)

1. Reset the server to the starting state (section 7). On each phone: **Me** → tap the version number 5 times to open **Debug** → **Clear local data** → **Sync now**.
3. On A, the **ITEMS** tab shows Kestrel (C-12), Osprey (C-07) and Heron (C-15), all **In store**.
4. In Debug on every phone, **Tampered phone** is **off**.

### Step 1: Issue, online (A → B)

- **A (Siti):** ITEMS → Kestrel → **Issue**.
  - The list shows **Jun Lim greyed out: "authorisation lapsed 03.11.2026"**. The phone checks authorisation itself, so this works offline too.
  - Pick **Ravi Kumar** → due chip **+5 MIN** → **Sign** → fingerprint. A shows "Sent to Ravi's phone".
- **B (Ravi):** a yellow banner appears: "Siti Aminah is handing you Kestrel". Tap **Open**.
  - The checklist turns green: signed by Siti, Siti holds Kestrel, intended recipient, still valid, not used before, authorised.
  - **Scan the seal** from the html file → "Seal S-40117 matches C-12" → **Accept** → fingerprint.
- A moves to **Done** by itself, and both phones print a custody receipt and stamp it.
- Wait until the sync bars on A and B show **All synced**.

### Step 2: Hand over to a colleague with no network (B → C)

- **Turn airplane mode on for B and C.** (Ravi and Wei Ming are in a shielded bay on site, with no signal at all.)
- **B (Ravi):** CUSTODY → Kestrel → **Hand over** → under "Hand to a colleague", pick **Tan Wei Ming** → **Sign** → fingerprint. B shows the sealed cube in an amber circle: **HOLD TO WEI MING'S PHONE**.
- **C (Wei Ming):** open the **RECEIVE** tab. Hold B and C back to back until both buzz.
  - Both screens flash. C's checklist turns green **with no network**: signed by Ravi; Ravi holds Kestrel, signed by Siti and Ravi; intended recipient; valid; not used before; authorised.
  - **Scan the seal** → **Accept** → fingerprint.
- B moves to **Done** by itself over the phone-to-phone link.
- **A (Siti)** still shows Kestrel with Ravi. That's correct: the store doesn't know yet, and it says so, because nothing has reached the server.

> **Note for no NFC:** On B, tap **SHOW QR CODE INSTEAD**. On C, tap **SCAN A QR CODE INSTEAD** and scan B's screen, scan the seal, and accept. C then shows an acceptance QR. On B, tap **Scan acceptance** and scan it. The technical process is the same, using QR codes to transfer information both ways instead.

The proof that Ravi really held the camera travels inside the offer, signed by Siti and Ravi, and Wei Ming's phone checks both signatures itself.

### Step 3: Wei Ming comes back online

- **C:** airplane mode **off** → tap the sync chip (or **Sync now**). C's sync bar shows **All synced**.
- **A (Siti):** Kestrel now shows **with Tan Wei Ming**. C carried the handover that Ravi signed offline. Ravi's phone is still offline, and that doesn't matter.

### Step 4: Return to store, online (C → A)

- **C (Wei Ming):** CUSTODY → Kestrel → **Hand over** → under "Return to store", pick **Siti Aminah** → **Sign** → fingerprint. C shows "Sent to Siti's phone".
- **A (Siti):** the yellow banner "Tan Wei Ming is handing you Kestrel" → **Open**. The checklist turns green (Wei Ming holds Kestrel, signed by Ravi and Wei Ming) → **Scan the seal** → **Accept** → fingerprint.
- C moves to **Done** by itself. On A, ITEMS shows Kestrel **In store**.

### Step 5: The chain of custody

- **A (Siti):** Kestrel → **Timeline**, oldest first:
  1. Registered to Siti Aminah · Store register
  2. Issued to Ravi Kumar · From Siti Aminah · both signed ✓
  3. Handed to Tan Wei Ming · From Ravi Kumar · both signed ✓ (signed offline)
  4. Returned to Siti Aminah · From Tan Wei Ming · both signed ✓
- Tap any entry to open its receipt, with both signers' keys and **SIGNATURES 2 OF 2 VERIFIED**.
- **B (Ravi):** airplane mode **off** → **Sync now**. B sends its copy of the offline handover again, and it shows as synced. **A's timeline still has exactly four entries.**

The demo covers three devices, three handovers and six fingerprints, one handover made offline.

### Configuration

| Name | Where | Needed for |
|---|---|---|
| `SUPABASE_URL` | `local.properties` or an environment variable | Syncing |
| `SUPABASE_PUBLISHABLE_KEY` | `local.properties` or an environment variable | Syncing |
| `DEMO_RESET_TOKEN` | `local.properties` | `reset-demo.bat` only. The server stores only its SHA-256, in `demo_secrets`. |

### Layout

```
app/src/main/java/com/nushhack26/app/
  App.kt, AppContainer.kt   start-up and wiring (no DI framework)
  auth/                     offline unlock, the 72-hour lease, who may do what
  crypto/                   device keys, signing, canonical records, QR payloads, offline checks
  nearby/                   tap to hand over: NFC card emulation and reader, Nearby Connections
  data/local/               Room database, entities, DAOs (outbox, sync_state)
  data/remote/              Supabase client, session, enrol, relay and revocation calls
  data/repo/                one repository per feature
  sync/                     network monitor, sync engine, worker and scheduler, sync status
  alarm/                    the overdue alarm
  ui/                       theme, shared components, navigation, one package per screen
app/src/test/               Robolectric tests and Roborazzi screenshot tests
.maestro/                   phone test flows
supabase/                   migrations, seed data and the demo reset
design/                     the Tenet design system (tokens, components, logo, fonts)
docs/                       concept, spec, sync protocol, printable seals (docs/demo/)
cards/                      task cards for Devin
hackathon-kit/              how the team works: roles, playbook, stack
```
