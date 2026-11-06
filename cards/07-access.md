# 07: Access: Enrol with a code, the Lock screen, and Me

Wave: 1 · Devin mode: Normal · Depends on: the plan job (card 01 for real keys; until then `deviceKeys()` throws)

## Goal
Only authorised personnel can use the app, and proving who you are works offline. This is the hackathon's Sudden Development requirement, so it's in the demo. A new phone joins the team with a staff number and a one-time enrolment code. After that, the app opens only for that person: the Lock screen asks for their fingerprint, and the phone's hardware key signs a fresh challenge that's checked with no network. Me shows who the phone is, what they're authorised for, and how long it can keep working offline.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/enrol/
- app/src/main/java/com/nushhack26/app/ui/lock/
- app/src/main/java/com/nushhack26/app/ui/me/
- app/src/test/java/com/nushhack26/app/screenshots/EnrolScreenshotTest.kt, LockScreenshotTest.kt, MeScreenshotTest.kt

## Build
Follow `docs/spec.md` §3 for E1, L1 and E2, with that exact text, and `docs/sync.md` A.7–A.9, C.2 and D.4. The logic is already on `main`; this card builds the screens on top of it.
- **E1 Enrol** (`EnrolScreen`, which `AppScaffold` shows until the phone has an identity):
  - a staff number field and a 6-digit code field (number keyboard, hint "From your safety officer"); **Create my signing key** stays disabled until both are filled
  - on tap: `container.deviceKeys.createKey()`, then `SignPrompt` (it proves the key works and that the person is present), then `container.enrolRepository.enrol(staffNo, code, key, "r${BuildConfig.VERSION_CODE}")`
  - map every `EnrolOutcome` to the spec's text: `WrongCode`, `Locked`, `NoConnection`, `Failed` (show the reason), and `NoSecureLockException` from `createKey()` in release builds
  - `Enrolled`: the "Enrolled as …" card, then the app switches to the role's home by itself (the identity Flow changes). `EnrolRepository` has already unlocked the app and started a sync.
  - no roster is shown before enrolment: the server only lets enrolled phones read
- **L1 Lock** (`LockScreen(container, identity)`, which `AppScaffold` draws over the app while `container.appLock.locked` is true):
  - replace the working placeholder with the designed screen. Keep its behaviour: opaque, eats every touch, Back does nothing, and **UNLOCK** runs `SignPrompt` then `container.unlocker.verify()`. Never unlock any other way.
  - show the name and staff number, and the access line from `container.identity.lease` ("Access confirmed with the server 14:02 · valid offline until Mon 9 Nov 14:02")
  - errors: the spec's texts, including `UnlockResult.Refused.message`
- **Lease expired** (A.9): when `lease.expired`, show a red banner under Me's header and on the Lock screen with `AccessRules.LEASE_EXPIRED_TEXT`. Cards 03, 04 and 06 disable signing themselves.
- **E2 Me:**
  - name, role and staff number
  - **the access card:** `AccessRules.roleName(role)`, "valid to <date>" from the person's `authorisedUntilMs` (or "no expiry"), "access confirmed <time>" and "valid offline until <time>" from `identity.lease`, and **LOCK NOW** (`container.appLock.lock()`), hidden when `!container.appLock.lockApplies`
  - **the key card:** "Device key 3F:A9:12:7C · hardware-backed (TEE)" from `deviceKeys.keyInfo()`, or "Unprotected test key" for `software`
  - the sync card with **Sync now**, and the release version (5 taps → Debug, as now). Keep what the kickoff's `MeScreen` already does.
- **Screenshot tests:** Enrol (empty, wrong code, enrolled), Lock (normal, error, lease expired), Me (radiographer, safety officer, lease expired), each rendering the stateless `…Content`.

## Offline behaviour
- **Enrol needs the network once.** Offline, it shows "No connection. Enrolment needs the network once." and never creates a half identity.
- **Unlocking needs no network:** the signature is checked on the phone against the key saved at enrolment.
- **After 72 hours with no successful sync,** the app still unlocks and shows records, but signing is disabled until it syncs once.

## Acceptance criteria (checked on a phone)
- [ ] A fresh install online: staff number R-2210, code 000000 → "That code doesn't match R-2210. Ask your safety officer." Then code 552917 → fingerprint → "Enrolled as Ravi Kumar · radiographer · key …", and the app opens on My custody. The `devices` row is on Supabase.
- [ ] Five wrong codes → "Too many wrong codes. Try again in 10 minutes."
- [ ] On a phone with a fingerprint: press power, wake the phone, and Tenet shows LOCKED with the person's name. In airplane mode, **UNLOCK** → fingerprint → the app opens where it was (for example mid-way through My custody).
- [ ] A wrong finger is refused by the system prompt, and the app stays locked. Back does nothing on the Lock screen.
- [ ] Me shows the role, "valid to 30 Jun 2027", the last access confirmation and the offline limit, and **LOCK NOW** locks at once.
- [ ] On an emulator with no screen lock (debug build), the app never locks, and Me says "Unprotected test key".
- [ ] Loading, empty and error states are shown.
- [ ] It fits at 360 dp wide and at 1.3× font size.

## Test steps
1. Fresh install online: Enrol as Ravi (R-2210 / 552917). Codes for everyone are in `docs/spec.md` §8.
2. Airplane mode on, press power, wake the phone, unlock with the fingerprint.
3. Me → **LOCK NOW** → unlock again.

## Notes
- **Data:** `people`, `devices`, `sync_state` (identity, `enrolled_at`, `device_public_key`, `last_sync_at`).
- **Uses (already on `main`, don't change them):** `auth/AppLock.kt`, `auth/Unlocker.kt`, `auth/AccessRules.kt`, `data/repo/EnrolRepository.kt`, `data/remote/EnrolApi.kt`, `IdentityRepository` (`lease`, `leaseNow()`), `crypto/DeviceKeys.kt` (card 01), `SignPrompt.kt`, `AppScaffold`'s Lock overlay. If one of them needs a change, list it under "Shared files changed" in the pull request.
- **For the pitch:** "No network and no login server, yet only Wei Ming can open Wei Ming's phone. His fingerprint unlocks a key in the phone's secure chip, and the app checks that key's signature against his enrolment."
- **Design:** build from `design/` (the Tenet design system). Read `design/brand.md` first.
  - **Theme:** use `ui/theme/`: `TenetTheme`, `Tenet.colors`, `TenetType`, `TenetDimens`, `hazardStripes`, `hatching`, `verticalText`. Icons are `Icons.Sharp.*`. Space Mono text is uppercase (call `.uppercase()`), and sentences are Space Grotesk.
  - **Already built (reuse, don't rebuild):**
    - in `ui/components/`: `PrimaryButton`, `SecondaryButton`, `DangerButton`, `TextLinkButton`, `HazardDiamond`, `HandoverStepper`, `SealCheckRow`, `QrPanel`, `SyncChip`, `ListRow`, `SquareList`, `ListDivider`, `EmptyState`, `LoadingState`, `ErrorState`
    - in the shared app frame: the top app bar, the offline banner and the bottom nav
    - you can see them in `app/src/test/.../screenshots/ComponentsScreenshotTest.kt`
  - **Build the others in your own package,** following `design/components/<Name>/README.md`: `TextField` (the staff number and code). Use `HazardDiamond(Filled, diamondLg)` on the Lock screen, `PrimaryButton` (CREATE MY SIGNING KEY, UNLOCK, SYNC NOW) and `SecondaryButton` (LOCK NOW). Lock and Enrol are full screens with no tabs.
