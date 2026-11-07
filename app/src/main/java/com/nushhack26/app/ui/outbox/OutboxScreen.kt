package com.nushhack26.app.ui.outbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ReceiptLong
import androidx.compose.material.icons.sharp.Outbox
import androidx.compose.material.icons.sharp.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.alerts.NoticeBanner
import com.nushhack26.app.ui.alerts.ScreenHeading
import com.nushhack26.app.ui.alerts.lastSyncText
import com.nushhack26.app.ui.components.EmptyState
import com.nushhack26.app.ui.components.ListDivider
import com.nushhack26.app.ui.components.ListRow
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.SquareList
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncChipState
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

const val OUTBOX_OFFLINE_TEXT = "Can't reach the server. Records stay on this phone."
const val OUTBOX_EMPTY_TEXT = "Every record has been synced."

sealed interface OutboxAction {
    data object SyncNow : OutboxAction
    data class OpenRecord(val handoverId: String) : OutboxAction
}

/** E4: what this phone still has to send (card 06). */
@Composable
fun OutboxScreen(
    container: AppContainer,
    /** Card 30: a handover row opens its receipt. */
    onOpenRecord: (handoverId: String) -> Unit = {},
) {
    val viewModel: OutboxViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OutboxViewModel(container.safetyRepository, container.syncStatus) { container.syncScheduler.syncNow() } }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    OutboxContent(state) { action ->
        when (action) {
            OutboxAction.SyncNow -> viewModel.onSyncNow()
            is OutboxAction.OpenRecord -> onOpenRecord(action.handoverId)
        }
    }
}

@Composable
fun OutboxContent(state: OutboxUiState, modifier: Modifier = Modifier, onAction: (OutboxAction) -> Unit = {}) {
    val colors = Tenet.colors
    val status = state.status
    Column(modifier.fillMaxSize().background(colors.bg000)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenHeading(
                    title = "Outbox",
                    meta = "${state.rows.size} waiting · ${lastSyncText(status.lastSyncAt)}",
                    modifier = Modifier.weight(1f),
                )
                SyncChip(
                    if (state.rows.isEmpty()) SyncChipState.Synced else SyncChipState.Waiting,
                    count = state.rows.size.takeIf { it > 0 },
                )
            }
            when {
                !status.isOnline -> NoticeBanner(OUTBOX_OFFLINE_TEXT)
                status.lastError != null && state.rows.isNotEmpty() ->
                    NoticeBanner("The last sync didn't finish: ${status.lastError}", problem = true)
            }
            when {
                state.loading -> Unit
                state.rows.isEmpty() -> EmptyState(
                    title = "Outbox empty",
                    message = "$OUTBOX_EMPTY_TEXT ${lastSyncText(status.lastSyncAt)}.",
                    icon = Icons.Sharp.Outbox,
                    modifier = Modifier.testTag("outbox_empty"),
                )
                else -> SquareList(Modifier.testTag("outbox_list")) {
                    state.rows.forEachIndexed { index, row ->
                        if (index > 0) ListDivider()
                        val handoverId = row.handoverId
                        val interaction = remember(row.opId) { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val open = if (handoverId != null) {
                            Modifier.heightIn(min = 48.dp)
                                .clickable(interactionSource = interaction, indication = null) { onAction(OutboxAction.OpenRecord(handoverId)) }
                                .pressGround(pressed, colors.bg200, colors.bg100)
                        } else {
                            Modifier
                        }
                        Column(Modifier.background(colors.bg100).then(open)) {
                            ListRow(
                                title = row.title,
                                supporting = row.supporting,
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                                        SyncChip(SyncChipState.Waiting)
                                        if (handoverId != null) {
                                            Icon(Icons.AutoMirrored.Sharp.ReceiptLong, contentDescription = "Open receipt", tint = colors.inkMuted, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                },
                            )
                            if (row.error != null) {
                                Text(
                                    row.error,
                                    style = TenetType.body,
                                    color = colors.alertText,
                                    modifier = Modifier.padding(start = TenetDimens.space4, end = TenetDimens.space4, bottom = TenetDimens.space3),
                                )
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton(
            label = "Sync now",
            icon = Icons.Sharp.Sync,
            onClick = { onAction(OutboxAction.SyncNow) },
            modifier = Modifier.padding(TenetDimens.space4).testTag("sync_now"),
        )
    }
}

/**
 * The square bg-200 press highlight, with no ripple, over a row whose [ListRow] paints its own
 * bg-100: darkens it in Daylight and lightens it at Night, leaving the text as it is.
 */
private fun Modifier.pressGround(pressed: Boolean, pressedColor: Color, restColor: Color): Modifier = drawWithContent {
    drawContent()
    if (pressed) {
        drawRect(pressedColor, blendMode = if (pressedColor.luminance() < restColor.luminance()) BlendMode.Darken else BlendMode.Lighten)
    }
}
