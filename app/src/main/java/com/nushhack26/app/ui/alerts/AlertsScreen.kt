package com.nushhack26.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Block
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.GppBad
import androidx.compose.material.icons.sharp.HourglassTop
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nushhack26.app.AppContainer
import com.nushhack26.app.ui.components.PrimaryButton
import com.nushhack26.app.ui.components.dashedBorder
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.delay

const val ALERTS_POLL_MS = 10_000L

sealed interface AlertsAction {
    data class OpenFork(val alertId: String) : AlertsAction
    data class OpenItem(val itemId: String) : AlertsAction
}

/** O1: the safety officer's alerts, as the server knows them (card 06). */
@Composable
fun AlertsScreen(container: AppContainer, onOpenFork: (alertId: String) -> Unit, onOpenItem: (itemId: String) -> Unit) {
    val viewModel: AlertsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AlertsViewModel(container.safetyRepository, container.syncStatus, { container.syncScheduler.syncNow() }) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val online = state.status.isOnline
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(online, lifecycleOwner) {
        if (!online) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.onSyncNow()
                delay(ALERTS_POLL_MS)
            }
        }
    }
    AlertsContent(state) { action ->
        when (action) {
            is AlertsAction.OpenFork -> onOpenFork(action.alertId)
            is AlertsAction.OpenItem -> onOpenItem(action.itemId)
        }
    }
}

@Composable
fun AlertsContent(state: AlertsUiState, modifier: Modifier = Modifier, onAction: (AlertsAction) -> Unit = {}) {
    val colors = Tenet.colors
    val status = state.status
    val neverSynced = status.lastSyncAt == null
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.bg000).testTag("alerts_list"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TenetDimens.space4),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
    ) {
        item(key = "heading") {
            ScreenHeading(
                title = "Alerts",
                meta = "${state.cards.count { it !is AlertCard.Resolved }} open · ${lastSyncText(status.lastSyncAt)}",
            )
        }
        if (!status.isOnline || (status.lastError != null && neverSynced)) {
            item(key = "offline") { NoticeBanner(alertsOfflineText(status.lastSyncAt)) }
        }
        when {
            state.loading || (neverSynced && status.isOnline && state.cards.isEmpty()) ->
                items(3, key = { "skeleton-$it" }) { SkeletonCard() }
            state.cards.isEmpty() -> item(key = "empty") { AllClear(status.lastSyncAt) }
            else -> items(state.cards, key = { it.key }) { card -> AlertCardView(card, onAction) }
        }
    }
}

