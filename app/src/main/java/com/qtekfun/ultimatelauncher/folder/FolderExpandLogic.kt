// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.folder

/**
 * Lógica pura (sin Android) de las carpetas ampliables (parches 0140–0143): qué apps se ven en la baldosa, geometría
 * de la baldosa y de las celdas, validación del tamaño guardado y búsqueda de hueco 2x2. Se prueba en JVM.
 */
object FolderExpandLogic {
    /** Lado (en celdas) de una carpeta ampliada: 2x2. */
    const val EXPANDED_SPAN = 2
    /** Resultado de [hitTest]: toque fuera de cualquier app. */
    const val NONE = -1
    /** Resultado de [hitTest]: toque en la celda «+N». */
    const val OVERFLOW = -2

    data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left
        val height get() = bottom - top
        fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
    }

    /** Baldosa cuadrada: esquina superior izquierda y lado, en píxeles de la vista. */
    data class Tile(val left: Int, val top: Int, val side: Int) {
        val right get() = left + side
        val bottom get() = top + side
    }

    /**
     * Reparto de las apps en la baldosa: [columns] x [columns] celdas; se dibujan [shown] iconos y, si [overflow] > 0,
     * la celda siguiente muestra «+overflow».
     */
    data class SlotPlan(val columns: Int, val shown: Int, val overflow: Int) {
        /** Celdas ocupadas (iconos + la de «+N» si la hay). */
        val used get() = shown + if (overflow > 0) 1 else 0
    }

    /** 1–4 apps: rejilla 2x2; 5–9: 3x3; más de 9: 3x3 con 8 iconos y «+N» en la última celda. */
    @JvmStatic
    fun slotPlan(count: Int): SlotPlan = when {
        count <= 4 -> SlotPlan(2, count.coerceAtLeast(0), 0)
        count <= 9 -> SlotPlan(3, count, 0)
        else -> SlotPlan(3, 8, count - 8)
    }

    /**
     * Baldosa dentro de una vista de [width] x [height]: cuadrada, centrada en horizontal, pegada arriba ([topPad]) y
     * dejando abajo sitio para el nombre ([labelHeight] + [gap]).
     */
    @JvmStatic
    fun tileFor(width: Int, height: Int, labelHeight: Int, gap: Int, sidePad: Int, topPad: Int): Tile {
        val availW = width - 2 * sidePad
        val availH = height - topPad - labelHeight - gap
        val side = minOf(availW, availH).coerceAtLeast(0)
        return Tile((width - side) / 2, topPad, side)
    }

    /** Celdas de la rejilla (orden de lectura) para los [SlotPlan.used] huecos, con un margen interior de [innerPadFrac]. */
    @JvmStatic
    fun slotCells(plan: SlotPlan, tile: Tile, innerPadFrac: Float = 0.08f): List<IntRect> {
        val pad = (tile.side * innerPadFrac).toInt()
        val cell = (tile.side - 2 * pad) / plan.columns
        return (0 until plan.used).map { i ->
            val l = tile.left + pad + (i % plan.columns) * cell
            val t = tile.top + pad + (i / plan.columns) * cell
            IntRect(l, t, l + cell, t + cell)
        }
    }

    /** Rectángulo del icono centrado en la celda: ≈ [maxIcon] px, sin pasar del [fill] de la celda. */
    @JvmStatic
    fun iconRect(cell: IntRect, maxIcon: Int, fill: Float = 0.86f): IntRect {
        val s = minOf(maxIcon, (minOf(cell.width, cell.height) * fill).toInt())
        val l = cell.left + (cell.width - s) / 2
        val t = cell.top + (cell.height - s) / 2
        return IntRect(l, t, l + s, t + s)
    }

    /** Índice de la app (0-based) bajo el toque, [OVERFLOW] para «+N» o [NONE]. Usa la celda entera como zona táctil. */
    @JvmStatic
    fun hitTest(plan: SlotPlan, cells: List<IntRect>, x: Float, y: Float): Int {
        val i = cells.indexOfFirst { it.contains(x, y) }
        return when {
            i < 0 -> NONE
            i >= plan.shown -> OVERFLOW
            else -> i
        }
    }

    /**
     * Tamaño (en celdas, ambos ejes) con el que se carga una carpeta de la base de datos: 2 solo si es un 2x2 que cabe
     * en el escritorio; cualquier otra cosa (hotseat, fuera de rejilla, 1x2, 3x3...) se carga como 1x1.
     */
    @JvmStatic
    fun sanitizedSpan(spanX: Int, spanY: Int, cellX: Int, cellY: Int, onDesktop: Boolean, cols: Int, rows: Int): Int =
        if (onDesktop && spanX == EXPANDED_SPAN && spanY == EXPANDED_SPAN &&
            cellX >= 0 && cellY >= 0 && cellX + EXPANDED_SPAN <= cols && cellY + EXPANDED_SPAN <= rows
        ) EXPANDED_SPAN else 1

    /**
     * Esquina superior izquierda de un 2x2 libre que contenga la celda de la carpeta ([fx], [fy]), probando en este orden
     * la propia celda como esquina, un hueco a la izquierda, uno arriba y uno en diagonal. [occupied] no debe contar la
     * propia carpeta. Devuelve null si ninguno es libre (hay que reordenar o no hay sitio).
     */
    @JvmStatic
    fun findExpandAnchor(cols: Int, rows: Int, fx: Int, fy: Int, occupied: (Int, Int) -> Boolean): IntArray? {
        for ((dx, dy) in listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)) {
            val ax = fx - dx
            val ay = fy - dy
            if (ax < 0 || ay < 0 || ax + EXPANDED_SPAN > cols || ay + EXPANDED_SPAN > rows) continue
            var free = true
            for (x in ax until ax + EXPANDED_SPAN) for (y in ay until ay + EXPANDED_SPAN) if (occupied(x, y)) free = false
            if (free) return intArrayOf(ax, ay)
        }
        return null
    }

    /** Esquina más cercana dentro de la rejilla para un 2x2 (se usa al reordenar los vecinos). */
    @JvmStatic
    fun clampAnchor(cols: Int, rows: Int, fx: Int, fy: Int): IntArray =
        intArrayOf(fx.coerceIn(0, (cols - EXPANDED_SPAN).coerceAtLeast(0)), fy.coerceIn(0, (rows - EXPANDED_SPAN).coerceAtLeast(0)))
}
