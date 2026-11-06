# Fonts

| File | Family | Weight | Licence |
| --- | --- | --- | --- |
| `space_mono_regular.ttf` | Space Mono | 400 | SIL OFL 1.1 (`OFL-SpaceMono.txt`) |
| `space_mono_bold.ttf` | Space Mono | 700 | SIL OFL 1.1 |
| `space_grotesk_regular.ttf` | Space Grotesk | 400 | SIL OFL 1.1 (`OFL-SpaceGrotesk.txt`) |
| `space_grotesk_medium.ttf` | Space Grotesk | 500 | SIL OFL 1.1 |
| `space_grotesk_bold.ttf` | Space Grotesk | 700 | SIL OFL 1.1 |
| `material-symbols-sharp.woff2` | Material Symbols Sharp (web previews only) | variable | Apache 2.0 |

The Space Grotesk files are static instances cut from the official variable font. File names are already valid Android resource names: copy the five `.ttf` files into `app/src/main/res/font/` as they are. On Android, use icons from `androidx.compose.material:material-icons-extended` (`Icons.Sharp.*`), not the woff2.
