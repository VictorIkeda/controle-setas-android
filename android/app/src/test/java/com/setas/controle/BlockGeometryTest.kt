package com.setas.controle

import com.setas.controle.Arrows.DOWN
import com.setas.controle.Arrows.LEFT
import com.setas.controle.Arrows.RIGHT
import com.setas.controle.Arrows.UP
import org.junit.Assert.assertEquals
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
}
