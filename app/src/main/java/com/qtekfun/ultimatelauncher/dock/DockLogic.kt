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

    /**
     * Rangos de huecos libres que quedan ANTES del último fijo (los intermedios y el de la izquierda); el espacio libre
     * tras el último no cuenta. Los índices son huecos del hotseat 0 until [capacity]. Entrada inválida (fuera de rango o
     * duplicados) devuelve lista vacía.
     */
    fun gapRanges(cells: List<Int>, capacity: Int): List<IntRange> {
        if (!valid(cells, capacity) || cells.isEmpty()) return emptyList()
        val taken = cells.toSortedSet()
        val last = taken.last()
        val out = ArrayList<IntRange>()
        var start = -1
        for (c in 0..last) {
            if (c in taken) {
                if (start >= 0) { out += start until c; start = -1 }
            } else if (start < 0) start = c
        }
        return out
    }

    /**
     * Hueco nuevo de cada fijo para empaquetar el dock hacia la izquierda sin cambiar su orden: el k-ésimo por posición
     * pasa al hueco k. Devuelve una lista del mismo tamaño y orden que [cells]. Si la entrada no es válida (duplicados,
     * fuera de [0, capacity)) no se toca nada y se devuelve [cells] tal cual.
     */
    fun compactTargets(cells: List<Int>, capacity: Int): List<Int> {
        if (!valid(cells, capacity)) return cells
        val order = cells.indices.sortedBy { cells[it] }
        val out = IntArray(cells.size)
        order.forEachIndexed { rank, idx -> out[idx] = rank }
        return out.toList()
    }

    /** true si hay al menos un hueco libre antes del último fijo (compactar movería algo). */
    fun needsCompaction(cells: List<Int>, capacity: Int): Boolean = compactTargets(cells, capacity) != cells

    private fun valid(cells: List<Int>, capacity: Int): Boolean =
        capacity > 0 && cells.size <= capacity && cells.all { it in 0 until capacity } && cells.toSet().size == cells.size
}
