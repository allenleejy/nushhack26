# 26: The camera preview stays inside the scanner when the page scrolls

Wave: 6 (bug fixes) · Devin mode: Lite · Depends on: none

## Goal
On every QR page (G4 scan acceptance, R1 in QR mode, R3 scan seal), scrolling the page moves the camera preview with the scanner bezel and never draws it outside its window.

## Files owned
- app/src/main/java/com/nushhack26/app/ui/components/QrScanner.kt

## Build
**The cause:** `CameraPreview` creates `PreviewView` with its default implementation mode, PERFORMANCE, which uses a `SurfaceView`. A `SurfaceView` is drawn in its own window layer. It doesn't move with a Compose `verticalScroll` (GiveScreen and ReceiveScreen both scroll) and isn't clipped by the bezel, so the picture lags, tears or spills over the header and the text when the page scrolls.

**The fix:**
- Create the view with `implementationMode = PreviewView.ImplementationMode.COMPATIBLE`, which uses a `TextureView`. It's drawn as part of the normal view tree, so it scrolls and clips like everything else.
- Clip the `AndroidView` to the window (`Modifier.clipToBounds()`), so nothing of the preview draws over the header or the hazard band.
- Keep `scaleType = FILL_CENTER`, and keep the scan-line and bracket overlay drawn over the preview as now.

Don't change `QrScanner`'s signature or anything else in the scanner.

## Offline behaviour
- Pure UI.

## Acceptance criteria (checked on a phone)
- [ ] R3 Scan seal: scroll the page up and down while the camera runs. The picture moves with the bezel, stays inside the window, and never covers the header, the hazard band or the hint.
- [ ] The same on G4 (QR fallback) and on R1 in QR mode.
- [ ] Scanning still works after scrolling: the C-12 seal locks in cyan.
- [ ] Rotating the camera feed or reopening the page shows no black or stretched preview.
- [ ] It works on both the Redmi Note 11 and the Galaxy (Xiaomi phones were the worst for SurfaceView glitches).

## Test steps
1. Receive → SCAN A QR CODE INSTEAD, then scroll while the camera shows.
2. Complete a seal scan, then do the same on Give's QR fallback.

## Notes
- **Screenshots:** none needed. The camera doesn't run in screenshot tests, and nothing they draw changes.
- **Data:** none.
