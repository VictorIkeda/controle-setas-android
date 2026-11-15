package com.setas.controle

import android.content.SharedPreferences

/** Guarda um layout ajustado para a horizontal e outro para a vertical. */
class LayoutStore(private val prefs: SharedPreferences) {
    private fun p(landscape: Boolean) = if (landscape) "h_" else "v_"

    fun load(landscape: Boolean): Layout? {
        val k = p(landscape)
        if (!prefs.contains(k + "u")) return null
        return Layout(
            BlockGeometry(prefs.getFloat(k + "bx", 0f), prefs.getFloat(k + "by", 0f), prefs.getFloat(k + "u", 0f)),
            StickGeometry(prefs.getFloat(k + "sx", 0f), prefs.getFloat(k + "sy", 0f), prefs.getFloat(k + "ss", 0f)),
        )
    }

    fun save(landscape: Boolean, l: Layout) {
        val k = p(landscape)
        prefs.edit()
            .putFloat(k + "bx", l.block.x).putFloat(k + "by", l.block.y).putFloat(k + "u", l.block.u)
            .putFloat(k + "sx", l.stick.x).putFloat(k + "sy", l.stick.y).putFloat(k + "ss", l.stick.size)
            .apply()
    }

    fun clear(landscape: Boolean) {
        val k = p(landscape)
        prefs.edit().apply { listOf("bx", "by", "u", "sx", "sy", "ss").forEach { remove(k + it) } }.apply()
    }
}
