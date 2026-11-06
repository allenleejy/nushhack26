// Generated from design/tokens.json (Tenet design system). Edit the tokens, not this file.
package com.nushhack26.app.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Square everywhere. Material 3 slots take a CornerBasedShape, so "square" is a 0dp corner. */
private val Square = RoundedCornerShape(0.dp)

val TenetShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)

/** The one exception: a 45° cut on the top-right corner of the primary button... */
val PrimaryButtonShape = CutCornerShape(topEnd = TenetDimens.cutMd)

/** ...and of the Synced and Fork chips. Nothing else is cut. */
val ChipCutShape = CutCornerShape(topEnd = TenetDimens.cutSm)
