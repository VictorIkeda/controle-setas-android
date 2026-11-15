package com.setas.controle

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Analógico: [x], [y] é o canto superior esquerdo do quadrado que o contém; [size] é o diâmetro. */
data class StickGeometry(val x: Float, val y: Float, val size: Float) {
    val centerX: Float get() = x + size / 2
    val centerY: Float get() = y + size / 2
    val maxTravel: Float get() = size * 0.3f
    val deadZone: Float get() = size * 0.12f

    fun contains(px: Float, py: Float): Boolean = hypot(px - centerX, py - centerY) <= size / 2
}

object StickMapper {
    private val SECTORS = intArrayOf(
        Arrows.RIGHT,
        Arrows.UP or Arrows.RIGHT,
        Arrows.UP,
        Arrows.UP or Arrows.LEFT,
        Arrows.LEFT,
        Arrows.DOWN or Arrows.LEFT,
        Arrows.DOWN,
        Arrows.DOWN or Arrows.RIGHT,
    )

    /** [dx], [dy] em coordenadas da tela (y para baixo), a partir do centro do analógico. */
    fun map(dx: Float, dy: Float, size: Float): Int {
        if (hypot(dx, dy) <= size * 0.12f) return 0
        val deg = Math.toDegrees(atan2(-dy, dx).toDouble())
        val sector = (((deg + 360.0) % 360.0) / 45.0).roundToInt() % 8
        return SECTORS[sector]
    }
}
