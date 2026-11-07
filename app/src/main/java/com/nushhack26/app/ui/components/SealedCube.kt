package com.nushhack26.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.Tenet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** The cube's edge on screen: E = 120 design units (card 28). */
val CubeEdge: Dp = 137.dp

/** The cube's rest turn, which shows the front, the right side and the lid. */
const val CUBE_REST_DEG = -35f

/** Design units per cube edge; corners sit at ±[HALF]. */
private const val UNITS = 120f
private const val HALF = UNITS / 2
private const val TILT_DEG = -22f
private const val PERSPECTIVE = 760f
private const val TAPE_HALF = 13f
private const val TAPE_DROP = 52f
private const val DIAMOND_SIDE = 24f
private const val DIAMOND_X = 34f
private const val DIAMOND_Y = 30f

enum class CubeFace { Front, Back, Right, Left, Top, Bottom }

/** A face as drawn: its projected corners, plus the tape and diamond on it, all in px from the cube's centre. */
data class ProjectedFace(
    val face: CubeFace,
    val corners: List<Offset>,
    /** Tape pieces: four corners each, the long sides being 0→1 and 2→3. */
    val tape: List<List<Offset>>,
    /** The Class 7 diamond on the front, or empty. */
    val diamond: List<Offset>,
)

private class V3(val x: Float, val y: Float, val z: Float)

private class FaceDef(
    val face: CubeFace,
    val normal: V3,
    val corners: List<V3>,
    val tape: List<List<V3>>,
    val diamond: List<V3>,
)

private val h = HALF
private val d = DIAMOND_SIDE / 2 * sqrt(2f)

/** Tape on the front (z = +60) or back (z = −60): from the top edge down [TAPE_DROP]. Long sides x = ±13. */
private fun sideTape(z: Float) = listOf(
    V3(-TAPE_HALF, -h, z), V3(-TAPE_HALF, -h + TAPE_DROP, z),
    V3(TAPE_HALF, -h + TAPE_DROP, z), V3(TAPE_HALF, -h, z),
)

private val Faces = listOf(
    FaceDef(
        CubeFace.Front, V3(0f, 0f, 1f),
        listOf(V3(-h, -h, h), V3(h, -h, h), V3(h, h, h), V3(-h, h, h)),
        listOf(sideTape(h)),
        listOf(
            V3(DIAMOND_X, DIAMOND_Y - d, h), V3(DIAMOND_X + d, DIAMOND_Y, h),
            V3(DIAMOND_X, DIAMOND_Y + d, h), V3(DIAMOND_X - d, DIAMOND_Y, h),
        ),
    ),
    FaceDef(
        CubeFace.Back, V3(0f, 0f, -1f),
        listOf(V3(h, -h, -h), V3(-h, -h, -h), V3(-h, h, -h), V3(h, h, -h)),
        listOf(sideTape(-h)),
        emptyList(),
    ),
    FaceDef(
        CubeFace.Right, V3(1f, 0f, 0f),
        listOf(V3(h, -h, h), V3(h, -h, -h), V3(h, h, -h), V3(h, h, h)),
        emptyList(), emptyList(),
    ),
    FaceDef(
        CubeFace.Left, V3(-1f, 0f, 0f),
        listOf(V3(-h, -h, -h), V3(-h, -h, h), V3(-h, h, h), V3(-h, h, -h)),
        emptyList(), emptyList(),
    ),
    FaceDef(
        CubeFace.Top, V3(0f, -1f, 0f),
        listOf(V3(-h, -h, -h), V3(h, -h, -h), V3(h, -h, h), V3(-h, -h, h)),
        listOf(
            listOf(
                V3(-TAPE_HALF, -h, -h), V3(-TAPE_HALF, -h, h),
                V3(TAPE_HALF, -h, h), V3(TAPE_HALF, -h, -h),
            ),
        ),
        emptyList(),
    ),
    FaceDef(
        CubeFace.Bottom, V3(0f, 1f, 0f),
        listOf(V3(-h, h, h), V3(h, h, h), V3(h, h, -h), V3(-h, h, -h)),
        emptyList(), emptyList(),
    ),
)

