# 33: The QR camera stays on while you scroll

Wave: 8 (final fixes) · Devin mode: Normal · Depends on: none

## Goal
On every QR screen, scrolling the page up or down leaves the camera running. Today on a phone, scrolling up on the Receive tab's QR scanner makes the preview flash black and restart, again and again. Card 26 (the TextureView fix) didn't cure it, because the cause is somewhere else.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/AppScaffold.kt (the pager and the Receive tab's flags only)
- app/src/main/java/com/nushhack26/app/ui/receive/ReceiveScreen.kt
- app/src/main/java/com/nushhack26/app/ui/components/QrScanner.kt (only if needed)

## The likely cause (confirm it first)
- The tabs sit in a `HorizontalPager` in `AppScaffold`. Each page gets `active = onHome && pagerState.settledPage == page && !pagerState.isScrollInProgress`.
- `ReceiveScreen` passes that straight through as the camera switch: `cameraOn = active`. When it's false, the QR window shows no camera, so `CameraPreview` leaves composition and its `DisposableEffect` unbinds CameraX. When it's true again, it binds again.
- Before an offer arrives (R1, including **SCAN A QR CODE INSTEAD**), `receiveInFlow` is false, so the pager still takes swipes (`userScrollEnabled = !receiveInFlow`).
- A vertical drag on the page is never perfectly vertical. The pager picks up the sideways part, `isScrollInProgress` turns true for a moment, `active` drops, and the camera is torn down and rebound. That's the flash, once per drag.
- **First prove it**: in a debug build, log each `bindToLifecycle` and `unbindAll` in `CameraPreview` and each change of `active` and `pagerState.isScrollInProgress`. Then drag up and down on R1's QR scanner. If binds track the drags, this is the cause. If not, find what's recomposing `CameraPreview` out and fix that instead, and say so in the PR.

## Build
1. **Keep the camera bound through pager wobble.** Drive the Receive tab's camera from whether the pager is on the Receive page (`pagerState.currentPage == page`, plus `onHome`), not from `isScrollInProgress`. Keep `active` exactly as it is for everything else that uses it (tap-to-receive arming, `onR1`), so NFC and the tap pulse behave as before.
   - The simplest shape: give `TabPage` and `ReceiveScreen` a separate `cameraActive` flag, defaulting to `active`, and pass it to `cameraOn`. Keep every other parameter of `ReceiveScreen` as it is.
2. **Don't let the pager steal vertical drags while the camera shows.** While the Receive tab is showing the camera (R1 in QR mode, or R3 Scan seal), turn off the pager's user scroll. Then a drag on the scanner only scrolls the page. Tabs still swipe everywhere else, and the bottom bar still switches tabs.
3. **Make `CameraPreview` cheap to keep.** Check that nothing above it (an `AnimatedContent` content key, a `key()`, or a `remember` keyed on state that changes while scanning) re-creates it during a scroll. It should bind once when the window appears and unbind once when it goes.
4. Keep `QrScanner`'s signature, its look, the TextureView (`COMPATIBLE`) mode and `clipToBounds` from card 26.

## Offline behaviour
- Pure UI: the camera and the checks run on the phone, so nothing changes offline.

## Acceptance criteria (checked on a phone)
- [ ] Receive tab → **SCAN A QR CODE INSTEAD**: drag up and down repeatedly over the scanner and the text. The preview never goes black or restarts, and logcat shows one bind for the whole visit.
- [ ] The same on R3 **Scan the seal**, and on Give's QR fallback (G4 **Scan acceptance**).
- [ ] Scanning still works straight after scrolling: the C-12 seal locks in cyan.
- [ ] Away from the camera, swiping between tabs still works, and the bottom bar always works.
- [ ] Tap to receive is unchanged: on R1 (not in QR mode), holding a giver's phone still links them.
- [ ] Leaving the Receive tab, or locking the phone, turns the camera off (the privacy dot goes away).

## Test steps
1. Receive tab → **SCAN A QR CODE INSTEAD** → drag up and down for 10 seconds.
2. Scan a C-12 seal sticker straight away.
3. Swipe to another tab, then back, and check the camera comes back once.
4. Repeat on G4 through Give's **SHOW QR CODE INSTEAD** → **Scan acceptance**.

## Notes
- **Shared files changed**: `AppScaffold.kt`, minimally (the camera flag and the pager's `userScrollEnabled`). List it in the PR.
- **Screenshots**: not needed. The camera doesn't run in screenshot tests, and nothing they draw changes.
- **Data**: none.
