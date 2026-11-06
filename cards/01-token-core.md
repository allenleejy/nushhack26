# 01: Token core: keys, signatures, record hashes, QR payloads, checks, custody

Wave: 1 · Devin mode: Fusion · Depends on: the plan job (the `crypto/` skeleton is on `main`)

## Goal
Every phone can create its fingerprint-locked signing key, sign and check handovers offline, turn records into QR codes and back, run the receiver's checks, and work out who holds each camera. Every other card calls this code. It has no screens.

## Files owned
- app/src/main/java/com/nushhack26/app/crypto/ (every file)
- app/src/test/java/com/nushhack26/app/crypto/

## Build
Fill in every `TODO()` in `crypto/`, following `docs/sync.md` §A exactly. Keep the public signatures the skeleton defines, because cards 03–06 already call them.
- **`DeviceKeys.kt`:**
  - `KeystoreDeviceKeys`: EC P-256, alias `countersign-device-key`, user authentication with a 10-second validity (A.1), StrongBox first, and `key_security` from `KeyInfo`.
  - `SoftwareDeviceKeys`: debug builds only, when `KeyguardManager.isDeviceSecure` is false. The key is stored in the app's files.
  - `deviceKeys(context)` picks between them.
  - `sign(text)` returns base64url DER. It throws `UserNotAuthenticatedException` if the user didn't authenticate within 10 seconds; callers show `SignPrompt` first.
- **`Signatures.kt`:** `verify(publicKey, text, sig)` with `SHA256withECDSA`; `keyFp(publicKey)` as `3F:A9:12:7C`.
- **`Canonical.kt`:** the offer, acceptance and resolution strings; `recordHash`; `registerHash`; `matchCode`; `sha256Hex`.
- **`HandoverBuilder.kt`:** build an unsigned offer from the head record (new id, 16-byte nonce, `expires_ms = offered_ms + 60000`, `due_ms` copied on transfer and empty on return), then attach the giver's signature. Build the receiver's half (to device, seal, accepted time), then attach the receiver's signature and compute `record_hash`.
- **`QrPayloads.kt`:** the `CS1O:`, `CS1A:` and `CS1S:` formats (A.3). `parse` returns a `ScannedCode`. An offer with a proof record must be **at most 1,100 characters**; use the shrinking tricks in A.3 if needed.
- **`OfferChecker.kt`:** the six checks in A.4, in order, each returning a `CheckLine` with the exact text from `docs/spec.md` §3 R2. `looksLikeCopy` is true when the only failures are the receiver, the expiry or the one-time number. Also `checkSeal` and `checkAcceptance`.
- **`Custody.kt`:** `deriveCustody`, exactly as A.6, including `serverView` mode and resolutions.

## Offline behaviour
- Everything here runs offline. Nothing in `crypto/` touches the network or Room.

## Acceptance criteria (checked on a phone)
- [ ] On a phone with a fingerprint, Debug → **Test signing** shows "OK · TEE" or "OK · StrongBox". A wrong finger gives a system "Not recognised", and nothing is signed.
- [ ] On an emulator with no screen lock (debug build), Test signing shows "OK · software".
- [ ] Unit tests pass for:
  - [ ] the test vectors below (the SQL migration computes the same hashes)
  - [ ] signing and checking with a software key; changing any one field makes the check fail
  - [ ] each offer check: valid, made out to someone else, expired, nonce already used, unknown signer, giver not the holder
  - [ ] an offer QR payload with a proof record of at most 1,100 characters, which parses back to equal records
  - [ ] `deriveCustody`: in store, out, overdue, tampered records hidden, fork Disputed, resolution picks the branch, and `serverView` ignoring unsynced records
- [ ] `quickCheck` passes.

## Test steps
1. `./gradlew quickCheck`
2. Install the debug build on a phone, then Me → tap the version 5 times → Debug → **Test signing**.
3. Repeat in airplane mode: same result.

## Notes
- **Test vectors.** Use them in a unit test. The SQL in `supabase/migrations/` must agree.
  - **Register record for C-12:**
    - id `c0000000-0000-4000-8000-000000000012`, item `b0000000-0000-4000-8000-000000000012`, to person `a0000000-0000-4000-8000-000000000001`
    - → `registerHash` = `eb882341f1e56ea7f674e5bc2c65855ae8ba5fe5e460d255c40795c870a5ebc4`
  - **An issue record:**
    - id `d0000000-0000-4000-8000-000000000001`, item `b0000000-0000-4000-8000-000000000012`, kind `issue`
    - prev `c0000000-0000-4000-8000-000000000012`, prev_hash the register hash above
    - from person `a0000000-0000-4000-8000-000000000001`, from device `e0000000-0000-4000-8000-000000000001`, to person `a0000000-0000-4000-8000-000000000002`
    - due `1794470400000`, nonce `AAECAwQFBgcICQoLDA0ODw`, offered `1794466800000`, expires `1794466860000`
    - giver_sig `GIVERSIG`, to device `e0000000-0000-4000-8000-000000000002`, seal `S-40117`, accepted `1794466830000`, receiver_sig `RECEIVERSIG`
    - → `record_hash` = `3ac4a29e2ac34f045ad932221b2c0e143bc32644546b768e9d406b79bd4a2993`, and the match code is `96 52 14`
- **Data:** `HandoverEntity`, `ResolutionEntity`, `PersonEntity`, `DeviceEntity`, `ItemEntity` (read only).
- No new dependencies are needed: `java.security` and `android.security.keystore`.
