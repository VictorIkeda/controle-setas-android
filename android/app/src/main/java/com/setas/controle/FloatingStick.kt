package com.setas.controle

import kotlin.math.hypot

/**
 * Analógico flutuante: o centro nasce onde o dedo encosta, então encostar nunca dispara
 * direção sozinho. A direção vem do deslocamento a partir desse ponto.
 */
class FloatingStick {
    private var active = false
    var originX = 0f; private set
    var originY = 0f; private set
    var knobDx = 0f; private set
    var knobDy = 0f; private set

    fun down(x: Float, y: Float, @Suppress("UNUSED_PARAMETER") g: StickGeometry): Int {
        active = true
        originX = x
        originY = y
        knobDx = 0f
        knobDy = 0f
        return 0
    }

    fun move(x: Float, y: Float, g: StickGeometry): Int {
        if (!active) return 0
        var dx = x - originX
        var dy = y - originY
        val mask = StickMapper.map(dx, dy, g.size)
        val d = hypot(dx, dy)
        if (d > g.maxTravel) {
            dx = dx / d * g.maxTravel
            dy = dy / d * g.maxTravel
        }
        knobDx = dx
        knobDy = dy
        return mask
    }

    fun up() {
        active = false
        knobDx = 0f
        knobDy = 0f
    }

    /** Centro atual do analógico: o ponto de toque, ou o centro do círculo se ninguém está tocando. */
    fun originXOr(g: StickGeometry): Float = if (active) originX else g.centerX
    fun originYOr(g: StickGeometry): Float = if (active) originY else g.centerY
}
