# 16: Tap link: NFC pairs the phones, Nearby Connections carries the answer

Wave: 2 · Devin mode: **Fusion** (its own session) · Depends on: the plan job (`docs/sync.md` A.11; the manifest, permissions, dependencies and stubs are on `main`)

## Goal
Two phones with no network swap a handover by touching once. The giver's phone acts as an NFC card holding the signed offer and a session key. The receiver's phone reads it, then the two connect over Nearby Connections (Bluetooth, upgraded to Wi-Fi when possible) so the acceptance can come back without a second tap. This card builds the transport and a Debug test that proves it on two real phones. Card 17 then puts it into the handover screens.

## Files owned
- app/src/main/java/com/nushhack26/app/nearby/ (`TapCardService.kt` and `TapLink.kt` are stubs to fill in; add the files you need)
- app/src/main/java/com/nushhack26/app/ui/debug/ (add the tap test only; keep everything else)
- app/src/test/java/com/nushhack26/app/nearby/

## Build
Follow `docs/sync.md` A.11 exactly: the payload, the AID, the APDUs, the endpoint names, the service id and the strategy.
- **`TapPayload`** (pure Kotlin, unit tested):
  - `encode(offer: String, key: ByteArray): String` and `parse(text): TapPayload?` for `CS1T:`
  - `giverName(key)` and `receiverName(key)`
  - `newKey()` (16 bytes from `SecureRandom`)
- **`TapApdu`** (pure, unit tested): the card's answer to SELECT, READ and other commands for a given payload, and the reader's loop that assembles the chunks. Test that a 1,400-character payload round-trips and that a wrong AID gets `6A82`.
- **`TapCardService`:** answers from `TapApdu` using the payload `TapLink` is currently serving. `6A82` when nothing is served. It runs in the app process, so share the current payload through a process-wide holder in `nearby/`.
- **`TapLink`:** keep the constructor `TapLink(context, scope)`; `AppContainer` already creates it as `container.tapLink`. The API that card 17 will use:
  - `val available: StateFlow<TapAvailability>`: `Ready`, `NfcOff`, `NoNfc`, `BluetoothOff` or `NeedsPermission`. Re-check it on resume.
  - `fun requiredPermissions(): Array<String>` for the current Android version.
  - `fun serve(offer: String): Flow<GiverTapEvent>`, for the giver.
    - While collected, it serves the payload over NFC and advertises on Nearby.
    - Events: `Tapped` (the reader finished reading), `Linked`, `Acceptance(text: String)`, `Failed(reason)`.
    - Cancelling the collection stops everything and makes a new key next time.
  - `@Composable fun rememberTapReader(enabled: Boolean, onTap: (TapPayload) -> Unit)`, for the receiver. It enables reader mode on the current activity while `enabled` and composed, and disables it on dispose.
  - `fun join(payload: TapPayload): ReceiverTapSession`, for the receiver. It starts discovery and connecting at once in `scope`.
    - `linked: StateFlow<Boolean>`
    - `suspend fun send(text: String, timeoutMs: Long = 10_000): Boolean`, which waits for the link up to the timeout, sends one BYTES payload, and returns whether Nearby confirmed it
    - `fun close()`
  - Nearby: `Nearby.getConnectionsClient(context)`, `SERVICE_ID`, `Strategy.P2P_POINT_TO_POINT`. Accept only the expected endpoint name on both sides, and reject others. Use `kotlinx-coroutines-play-services` `await()` for the Tasks.
- **Debug tap test** (in `ui/debug/`): a "Tap test" section.
  - **Serve test offer** serves `CS1O:test-<time>` and shows each `GiverTapEvent`.
  - **Read** turns the reader on, shows the payload when tapped, joins, and sends back `CS1A:test-<time>`.
  - It also shows `available`, plus a button that asks for `requiredPermissions()`.
  - This is how the card is checked on the phones before card 17.

## Offline behaviour
- Everything here works with no network: NFC, plus Bluetooth for Nearby.
- With airplane mode on, Bluetooth (and NFC on some phones) is off unless re-enabled. `available` must say so instead of failing silently. See A.11 "Airplane mode in the demo".

## Acceptance criteria (checked on two phones, both directions)
- [ ] Release build on the two Xiaomis: Debug → Tap test.
  - Phone A: **Serve test offer**. Phone B: **Read** and hold the backs together.
  - B shows the payload within 1 second, and A shows `Tapped`.
  - Within 10 seconds A shows `Linked`, then `Acceptance(CS1A:test-…)`.
- [ ] The same with the phones swapped (Android 16 serving, Android 13 reading).
- [ ] The same with mobile data and Wi-Fi off on both phones (Bluetooth and NFC on).
- [ ] With Bluetooth off, `available` says `BluetoothOff`; with NFC off, `NfcOff`. Nothing crashes.
- [ ] Leaving the Debug screen stops advertising and reader mode: a second phone can't read anything afterwards.
- [ ] `./gradlew quickCheck` passes, with the `TapPayload` and `TapApdu` unit tests.

## Test steps
1. Install the release APK on both phones (the build account does this in the merge round), and grant the permissions from the tap test.
2. Run the Debug tap test both ways, online and with the network off.
3. Note in the PR how long the tap read and the link took on each phone.

## Notes
- **Uses:** `play-services-nearby` and `kotlinx-coroutines-play-services` (already in `libs.versions.toml`), the permissions and the `TapCardService` entry in the manifest, and `res/xml/tap_apdu_service.xml` (all on `main`; don't change them, and list any change under "Shared files changed").
- **HCE on Xiaomi:** the card needs the screen on and the phone unlocked (`requireDeviceUnlock`). If the reader gets nothing, check NFC is on and that no wallet app is set to grab every tap. Say what you found in the PR.
- **No root** may be required: the Android 16 phone isn't rooted.
- Don't touch `ui/give/` or `ui/receive/`: cards 14 and 15 are working there in this wave.
