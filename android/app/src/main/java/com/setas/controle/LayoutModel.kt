package com.setas.controle

data class Layout(val block: BlockGeometry, val stick: StickGeometry)

/** Layouts padrão por orientação e as operações do modo de ajuste (cadeado aberto). */
object LayoutModel {
    /** Proporção largura/u e altura/u do bloco (3u + 2·0,08u e 2u + 0,08u). */
    private const val BLOCK_W = 3.16f
    private const val BLOCK_H = 2.08f

    fun default(w: Float, h: Float): Layout = if (w > h) {
        val m = minOf(w, h)
        val u = 0.30f * m
        val size = 0.70f * m
        Layout(
            BlockGeometry(0.04f * m, (h - BLOCK_H * u) / 2, u),
            StickGeometry(w - 0.04f * m - size, (h - size) / 2, size),
        )
    } else {
        val u = 0.27f * w
        val size = 0.60f * w
        Layout(
            BlockGeometry((w - BLOCK_W * u) / 2, h - 0.06f * h - BLOCK_H * u, u),
            StickGeometry((w - size) / 2, 0.30f * h - size / 2, size),
        )
    }

    fun clamp(l: Layout, w: Float, h: Float, minU: Float, minStick: Float): Layout {
        val maxU = minOf(w / BLOCK_W, h / BLOCK_H)
        val u = l.block.u.coerceIn(minOf(minU, maxU), maxU)
        val bw = BLOCK_W * u
        val bh = BLOCK_H * u
        val block = BlockGeometry(
            l.block.x.coerceIn(0f, maxOf(0f, w - bw)),
            l.block.y.coerceIn(0f, maxOf(0f, h - bh)),
            u,
        )
        val maxSize = minOf(w, h)
        val size = l.stick.size.coerceIn(minOf(minStick, maxSize), maxSize)
        val stick = StickGeometry(
            l.stick.x.coerceIn(0f, maxOf(0f, w - size)),
            l.stick.y.coerceIn(0f, maxOf(0f, h - size)),
            size,
        )
        return Layout(block, stick)
    }

    fun moveBlock(l: Layout, dx: Float, dy: Float): Layout =
        l.copy(block = l.block.copy(x = l.block.x + dx, y = l.block.y + dy))

    fun resizeBlock(l: Layout, dx: Float, dy: Float): Layout =
        l.copy(block = l.block.copy(u = l.block.u + (dx + dy) / 6f))

    fun moveStick(l: Layout, dx: Float, dy: Float): Layout =
        l.copy(stick = l.stick.copy(x = l.stick.x + dx, y = l.stick.y + dy))

    fun resizeStick(l: Layout, dx: Float, dy: Float): Layout =
        l.copy(stick = l.stick.copy(size = l.stick.size + (dx + dy) / 2f))
}
