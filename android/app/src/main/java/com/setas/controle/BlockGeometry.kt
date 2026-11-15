package com.setas.controle

import android.graphics.RectF

/**
 * Bloco das 4 setas no formato do desenho: ↑ em cima, ← ↓ → embaixo.
 * [x], [y] é o canto superior esquerdo do bloco; [u] é o lado de cada seta.
 */
data class BlockGeometry(val x: Float, val y: Float, val u: Float) {
    val gap: Float get() = u * 0.08f
    val width: Float get() = 3 * u + 2 * gap
    val height: Float get() = 2 * u + gap

    /** Canto superior esquerdo de cada seta, relativo ao bloco. */
    private fun origin(arrow: Int): Pair<Float, Float> = when (arrow) {
        Arrows.UP -> (u + gap) to 0f
        Arrows.LEFT -> 0f to (u + gap)
        Arrows.DOWN -> (u + gap) to (u + gap)
        else -> (2 * u + 2 * gap) to (u + gap)
    }

    /** Retângulo da seta na tela (para desenhar). */
    fun keyRect(arrow: Int): RectF {
        val (ox, oy) = origin(arrow)
        return RectF(x + ox, y + oy, x + ox + u, y + oy + u)
    }

    /** Retângulos das zonas de diagonal ↖ e ↗ (para desenhar). */
    fun cornerRects(): List<RectF> = listOf(
        RectF(x, y, x + u, y + u),
        RectF(x + 2 * u + 2 * gap, y, x + width, y + u),
    )

    /**
     * Quais setas estão sob o ponto (coordenadas da tela).
     * - Cada seta aceita uma pequena folga (0,08u) em volta.
     * - ↙ e ↘: faixa de ±0,15u em torno do vão entre ↓ e ←/→.
     * - ↖ e ↗: os cantos vazios ao lado do ↑.
     * - Nunca devolve direções opostas (↑+↓ ou ←+→): fica a mais próxima.
     */
    fun hit(px: Float, py: Float): Int {
        val lx = px - x
        val ly = py - y
        val tol = u * 0.08f
        val band = u * 0.15f
        var mask = 0
        for (a in intArrayOf(Arrows.UP, Arrows.DOWN, Arrows.LEFT, Arrows.RIGHT)) {
            val (ox, oy) = origin(a)
            if (lx >= ox - tol && lx <= ox + u + tol && ly >= oy - tol && ly <= oy + u + tol) mask = mask or a
        }
        if (ly >= 0 && ly < u) {
            if (lx >= 0 && lx < u) mask = mask or Arrows.UP or Arrows.LEFT
            if (lx >= 2 * u + 2 * gap && lx < width) mask = mask or Arrows.UP or Arrows.RIGHT
        }
        val rowTop = u + gap - tol
        val rowBottom = 2 * u + gap + tol
        if (ly >= rowTop && ly <= rowBottom) {
            if (kotlin.math.abs(lx - (u + gap / 2)) <= band) mask = mask or Arrows.LEFT or Arrows.DOWN
            if (kotlin.math.abs(lx - (2 * u + 1.5f * gap)) <= band) mask = mask or Arrows.DOWN or Arrows.RIGHT
        }
        if (mask and Arrows.UP != 0 && mask and Arrows.DOWN != 0) {
            mask = if (ly < u + gap / 2) mask and Arrows.DOWN.inv() else mask and Arrows.UP.inv()
        }
        if (mask and Arrows.LEFT != 0 && mask and Arrows.RIGHT != 0) {
            mask = if (lx < width / 2) mask and Arrows.RIGHT.inv() else mask and Arrows.LEFT.inv()
        }
        return mask
    }
}
