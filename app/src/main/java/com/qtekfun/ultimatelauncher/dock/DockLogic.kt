package com.qtekfun.ultimatelauncher.dock

/** Lógica pura (sin Android) de la zona de apps fijas del dock de tablet, para poder probarla en la JVM. */
object DockLogic {
    /** Rango [primero, último] de huecos ocupados, o [0, 0] si no hay ninguno. */
    fun occupiedRange(cells: List<Int>, spans: List<Int> = cells.map { 1 }): IntArray {
        if (cells.isEmpty()) return intArrayOf(0, 0)
        var lo = Int.MAX_VALUE
        var hi = -1
        for (i in cells.indices) {
            lo = minOf(lo, cells[i]); hi = maxOf(hi, cells[i] + spans[i] - 1)
        }
        return intArrayOf(lo, hi)
    }
}
