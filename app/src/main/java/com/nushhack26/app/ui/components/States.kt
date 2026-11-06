package com.nushhack26.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Inventory2
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

/**
 * What a list shows when there's nothing in it yet (design/components/EmptyState): a dashed
 * box, an icon for the kind of list, the title in uppercase mono, a sentence, and at most one
 * secondary action. Never the Class 7 diamond, never a primary button.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Sharp.Inventory2,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Tenet.colors
    CenteredMessage(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(colors.lineStrong)
                .padding(TenetDimens.space8),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(TenetDimens.space12))
            Text(title.uppercase(), style = TenetType.title, color = colors.ink, textAlign = TextAlign.Center)
            if (message != null) {
                Text(message, style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center)
            }
            if (actionLabel != null && onAction != null) {
                SecondaryButton(actionLabel, onClick = onAction)
            }
        }
    }
}

/** Local data loads instantly; use this only for something that really waits (design: LoadingSkeleton). */
@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Loading") {
    val colors = Tenet.colors
    CenteredMessage(modifier) {
        CircularProgressIndicator(color = colors.signal)
        Text(message.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
    }
}

/** Something really failed. Say what, in words, and offer one way on. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = Tenet.colors
    CenteredMessage(modifier) {
        Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText, modifier = Modifier.size(TenetDimens.space12))
        Text("CAN'T SHOW THIS", style = TenetType.title, color = colors.alertText)
        Text(message, style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center)
        if (onRetry != null) {
            SecondaryButton("Try again", onClick = onRetry)
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(TenetDimens.space6),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
