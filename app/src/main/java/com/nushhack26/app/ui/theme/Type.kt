// Generated from design/tokens.json (Tenet design system). Edit the tokens, not this file.
package com.nushhack26.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nushhack26.app.R

// Copy design/fonts/*.ttf into app/src/main/res/font/ (names are already valid resource names).
val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/**
 * The type scale. Space Mono styles are ALWAYS shown uppercase: Compose has no text-transform,
 * so call `.uppercase()` on the string (for example `Text("Sign and show QR".uppercase(), style = TenetType.label)`).
 * Space Grotesk styles are sentence case.
 */
object TenetType {
    /** Uppercase. The item ID on the custody card and the handover confirmation. One per screen. */
    val readoutXl = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 64.sp)
    /** Uppercase. Screen headline: what this screen is for. M3 headlineMedium. */
    val headline = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)
    /** Uppercase. Top app bar title, card and alert headers, sheet titles. M3 titleLarge. */
    val title = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.02.em)
    /** Uppercase. Button labels, tabs, list row first lines, names in the custody chain. M3 labelLarge. */
    val label = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.06.em)
    /** Uppercase. Values a person compares or reads aloud: item and source IDs, seal numbers, times, sequence numbers, short hashes. Bold (700) for the value that matters most in a group. */
    val record = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    /** Uppercase. Field labels above a value, chip text (bold), status lines, meta lines. Never a full sentence. M3 labelMedium. */
    val recordLabel = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
    /** The six-digit radio check code and the challenge. Digits in groups of three. */
    val codeLg = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.04.em)
    /** Sentence case. Instructions on the handover flow and alert explanations. M3 bodyLarge. */
    val bodyLg = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp)
    /** Sentence case. Default running text: banner messages, helper copy, empty states. Never smaller for a sentence. M3 bodyMedium. */
    val body = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    /** Sentence case. A name or fact to stress inside body text. */
    val bodyStrong = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp)
}

val TenetTypography = Typography(
    headlineMedium = TenetType.headline,
    titleLarge = TenetType.title,
    titleMedium = TenetType.label,
    labelLarge = TenetType.label,
    labelMedium = TenetType.recordLabel,
    bodyLarge = TenetType.bodyLg,
    bodyMedium = TenetType.body,
)
