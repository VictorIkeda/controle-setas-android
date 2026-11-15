package com.setas.controle

/** Junta todos os dedos nas setas e o analógico num único estado, e diz quando vibrar. */
class TouchState {
    private val pointers = HashMap<Int, Int>()
    private var stick = 0
    private var lastFingerBits = 0

    private val fingerBits: Int get() = pointers.values.fold(0) { acc, m -> acc or m }

    val mask: Int get() = fingerBits or stick

    fun setPointer(id: Int, mask: Int) {
        pointers[id] = mask
    }

    fun removePointer(id: Int) {
        pointers.remove(id)
    }

    fun setStick(mask: Int) {
        stick = mask
    }

    fun releaseAll() {
        pointers.clear()
        stick = 0
        lastFingerBits = 0
    }

    /** Setas que passaram a ser apertadas pelos dedos desde a última chamada (o analógico não conta). */
    fun takeNewArrowBits(): Int {
        val now = fingerBits
        val added = now and lastFingerBits.inv()
        lastFingerBits = now
        return added
    }
}
