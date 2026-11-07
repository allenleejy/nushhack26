package com.nushhack26.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SealedCubeTest {
    /** One px per design unit, so expected values read straight off the formulas. */
    private val edge = 120f

    @Test fun restShowsFrontRightAndLid() {
        val shown = cubeFaces(CUBE_REST_DEG, edge).map { it.face }.toSet()
        assertEquals(setOf(CubeFace.Front, CubeFace.Right, CubeFace.Top), shown)
    }

    @Test fun backIsShownWhenTurnedRound() {
        val shown = cubeFaces(145f, edge).map { it.face }
        assertTrue(CubeFace.Back in shown)
        assertTrue(CubeFace.Front !in shown)
    }

    @Test fun frontTopLeftCornerProjects() {
        // (−60, −60, 60): rotateY(−35°) → x −84.56, z 14.73; rotateX(−22°) → y −50.11, z 36.14;
        // perspective 760 / (760 − 36.14) = 1.04993 → (−88.78·…): (−87.74, −52.61).
        val front = cubeFaces(CUBE_REST_DEG, edge).first { it.face == CubeFace.Front }
        val corner = front.corners[0]
        assertEquals(-87.74f, corner.x, 1f)
        assertEquals(-52.61f, corner.y, 1f)
    }

    @Test fun tapeAndDiamondSitOnTheirFaces() {
        val faces = cubeFaces(CUBE_REST_DEG, edge).associateBy { it.face }
        assertEquals(1, faces.getValue(CubeFace.Top).tape.size)
        assertEquals(1, faces.getValue(CubeFace.Front).tape.size)
        assertEquals(4, faces.getValue(CubeFace.Front).diamond.size)
        assertTrue(faces.getValue(CubeFace.Right).tape.isEmpty())
    }

    @Test fun drawnFarToNear() {
        // At rest the front's centre is nearest the viewer (z 45.5), then the right (31.9), then the lid (22.5).
        assertEquals(listOf(CubeFace.Top, CubeFace.Right, CubeFace.Front), cubeFaces(CUBE_REST_DEG, edge).map { it.face })
    }
}
