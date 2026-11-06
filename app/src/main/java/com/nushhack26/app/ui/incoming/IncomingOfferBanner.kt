package com.nushhack26.app.ui.incoming

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.repo.BannerLookup
import com.nushhack26.app.ui.components.DiamondVariant
import com.nushhack26.app.ui.components.HazardDiamond
import com.nushhack26.app.ui.theme.SpaceMono
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.delay
import java.util.Collections

/** Offer ids that have already buzzed, so each one vibrates once however often the frame recomposes. */
private val buzzedOfferIds: MutableSet<String> = Collections.synchronizedSet(HashSet())

/**
 * R0 (docs/spec.md, docs/sync.md A.10): an offer sent to me through the server. Collecting
 * `offerRelay.incoming` is what turns the inbox polling on, so it only runs while this is shown.
 */
@Composable
fun IncomingOfferBanner(container: AppContainer, onOpen: (String) -> Unit) {
    val offers by container.offerRelay.incoming.collectAsStateWithLifecycle()
    val lookup by remember { container.receiveRepository.observeBannerLookup() }
        .collectAsStateWithLifecycle(BannerLookup())
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(offers.isNotEmpty()) {
        while (offers.isNotEmpty()) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val visible = IncomingOffers.visible(offers, lookup, nowMs)
    val context = LocalContext.current
    LaunchedEffect(visible.map { it.id }) {
        offers.forEach { container.receiveRepository.rememberRelayedGiver(it.id, it.fromPersonId) }
        if (visible.any { buzzedOfferIds.add(it.id) }) vibrateOnce(context)
    }
    val newest = visible.maxByOrNull { it.expiresMs } ?: return
    IncomingOfferBannerContent(newest, nowMs, onOpen = { onOpen(newest.id) })
}

/** Stateless: the signal-yellow bar with the giver, the camera, a countdown and Open. */
@Composable
fun IncomingOfferBannerContent(offer: IncomingOfferUi, nowMs: Long, modifier: Modifier = Modifier, onOpen: () -> Unit = {}) {
    val colors = Tenet.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.signal)
            .clickable(role = Role.Button, onClickLabel = "Open the offer", onClick = onOpen)
            .testTag("incoming_offer_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TenetDimens.targetRow)
                .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        ) {
            HazardDiamond(variant = DiamondVariant.Filled)
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        append("${offer.giverName} is handing you ")
                        if (offer.itemName.isBlank() || offer.itemName == offer.itemCode) {
                            withStyle(SpanStyle(fontFamily = SpaceMono)) { append(offer.itemCode.uppercase()) }
                        } else {
                            append("${offer.itemName} (")
                            withStyle(SpanStyle(fontFamily = SpaceMono)) { append(offer.itemCode.uppercase()) }
                            append(")")
                        }
                    },
                    style = TenetType.bodyStrong,
                    color = colors.onSignal,
                )
                Text("Scan its seal to accept", style = TenetType.body, color = colors.onSignal)
                Text("VALID ${offer.countdown(nowMs)}", style = TenetType.recordLabel, color = colors.onSignal)
            }
            Box(
                Modifier
                    .heightIn(min = TenetDimens.targetMin)
                    .widthIn(min = TenetDimens.targetMin)
                    .border(TenetDimens.strokeControl, colors.onSignal, RectangleShape)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = TenetDimens.space3),
                contentAlignment = Alignment.Center,
            ) {
                Text("OPEN", style = TenetType.label, color = colors.onSignal)
            }
        }
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.signalEdge)
    }
}

private fun vibrateOnce(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
