package com.setas.controle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutModelTest {
    private val minU = 100f
    private val minStick = 200f

    private fun fits(l: Layout, w: Float, h: Float) {
        val b = l.block
        val s = l.stick
        assertTrue("bloco dentro", b.x >= 0f && b.y >= 0f && b.x + b.width <= w + 0.01f && b.y + b.height <= h + 0.01f)
        assertTrue("analógico dentro", s.x >= 0f && s.y >= 0f && s.x + s.size <= w + 0.01f && s.y + s.size <= h + 0.01f)
    }

    @Test fun defaultLandscape_blockLeftStickRight() {
        val l = LayoutModel.default(2000f, 1000f)
        assertTrue(l.block.x + l.block.width < l.stick.x)
    }

    @Test fun defaultPortrait_stickAboveBlock() {
        val l = LayoutModel.default(1000f, 2000f)
        assertTrue(l.stick.y + l.stick.size < l.block.y)
    }

    @Test fun defaults_fitOnScreen() {
        fits(LayoutModel.default(2000f, 1000f), 2000f, 1000f)
        fits(LayoutModel.default(1000f, 2000f), 1000f, 2000f)
    }

    @Test fun clamp_blockDraggedOffScreen_staysInside() {
        val l = LayoutModel.default(2000f, 1000f)
        val moved = LayoutModel.clamp(LayoutModel.moveBlock(l, -5000f, 0f), 2000f, 1000f, minU, minStick)
        assertEquals(0f, moved.block.x, 0.001f)
    }

    @Test fun clamp_stickDraggedOffScreen_staysInside() {
        val l = LayoutModel.default(2000f, 1000f)
        val moved = LayoutModel.clamp(LayoutModel.moveStick(l, 5000f, 5000f), 2000f, 1000f, minU, minStick)
        fits(moved, 2000f, 1000f)
    }

    @Test fun clamp_tooSmall_raisedToMin() {
        val l = LayoutModel.default(2000f, 1000f)
        val small = LayoutModel.clamp(LayoutModel.resizeBlock(l, -5000f, -5000f), 2000f, 1000f, minU, minStick)
        assertEquals(minU, small.block.u, 0.001f)
        val smallStick = LayoutModel.clamp(LayoutModel.resizeStick(l, -5000f, -5000f), 2000f, 1000f, minU, minStick)
        assertEquals(minStick, smallStick.stick.size, 0.001f)
    }

    @Test fun clamp_tooBig_stillFits() {
        val l = LayoutModel.default(2000f, 1000f)
        val big = LayoutModel.clamp(
            LayoutModel.resizeStick(LayoutModel.resizeBlock(l, 9000f, 9000f), 9000f, 9000f),
            2000f, 1000f, minU, minStick,
        )
        fits(big, 2000f, 1000f)
    }

    @Test fun resizeBlock_growsByAverageOverSix() {
        val l = LayoutModel.default(2000f, 1000f)
        val r = LayoutModel.resizeBlock(l, 30f, 30f)
        assertEquals(l.block.u + 10f, r.block.u, 0.001f)
    }
}
