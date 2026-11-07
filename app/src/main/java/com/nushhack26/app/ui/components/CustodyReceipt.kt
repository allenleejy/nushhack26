package com.nushhack26.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.SpaceGrotesk
import com.nushhack26.app.ui.theme.SpaceMono
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetAngles
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

/** A signer on the receipt: "RAVI KUMAR", and on the record view "KEY 3F:A9:12:7C" under it. */
data class ReceiptParty(val name: String, val key: String? = null)

/**
 * What one custody receipt prints (docs/spec.md section 3, "The custody receipt"). Every value is
 * already formatted; the receipt uppercases. A null optional line isn't printed.
 */
data class ReceiptContent(
    /** RECORD #5: this handover's place in the item's chain, counting the register as #1. */
    val recordNo: Int?,
    /** FOLLOWS #4; null on the register record. */
    val followsNo: Int?,
    /** "07.11.2026" */
    val date: String,
    /** SIGNED 13:02 */
    val signedTime: String,
    /** "Kestrel"; blank falls back to [itemCode]. */
    val itemName: String,
    /** "C-12" */
    val itemCode: String,
    /** "IR-0457 · IR-192" */
    val source: String?,
    /** "S-40117" */
    val sealNo: String?,
    val sealMatches: Boolean = true,
    /** Null on the register record. */
    val from: ReceiptParty?,
    val to: ReceiptParty,
    /** "19:00" */
    val returnBy: String?,
    /** "NFC TAP", "QR CODE" or "SERVER"; record view only, when known. */
    val via: String? = null,
    /** "TAN WEI MING", or "YOU" on the receiver's Done screen. */
    val heldBy: String,
    /** "2 OF 2 VERIFIED"; record view only. */
    val signatures: String? = null,
    val signaturesOk: Boolean = true,
    /** The full record hash: printed as "SHA 4F2A 91C0 77E3 0B1D" and seeds the barcode. Record view only. */
    val hash: String? = null,
    /** "OFFLINE" under the title when the handover was signed with no network. Record view only. */
    val offline: Boolean = false,
    /** "482 107" */
    val verifyCode: String,
    /** "Saved on this phone · not synced yet"; record view only. */
    val footer: String? = null,
)

/** The countersigned stamp: HANDED OVER / 13:02 / TO WEI MING. */
data class ReceiptStamp(val word: String, val time: String, val line: String)

enum class ReceiptStyle {
    /** G5 and R Done: torn off the roll at both ends, printed out from under the hazard band, stamp and wave. */
    Done,
    /** The record view: fed out of a printer slot, keys, signatures, barcode and hash, stamp, no wave. */
    Record,
}

private val Base = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.02.em)
private val Bold = Base.copy(fontWeight = FontWeight.Bold)
private val TitleStyle = Bold.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.12.em)
private val TotalName = Bold.copy(fontSize = 20.sp, lineHeight = 26.sp)
private val StampWord = Bold.copy(fontSize = 20.sp, lineHeight = 24.sp)
private val StampTime = Bold.copy(fontSize = 16.sp, lineHeight = 22.sp)
private val StampLine = Bold.copy(fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
private val FooterStyle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)

private val Indent = 28.dp
private val RuleGap = 10.dp
private val Tear = 8.dp
private val Tooth = 12.dp
private val LeaderMin = 12.dp
private val BarcodeHeight = 44.dp
private const val BARCODE_CELLS = 60

/** The print, stamp and wave timings (card 29), per style. */
private class ReceiptTiming(val printDelayMs: Int, val printMs: Int, val printSteps: Int, val stampAtMs: Long, val wave: Boolean)

private val DoneTiming = ReceiptTiming(printDelayMs = 150, printMs = 1200, printSteps = 22, stampAtMs = 1450, wave = true)
private val RecordTiming = ReceiptTiming(printDelayMs = 0, printMs = 1700, printSteps = 26, stampAtMs = 1900, wave = false)

private const val DROP_MS = 260
private const val DROP_FROM = 2.2f
private val DropEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private const val SQUASH_MS = 320
private const val WAVE_MS = 420
private val WaveDelaysMs = longArrayOf(0, 25, 32, 48)

