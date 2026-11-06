# Android theme (Jetpack Compose)

Starter theme files generated from `../tokens.json`, for `app/src/main/java/com/nushhack26/app/ui/theme/`. They follow the brand book (`../brand.md`); the component guidelines in `../components/*/README.md` say how to build each piece.

| File | What it holds |
| --- | --- |
| `Color.kt` | `TenetColors` (every colour role), `FieldColors` (dark) and `DaylightColors` (light), and the mapping onto a Material 3 `ColorScheme` |
| `Type.kt` | `SpaceMono`, `SpaceGrotesk`, `TenetType` (every type style) and `TenetTypography` |
| `Shape.kt` | `TenetShapes` (all square), `PrimaryButtonShape` and `ChipCutShape` (the 45° cuts) |
| `Dimens.kt` | `TenetDimens` (spacing, sizes, strokes in dp) and `TenetAngles` |
| `Patterns.kt` | `Modifier.hazardStripes`, `Modifier.hatching` and `Modifier.verticalText` |
| `Theme.kt` | `TenetTheme { }` and `Tenet.colors` |

## Setup

1. Copy the five `.ttf` files from `../fonts/` into `app/src/main/res/font/`.
2. Copy these `.kt` files into the theme package. If your package isn't `com.nushhack26.app`, change the `package` line and the `R` import in `Type.kt`.
3. Add `androidx.compose.material:material-icons-extended` for `Icons.Sharp.*`.
4. Wrap the app: `setContent { TenetTheme { AppNavHost() } }`.

5. For the launcher icon, copy `../logo/android/res/` into `app/src/main/res/` (see `../logo/README.md`).

## Rules worth repeating

- Space Mono text is always uppercase: call `.uppercase()` on the string.
- Every shape is square except the primary button and the Synced and Fork chips.
- Use `Tenet.colors` for roles Material 3 has no slot for: `signalEdge`, `signalText`, `alertText`, `pending`, `focus`, `inverseOk`, `qrGround`, `qrInk`.
- QR panels are always `qrInk` on `qrGround`, in both themes.
- Field (dark) is the signature theme. Turn off dynamic colour.

These files were generated, not compiled here: build once and fix any version mismatch (the `surfaceContainer*` roles need Material 3 1.2 or newer).
