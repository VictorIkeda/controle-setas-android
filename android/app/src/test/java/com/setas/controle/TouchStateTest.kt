package com.setas.controle

import com.setas.controle.Arrows.DOWN
import com.setas.controle.Arrows.LEFT
import com.setas.controle.Arrows.RIGHT
import com.setas.controle.Arrows.UP
import org.junit.Assert.assertEquals
import org.junit.Test

class TouchStateTest {
    private val s = TouchState()

    @Test fun twoFingersSameArrow_oneLifts_stillPressed() {
        s.setPointer(1, UP)
        s.setPointer(2, UP)
        s.removePointer(1)
        assertEquals(UP, s.mask)
    }

    @Test fun fingersAndStick_areUnited() {
        s.setPointer(1, LEFT)
        s.setStick(DOWN)
        assertEquals(LEFT or DOWN, s.mask)
    }

    @Test fun stickDoesNotVibrate() {
        s.setStick(DOWN)
        assertEquals(0, s.takeNewArrowBits())
    }

    @Test fun slideIntoNewArrow_vibratesOnce() {
        s.setPointer(1, DOWN)
        s.takeNewArrowBits()
        s.setPointer(1, DOWN or RIGHT)
        assertEquals(RIGHT, s.takeNewArrowBits())
        assertEquals(0, s.takeNewArrowBits())
    }

    @Test fun reEnteringArrow_vibratesAgain() {
        s.setPointer(1, DOWN)
        s.takeNewArrowBits()
        s.setPointer(1, 0)
        s.takeNewArrowBits()
        s.setPointer(1, DOWN)
        assertEquals(DOWN, s.takeNewArrowBits())
    }

    @Test fun releaseAll_clearsEverything() {
        s.setPointer(1, UP)
        s.setPointer(2, RIGHT)
        s.setStick(DOWN)
        s.releaseAll()
        assertEquals(0, s.mask)
    }

    @Test fun releaseAll_thenSameArrow_vibratesAgain() {
        s.setPointer(1, UP)
        s.takeNewArrowBits()
        s.releaseAll()
        s.setPointer(2, UP)
        assertEquals(UP, s.takeNewArrowBits())
    }
}