private class StepEasing(private val steps: Int) : Easing {
    override fun transform(fraction: Float): Float = floor(fraction * steps) / steps
}

/** The animated values, read only while drawing so nothing re-lays out. */
private class ReceiptMotion(animate: Boolean) {
    val print = Animatable(if (animate) 0f else 1f)
    val stampScale = Animatable(if (animate) DROP_FROM else 1f)
    var stampShown by mutableStateOf(!animate)
    /** translationY in px for sections w0–w3. */
    val wave = List(4) { Animatable(0f) }
}

/**
 * The custody receipt, set like a printed till roll (docs/spec.md section 3, card 29).
 * [ReceiptStyle.Done] is torn at both ends, prints out line by line, then the stamp drops,
 * thuds and sends a ripple through the sections. [ReceiptStyle.Record] hangs from a printer
 * slot, adds keys, signatures, barcode and hash, and has no wave. It plays once per receipt;
 * with reduce motion it's there at once with one thud, and in inspection mode it's still.
 */
@Composable
fun CustodyReceipt(
    content: ReceiptContent,
    stamp: ReceiptStamp,
    style: ReceiptStyle,
    modifier: Modifier = Modifier,
) {
    val colors = Tenet.colors
    val inspection = LocalInspectionMode.current
    val reduceMotion = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val timing = if (style == ReceiptStyle.Done) DoneTiming else RecordTiming

    var played by rememberSaveable(content.verifyCode, style) { mutableStateOf(false) }
    val animate = remember { !played && !reduceMotion && !inspection }
    val motion = remember { ReceiptMotion(animate) }

    LaunchedEffect(Unit) {
        if (played || inspection) return@LaunchedEffect
        if (!animate) {
            played = true
            haptics.thud()
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                motion.print.animateTo(
                    1f,
                    tween(timing.printMs, delayMillis = timing.printDelayMs, easing = StepEasing(timing.printSteps)),
                )
            }
            delay(timing.stampAtMs)
            motion.stampShown = true
            motion.stampScale.animateTo(1f, tween(DROP_MS, easing = DropEasing))
            played = true
            haptics.thud()
            launch {
                motion.stampScale.animateTo(
                    1f,
                    keyframes {
                        durationMillis = SQUASH_MS
                        0.94f at (SQUASH_MS * 0.15f).toInt()
                        1.03f at (SQUASH_MS * 0.55f).toInt()
                    },
                )
            }
            if (timing.wave) {
                val dp = density.density
                motion.wave.forEachIndexed { i, a ->
                    launch {
                        delay(WaveDelaysMs[i])
                        a.animateTo(
                            0f,
                            keyframes {
                                durationMillis = WAVE_MS
                                6f * dp at (WAVE_MS * 0.18f).toInt()
                                -2f * dp at (WAVE_MS * 0.45f).toInt()
                                1f * dp at (WAVE_MS * 0.70f).toInt()
                            },
                        )
                    }
                }
            }
        }
    }

    val description = receiptDescription(content, stamp)
    val record = style == ReceiptStyle.Record
    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        if (record) PrinterSlot(content)
        val paper = colors.bg100
        Column(
            Modifier
                .padding(horizontal = if (record) 20.dp else 0.dp)
                .padding(top = if (record) 0.dp else Tear, bottom = Tear)
                .fillMaxWidth()
                .drawWithContent {
                    val tear = Tear.toPx()
                    val p = motion.print.value
                    val draw: DrawScope.() -> Unit = {
                        drawTears(paper, top = !record, tear = tear, tooth = Tooth.toPx())
                        this@drawWithContent.drawContent()
                    }
                    if (p >= 1f) {
                        draw()
                    } else {
                        val top = if (record) 0f else -tear
                        val full = size.height + tear - top
                        clipRect(left = -size.width, top = top, right = size.width * 2, bottom = top + full * p) { draw() }
                    }
                }
                .background(paper)
                .padding(
                    start = if (record) 18.dp else 20.dp,
                    end = if (record) 18.dp else 20.dp,
                    top = if (record) 18.dp else 20.dp,
                    bottom = if (record) 22.dp else 18.dp,
                ),
        ) {
            ReceiptLines(content, stamp, record, motion)
        }
    }
}

