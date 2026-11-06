// Generated from design/tokens.json (Tenet design system). Edit the tokens, not this file.
package com.nushhack26.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Wrap the app in this. Field (dark) follows the system setting by default; pass darkTheme to override. */
@Composable
fun TenetTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) FieldColors else DaylightColors
    CompositionLocalProvider(LocalTenetColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = TenetTypography,
            shapes = TenetShapes,
            content = content,
        )
    }
}

/** Shortcut for the extra roles M3 has no slot for: `Tenet.colors.signalText`, `Tenet.colors.pending`... */
object Tenet {
    val colors: TenetColors
        @Composable @ReadOnlyComposable get() = LocalTenetColors.current
}