/** CSS's `rotateX(-22deg) rotateY(θ)`: rotateY first, then rotateX. */
private class Pose(angleYDeg: Float) {
    private val cy = cos(Math.toRadians(angleYDeg.toDouble())).toFloat()
    private val sy = sin(Math.toRadians(angleYDeg.toDouble())).toFloat()
    private val cx = cos(Math.toRadians(TILT_DEG.toDouble())).toFloat()
    private val sx = sin(Math.toRadians(TILT_DEG.toDouble())).toFloat()

    fun rotate(p: V3): V3 {
        val x1 = p.x * cy + p.z * sy
        val z1 = -p.x * sy + p.z * cy
        val y2 = p.y * cx - z1 * sx
        val z2 = p.y * sx + z1 * cx
        return V3(x1, y2, z2)
    }
}

private fun project(p: V3, unitPx: Float): Offset {
    val s = PERSPECTIVE / (PERSPECTIVE - p.z) * unitPx
    return Offset(p.x * s, p.y * s)
}

/**
 * The cube's visible faces at turn [angleYDeg], far to near, projected with perspective 760 and
 * scaled so one edge is [edgePx]. Points are relative to the cube's centre.
 */
internal fun cubeFaces(angleYDeg: Float, edgePx: Float): List<ProjectedFace> {
    val pose = Pose(angleYDeg)
    val unitPx = edgePx / UNITS
    val eye = PERSPECTIVE
    return Faces.mapNotNull { def ->
        val n = pose.rotate(def.normal)
        val c = pose.rotate(V3(def.normal.x * h, def.normal.y * h, def.normal.z * h))
        // Seen from the eye at (0, 0, 760): the face shows if its normal points towards the eye.
        val facing = n.x * -c.x + n.y * -c.y + n.z * (eye - c.z)
        if (facing <= 0f) null else c.z to ProjectedFace(
            face = def.face,
            corners = def.corners.map { project(pose.rotate(it), unitPx) },
            tape = def.tape.map { piece -> piece.map { project(pose.rotate(it), unitPx) } },
            diamond = def.diamond.map { project(pose.rotate(it), unitPx) },
        )
    }.sortedBy { it.first }.map { it.second }
}

private fun Path.polygon(points: List<Offset>) {
    reset()
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

/**
 * The camera as a sealed cube (docs/spec.md section 3, "The tap moment"): amber tape over the lid
 * and down the front and back, the Class 7 diamond on the front. [angleY] is θ in degrees and
 * [scale] multiplies the cube; both are read only while drawing.
 */
@Composable
fun SealedCube(angleY: () -> Float, scale: () -> Float, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Box(
        modifier.drawWithCache {
            val edgePx = CubeEdge.toPx()
            val outline = Stroke(2.dp.toPx(), join = StrokeJoin.Miter)
            val tapeLine = 2.dp.toPx()
            val paths = List(3) { Path() }
            val fills = mapOf(
                CubeFace.Front to lerp(colors.line, colors.ink, 0.2f),
                CubeFace.Back to colors.bg100,
                CubeFace.Right to colors.line,
                CubeFace.Left to colors.bg200,
                CubeFace.Top to lerp(colors.line, colors.ink, 0.4f),
                CubeFace.Bottom to colors.bg000,
            )
            onDrawBehind {
                val s = scale()
                if (s <= 0f) return@onDrawBehind
                val faces = cubeFaces(angleY(), edgePx)
                translate(center.x, center.y) {
                    scale(s, pivot = Offset.Zero) {
                        for (f in faces) {
                            val face = paths[0].apply { polygon(f.corners) }
                            drawPath(face, fills.getValue(f.face))
                            for (piece in f.tape) {
                                val tape = paths[1].apply { polygon(piece) }
                                drawPath(tape, colors.signal)
                                drawLine(colors.signalEdge, piece[0], piece[1], tapeLine)
                                drawLine(colors.signalEdge, piece[2], piece[3], tapeLine)
                            }
                            if (f.diamond.isNotEmpty()) {
                                val diamond = paths[2].apply { polygon(f.diamond) }
                                drawPath(diamond, colors.signal)
                                drawPath(diamond, colors.signalEdge, style = outline)
                            }
                            drawPath(face, colors.lineStrong, style = outline)
                        }
                    }
                }
            }
        },
    )
}