@Composable
private fun AlertCardView(card: AlertCard, onAction: (AlertsAction) -> Unit) {
    val colors = Tenet.colors
    when (card) {
        is AlertCard.Fork -> LabelCard(
            header = "Handed over twice",
            meta = card.meta,
            icon = Icons.Sharp.Warning,
            edge = colors.alert,
            cell = colors.alert,
            onCell = colors.onAlert,
            headerText = colors.alertText,
            modifier = Modifier.testTag("fork_card"),
        ) {
            Text(
                buildAnnotatedString {
                    val name = card.personName
                    val start = card.sentence.indexOf(name)
                    if (start < 0) append(card.sentence) else {
                        append(card.sentence.substring(0, start))
                        withStyle(SpanStyle(color = colors.alertText, fontWeight = FontWeight.Bold)) { append(name) }
                        append(card.sentence.substring(start + name.length))
                    }
                },
                style = TenetType.bodyLg,
                color = colors.ink,
            )
            if (!card.signaturesValid) {
                Text("SIGNATURE CHECK FAILED", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.alertText)
            }
            PrimaryButton(
                label = "Decide who holds ${card.itemName}",
                onClick = { onAction(AlertsAction.OpenFork(card.alertId)) },
            )
        }
        is AlertCard.Overdue -> LabelCard(
            header = "Overdue",
            meta = card.itemCode,
            icon = Icons.Sharp.Schedule,
            edge = colors.signalEdge,
            cell = colors.signal,
            onCell = colors.onSignal,
            headerText = colors.signalText,
            onClick = { onAction(AlertsAction.OpenItem(card.itemId)) },
        ) { Text(card.sentence, style = TenetType.body, color = colors.ink) }
        is AlertCard.SealMismatch -> LabelCard(
            header = "Seal mismatch",
            meta = card.meta,
            icon = Icons.Sharp.GppBad,
            edge = colors.alert,
            cell = colors.alert,
            onCell = colors.onAlert,
            headerText = colors.alertText,
            onClick = card.itemId?.let { id -> { onAction(AlertsAction.OpenItem(id)) } },
        ) { Text(card.sentence, style = TenetType.body, color = colors.ink) }
        is AlertCard.Refused -> LabelCard(
            header = "Refused by the server",
            meta = card.meta,
            icon = Icons.Sharp.Block,
            edge = colors.lineStrong,
            cell = colors.bg200,
            onCell = colors.ink,
            headerText = colors.ink,
            onClick = card.itemId?.let { id -> { onAction(AlertsAction.OpenItem(id)) } },
        ) { Text(card.sentence, style = TenetType.body, color = colors.ink) }
        is AlertCard.Waiting -> Column(
            Modifier.fillMaxWidth().dashedBorder(colors.lineStrong).padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space1),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                Icon(Icons.Sharp.HourglassTop, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                Text("WAITING · ${card.itemCode}".uppercase(), style = TenetType.label, color = colors.inkMuted)
            }
            Text(card.sentence, style = TenetType.body, color = colors.inkMuted)
        }
        is AlertCard.Resolved -> Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.targetRow)
                .background(colors.bg100)
                .border(TenetDimens.strokeControl, colors.line)
                .clickable { onAction(AlertsAction.OpenFork(card.alertId)) }
                .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2)
                .testTag("resolved_card"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        ) {
            Icon(Icons.Sharp.CheckCircle, contentDescription = null, tint = colors.ok, modifier = Modifier.size(TenetDimens.iconMd))
            Column(Modifier.weight(1f)) {
                Text("FORK · ${card.itemCode}".uppercase(), style = TenetType.label, color = colors.ink)
                Text(card.text, style = TenetType.body, color = colors.inkMuted)
                if (card.notSynced) {
                    Text("DECISION NOT SYNCED YET", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.inkMuted)
                }
            }
        }
    }
}

/** The warning-label card (design/components/ForkAlert): a coloured symbol cell beside the claim, then the evidence. */
@Composable
private fun LabelCard(
    header: String,
    meta: String,
    icon: ImageVector,
    edge: Color,
    cell: Color,
    onCell: Color,
    headerText: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.bg100)
            .border(TenetDimens.strokeControl, edge)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(cell), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = onCell, modifier = Modifier.size(TenetDimens.iconLg))
            }
            Column(Modifier.weight(1f).padding(horizontal = TenetDimens.space3)) {
                Text(header.uppercase(), style = TenetType.title, color = headerText)
                Text(meta.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
            }
        }
        Column(
            Modifier.padding(TenetDimens.space4),
            verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        ) { content() }
    }
}

@Composable
private fun AllClear(lastSyncAt: Long?) {
    val colors = Tenet.colors
    Column(
        Modifier.fillMaxWidth().dashedBorder(colors.lineStrong).padding(TenetDimens.space8).testTag("alerts_empty"),
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Sharp.CheckCircle, contentDescription = null, tint = colors.ok, modifier = Modifier.size(TenetDimens.space12))
        Text("ALL CAMERAS ACCOUNTED FOR", style = TenetType.title, color = colors.ink, textAlign = TextAlign.Center)
        Text(
            "No forks, overdue cameras, seal mismatches or refused records. ${lastSyncText(lastSyncAt)}.",
            style = TenetType.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** design/components/LoadingSkeleton: the card's shape in bg-200 while the first sync runs. */
@Composable
private fun SkeletonCard() {
    val colors = Tenet.colors
    Column(
        Modifier.fillMaxWidth().background(colors.bg100).border(TenetDimens.strokeControl, colors.line),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(colors.bg200))
            Column(Modifier.padding(horizontal = TenetDimens.space3), verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
                Box(Modifier.width(180.dp).height(16.dp).background(colors.bg200))
                Box(Modifier.width(120.dp).height(12.dp).background(colors.bg200))
            }
        }
        Column(Modifier.padding(TenetDimens.space4), verticalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
            Box(Modifier.fillMaxWidth().height(16.dp).background(colors.bg200))
            Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(colors.bg200))
        }
    }
}
