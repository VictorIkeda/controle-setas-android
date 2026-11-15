package com.setas.controle

import com.setas.controle.Arrows.DOWN
import com.setas.controle.Arrows.RIGHT
import com.setas.controle.Arrows.UP
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingStickTest {
    private val geometry = StickGeometry(0f, 0f, 200f) // centro (100,100)
    private val stick = FloatingStick()

    @Test fun touchDownFarFromCenter_firesNothing() {
        assertEquals(0, stick.down(160f, 160f, geometry))
    }

    @Test fun moveFromTouchPoint_givesDirection() {
        stick.down(160f, 160f, geometry)
        assertEquals(UP, stick.move(160f, 110f, geometry))
    }

    @Test fun smallWiggleNearTouchPoint_isDeadZone() {
        stick.down(160f, 160f, geometry)
        assertEquals(0, stick.move(170f, 165f, geometry))
    }

    @Test fun knobOffset_isRelativeToTouchPoint_andCappedAtMaxTravel() {
        stick.down(160f, 160f, geometry)
        stick.move(400f, 160f, geometry)
        assertEquals(160f, stick.originX, 0.001f)
        assertEquals(60f, stick.knobDx, 0.001f) // maxTravel = 0,3 · 200
        assertEquals(0f, stick.knobDy, 0.001f)
    }

    @Test fun up_resetsEverything() {
        stick.down(160f, 160f, geometry)
        stick.move(200f, 200f, geometry)
        stick.up()
        assertEquals(0f, stick.knobDx, 0.001f)
        assertEquals(geometry.centerX, stick.originXOr(geometry), 0.001f)
    }

    @Test fun diagonal_downRight() {
        stick.down(100f, 100f, geometry)
        assertEquals(DOWN or RIGHT, stick.move(140f, 140f, geometry))
    }
}