@Composable
private fun ReceiptLines(content: ReceiptContent, stamp: ReceiptStamp, record: Boolean, motion: ReceiptMotion) {
    val colors = Tenet.colors
    val w = motion.wave
    fun Modifier.wave(i: Int) = graphicsLayer { translationY = w[i].value }

    Column(Modifier.fillMaxWidth().wave(1), horizontalAlignment = Alignment.CenterHorizontally) {
        Line("TENET", TitleStyle, align = TextAlign.Center)
        Line("CUSTODY RECEIPT", Bold, Modifier.padding(top = 2.dp), align = TextAlign.Center)
        if (record && content.offline) Line("OFFLINE", Base, color = colors.inkMuted, align = TextAlign.Center)
    }
    DashedRule()
    Column(Modifier.fillMaxWidth().wave(1)) {
        PairLine(content.recordNo?.let { "RECORD #$it" } ?: "RECORD", content.date)
        PairLine(content.followsNo?.let { "FOLLOWS #$it" } ?: "", "SIGNED ${content.signedTime}")
    }
    DashedRule()
    Column(Modifier.fillMaxWidth().wave(2)) {
        Line("1 × ${content.itemName.ifBlank { content.itemCode }}", Bold)
        Column(Modifier.padding(start = Indent)) {
            Line("CAMERA ${content.itemCode}", Base)
            content.source?.let { Line("SOURCE $it", Base) }
            content.sealNo?.let {
                LeaderLine(
                    "SEAL $it",
                    if (content.sealMatches) "MATCHES" else "DOESN'T MATCH",
                    valueColor = if (content.sealMatches) colors.ok else colors.alertText,
                )
            }
        }
    }
    DashedRule()
    Column(Modifier.fillMaxWidth().wave(2)) {
        content.from?.let { party ->
            LeaderLine("FROM", party.name)
            if (record) party.key?.let { KeyLine(it) }
        }
        LeaderLine("TO", content.to.name)
        if (record) content.to.key?.let { KeyLine(it) }
        content.returnBy?.let { LeaderLine("RETURN BY", it) }
        if (record) content.via?.let { LeaderLine("VIA", it) }
    }
    DoubleRule(Modifier.wave(1))
    TotalLine(content.heldBy, Modifier.wave(1))
    DoubleRule(Modifier.wave(1))
    if (record) {
        content.signatures?.let {
            LeaderLine("SIGNATURES", it, valueColor = if (content.signaturesOk) colors.ok else colors.alertText)
        }
    }
    Box(
        Modifier.zIndex(1f).wave(0).fillMaxWidth().height(if (record) 132.dp else 124.dp),
        contentAlignment = Alignment.Center,
    ) {
        Stamp(
            stamp,
            Modifier.graphicsLayer {
                alpha = if (motion.stampShown) 1f else 0f
                scaleX = motion.stampScale.value
                scaleY = motion.stampScale.value
                rotationZ = TenetAngles.angleStamp
            },
        )
    }
    val hash = content.hash?.takeIf { record && it.isNotBlank() }
    if (hash != null) {
        Barcode(hash, Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Line(ReceiptNumbers.shortHash(hash), Base, Modifier.fillMaxWidth(), color = colors.inkMuted, align = TextAlign.Center)
    }
    Line("VERIFY CODE ${content.verifyCode}", Bold, Modifier.fillMaxWidth().wave(3), align = TextAlign.Center)
    if (record) {
        content.footer?.let {
            DashedRule()
            Text(it, Modifier.fillMaxWidth(), style = FooterStyle, color = colors.ink, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Line(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Tenet.colors.ink,
    align: TextAlign? = null,
) {
    Text(text.uppercase(), modifier, style = style, color = color, textAlign = align)
}

/** `tot`: HELD BY and the name on one baseline; a name too long to sit beside it goes under, right-aligned. */
@Composable
private fun TotalLine(name: String, modifier: Modifier) {
    Layout(
        content = {
            Line("HELD BY", Base)
            Line(name, TotalName, align = TextAlign.End)
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = 12.dp.roundToPx()
        val labelP = measurables[0].measure(Constraints(maxWidth = width))
        val room = width - labelP.width - gap
        val oneRow = measurables[1].maxIntrinsicWidth(constraints.maxHeight) <= room
        val nameP = measurables[1].measure(Constraints(maxWidth = if (oneRow) room else width))
        if (oneRow) {
            val top = (nameP[FirstBaseline] - labelP[FirstBaseline]).coerceAtLeast(0)
            layout(width, maxOf(nameP.height, labelP.height + top)) {
                labelP.place(0, top)
                nameP.place(width - nameP.width, 0)
            }
        } else {
            layout(width, labelP.height + nameP.height) {
                labelP.place(0, 0)
                nameP.place(width - nameP.width, labelP.height)
            }
        }
    }
}

@Composable
private fun KeyLine(key: String) {
    Line(key, Base, Modifier.fillMaxWidth(), color = Tenet.colors.inkMuted, align = TextAlign.End)
}

/** `ld2`: label left, value right. */
@Composable
private fun PairLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Line(label, Base)
        Spacer(Modifier.weight(1f).width(8.dp))
        Line(value, Base, align = TextAlign.End)
    }
}

/**
 * `ld`: the label, a dotted leader filling the gap, then the value in bold. When the value
 * can't fit beside the label with at least [LeaderMin] of leader, it wraps under, right-aligned.
 */
@Composable
private fun LeaderLine(label: String, value: String, valueColor: Color = Tenet.colors.ink) {
    val muted = Tenet.colors.inkMuted
    val leader = remember { LeaderGeometry() }
    Layout(
        content = {
            Line(label, Base)
            Line(value, Bold, color = valueColor, align = TextAlign.End)
        },
        modifier = Modifier.fillMaxWidth().drawBehind { drawLeader(leader, muted) },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val minLeader = LeaderMin.roundToPx()
        val gap = 4.dp.roundToPx()
        val labelP = measurables[0].measure(Constraints(maxWidth = (width - minLeader).coerceAtLeast(0)))
        val oneRow = measurables[1].maxIntrinsicWidth(constraints.maxHeight) <= width - labelP.width - minLeader - 2 * gap
        val valueP = measurables[1].measure(
            Constraints(maxWidth = if (oneRow) width - labelP.width - minLeader - 2 * gap else width),
        )
        val labelBase = labelP[FirstBaseline]
        val height = if (oneRow) maxOf(labelP.height, valueP.height) else labelP.height + valueP.height
        layout(width, height) {
            labelP.place(0, 0)
            if (oneRow) {
                valueP.place(width - valueP.width, 0)
                leader.set(labelP.width + gap, width - valueP.width - gap, labelBase)
            } else {
                valueP.place(width - valueP.width, labelP.height)
                leader.set(labelP.width + gap, width, labelBase)
            }
        }
    }
}

private class LeaderGeometry {
    var start = 0
    var end = 0
    var baseline = 0
    fun set(start: Int, end: Int, baseline: Int) {
        this.start = start
        this.end = end
        this.baseline = baseline
    }
}

private fun DrawScope.drawLeader(g: LeaderGeometry, color: Color) {
    val dot = 2.dp.toPx()
    val y = g.baseline - 4.dp.toPx() - dot
    var x = g.start.toFloat()
    while (x + dot <= g.end) {
        drawRect(color, Offset(x, y), Size(dot, dot))
        x += dot * 2
    }
}

/** `rr`: a 2 dp dashed rule with 10 dp above and below. */
@Composable
private fun DashedRule() {
    val color = Tenet.colors.lineStrong
    Spacer(
        Modifier.padding(vertical = RuleGap).fillMaxWidth().height(2.dp).drawBehind {
            val dash = 6.dp.toPx()
            val gap = 4.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(color, Offset(x, 0f), Size(minOf(dash, size.width - x), size.height))
                x += dash + gap
            }
        },
    )
}

/** `rr2`: two 2 dp ink lines with a 2 dp gap, 10 dp above and below. */
@Composable
private fun DoubleRule(modifier: Modifier = Modifier) {
    val color = Tenet.colors.ink
    Spacer(
        modifier.padding(vertical = RuleGap).fillMaxWidth().height(6.dp).drawBehind {
            val line = 2.dp.toPx()
            drawRect(color, Offset.Zero, Size(size.width, line))
            drawRect(color, Offset(0f, size.height - line), Size(size.width, line))
        },
    )
}

@Composable
private fun Stamp(stamp: ReceiptStamp, modifier: Modifier) {
    val colors = Tenet.colors
    Column(
        modifier
            .border(3.dp, colors.ok)
            .background(colors.bg100)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Line(stamp.word, StampWord, color = colors.ok, align = TextAlign.Center)
        Line(stamp.time, StampTime, color = colors.ok, align = TextAlign.Center)
        Line(stamp.line, StampLine, color = colors.ok, align = TextAlign.Center)
    }
}

/** The record view's printer slot and mouth, above the receipt. */
@Composable
private fun PrinterSlot(content: ReceiptContent) {
    val colors = Tenet.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.bg200)
            .border(2.dp, colors.lineStrong)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            content.recordNo?.let { "RECORD #$it" } ?: "RECORD",
            Modifier.weight(1f),
            style = TenetType.recordLabel,
            color = colors.ink,
        )
        Box(Modifier.size(8.dp).background(colors.ok))
    }
    Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(6.dp).background(MouthColor))
}

