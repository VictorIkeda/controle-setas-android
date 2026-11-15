package com.setas.controle

import com.setas.controle.Arrows.DOWN
import com.setas.controle.Arrows.LEFT
import com.setas.controle.Arrows.RIGHT
import com.setas.controle.Arrows.UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockGeometryTest {
    private val block = BlockGeometry(0f, 0f, 100f)

    @Test fun hit_centerOfDown_isDown() = assertEquals(DOWN, block.hit(158f, 158f))

    @Test fun hit_gapBetweenDownAndRight_isBoth() = assertEquals(DOWN or RIGHT, block.hit(212f, 158f))

    @Test fun hit_upperLeftCorner_isUpLeft() = assertEquals(UP or LEFT, block.hit(50f, 50f))

    @Test fun hit_upperRightCorner_isUpRight() = assertEquals(UP or RIGHT, block.hit(266f, 50f))

    @Test fun hit_outside_isZero() = assertEquals(0, block.hit(-30f, -30f))

    @Test fun hit_respectsBlockPosition() =
        assertEquals(DOWN, BlockGeometry(500f, 300f, 100f).hit(658f, 458f))

    @Test fun hit_neverOppositeDirections() {
        var y = -10f
        while (y <= 220f) {
            var x = -10f
            while (x <= 330f) {
                val m = block.hit(x, y)
                assertTrue("↑+↓ em ($x,$y)", (m and UP == 0) || (m and DOWN == 0))
                assertTrue("←+→ em ($x,$y)", (m and LEFT == 0) || (m and RIGHT == 0))
                x += 1f
            }
            y += 1f
        }
    }

    @Test fun hit_gapBetweenUpAndDown_isOnlyOne() {
        assertEquals(UP, block.hit(158f, 103f))
        assertEquals(DOWN, block.hit(158f, 105f))
    }

    @Test fun hit_downLeftBand_isWide() {
        // centro do vão entre ← e ↓ = u + g/2 = 104; faixa de ±0,15u
        assertEquals(LEFT or DOWN, block.hit(91f, 158f))
        assertEquals(LEFT or DOWN, block.hit(117f, 158f))
        assertEquals(LEFT, block.hit(80f, 158f))
        assertEquals(DOWN, block.hit(130f, 158f))
    }

    @Test fun hit_downRightBand_isWide() {
        // centro do vão entre ↓ e → = 2u + 1,5g = 212
        assertEquals(DOWN or RIGHT, block.hit(199f, 158f))
        assertEquals(DOWN or RIGHT, block.hit(225f, 158f))
        assertEquals(DOWN, block.hit(190f, 158f))
        assertEquals(RIGHT, block.hit(235f, 158f))
    }
}
