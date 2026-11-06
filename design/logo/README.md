# Logos

The Tenet logo is the **Junction** mark plus the **TENET** wordmark (Space Mono Bold, tracked 0.12em, outlined). Every file is full colour unless named black or white.

| File | Use | Ink |
| --- | --- | --- |
| `tenet-lockup-field.svg` | Mark + wordmark on dark grounds (`bg-000`, slides, the splash screen in Field) | crossbar and letters #f4f1e8, stem and junction #f2b81f |
| `tenet-lockup-daylight.svg` | Mark + wordmark on light grounds | crossbar, stem outline and letters #0e0f11, junction #f2b81f |
| `tenet-icon-field.svg` | The mark alone on dark grounds | as the Field lockup |
| `tenet-icon-daylight.svg` | The mark alone on light grounds | as the Daylight lockup |
| `tenet-wordmark-field.svg` | Wordmark alone on dark grounds (the top app bar title on brand screens) | #f4f1e8 |
| `tenet-wordmark-daylight.svg` | Wordmark alone on light grounds | #0e0f11 |
| `tenet-icon-white.svg` | One colour, white, for photos or amber grounds | #ffffff |
| `tenet-icon-black.svg` | One colour, black, for print and stamps | #0e0f11 |
| `tenet-app-icon.svg`, `tenet-app-icon-512.png` | The square app tile for slides and listings (the Android launcher uses `android/` below) | mark on #262522 |

## Android launcher icon

`android/res/` holds an adaptive icon ready to copy into `app/src/main/res/`:

- `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: the adaptive icon (minSdk 26, so no PNG fallbacks are needed).
- `drawable/ic_launcher_foreground.xml`: the mark inside the 66dp safe zone of the 108dp canvas.
- `drawable/ic_launcher_monochrome.xml`: a white version for Android 13+ themed icons.
- `values/ic_launcher_background.xml`: `bg-000`, #262522.

Point `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` at them in the manifest, and delete the template's own `ic_launcher*` files so they don't clash.

Rules for using the logo are in `../brand.md`, under Logo.