private val MouthColor = Color(0xFF121211)

/** 60 cells alternating bar and space, each 2 dp or (p = 0.35) 4 dp, seeded from the hash so it never changes. */
@Composable
private fun Barcode(hash: String, modifier: Modifier) {
    val ink = Tenet.colors.ink
    val cells = remember(hash) { barcodeCells(hash) }
    val total = cells.sum()
    Box(modifier, contentAlignment = Alignment.Center) {
        Spacer(
            Modifier.width(total.dp).height(BarcodeHeight).drawBehind {
                var x = 0f
                cells.forEachIndexed { i, cell ->
                    val w = cell.dp.toPx()
                    if (i % 2 == 0) drawRect(ink, Offset(x, 0f), Size(w, size.height))
                    x += w
                }
            },
        )
    }
}

/** Cell widths in dp, from a mulberry32 PRNG seeded with the hash's first 8 hex digits. */
internal fun barcodeCells(hash: String): List<Int> {
    var state = hash.take(8).toLongOrNull(16)?.toInt() ?: hash.hashCode()
    fun next(): Double {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
    }
    return List(BARCODE_CELLS) { if (next() < 0.35) 4 else 2 }
}

/** The zigzag tear in the paper colour, just outside the box: top and bottom, or bottom only. */
private fun DrawScope.drawTears(paper: Color, top: Boolean, tear: Float, tooth: Float) {
    fun edge(y: Float, dir: Float) {
        val path = Path().apply {
            moveTo(0f, y)
            var x = 0f
            while (x < size.width) {
                val mid = minOf(x + tooth / 2, size.width)
                val next = minOf(x + tooth, size.width)
                lineTo(mid, y + dir * tear)
                lineTo(next, y)
                x = next
            }
            close()
        }
        drawPath(path, paper)
    }
    if (top) edge(0f, -1f)
    edge(size.height, 1f)
}

/** "Custody receipt, record 5. Kestrel, camera C-12, handed over by Ravi Kumar to Tan Wei Ming at 13:02. Held by Tan Wei Ming. Verify code 482 107." */
internal fun receiptDescription(content: ReceiptContent, stamp: ReceiptStamp): String = buildString {
    append("Custody receipt")
    content.recordNo?.let { append(", record $it") }
    append(". ")
    val name = content.itemName.ifBlank { content.itemCode }
    append("$name, camera ${content.itemCode}, ")
    content.from?.let { append("handed over by ${it.name} to ${content.to.name}") } ?: append("registered to ${content.to.name}")
    append(" at ${stamp.time}.")
    content.returnBy?.let { append(" Return by $it.") }
    val held = if (content.heldBy.equals("you", ignoreCase = true)) "you" else content.heldBy
    append(" Held by $held.")
    append(" Verify code ${content.verifyCode}.")
}
