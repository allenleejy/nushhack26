package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

enum class DiamondVariant { Outlined, Filled, Alert }

/**
 * The Class 7 diamond (design/components/HazardDiamond): marks an item as a sealed source.
 * Outlined in lists and labels, Filled on the acceptance screen, Alert when overdue or a
 * seal doesn't match. Sizes: TenetDimens.diamondMd (48dp) or diamondLg (96dp).
 */
@Composable
fun HazardDiamond(
    modifier: Modifier = Modifier,
    variant: DiamondVariant = DiamondVariant.Outlined,
    size: Dp = TenetDimens.diamondMd,
) {
    val colors = Tenet.colors
    val fill = when (variant) {
        DiamondVariant.Outlined -> Color.Transparent
        DiamondVariant.Filled -> colors.signal
        DiamondVariant.Alert -> colors.alert
    }
    val edge = when (variant) {
        DiamondVariant.Outlined -> colors.signalText
        DiamondVariant.Filled -> colors.signalEdge
        DiamondVariant.Alert -> colors.alert
    }
    val figure = when (variant) {
        DiamondVariant.Outlined -> colors.signalText
        DiamondVariant.Filled -> colors.onSignal
        DiamondVariant.Alert -> colors.onAlert
    }
    Box(
        modifier = modifier.size(size).semantics { contentDescription = "Sealed source, class 7" },
        contentAlignment = Alignment.Center,
    ) {
        // A square whose diagonal is the box's side, turned 45°.
        Box(
            Modifier
                .size(size / 1.4142f)
                .rotate(45f)
                .background(fill)
                .border(TenetDimens.strokeControl, edge),
        )
        Text("7", style = TenetType.label.copy(fontSize = (size.value * 0.36f).sp, lineHeight = (size.value * 0.4f).sp), color = figure)
    }
}
