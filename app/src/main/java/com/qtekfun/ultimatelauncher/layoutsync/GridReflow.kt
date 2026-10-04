package com.qtekfun.ultimatelauncher.layoutsync

/**
 * Reubicación de elementos sobre una rejilla de destino (RF-52 y docs/06 «Rejilla distinta»).
 * Si todo cabe en la rejilla de destino se conservan las posiciones; si no, se recolocan TODOS los elementos
 * por orden de lectura (página, fila, columna) con primer hueco libre, repartiendo páginas si hace falta.
 */
object GridReflow {
    data class Result(val pages: List<List<Item>>, val reflowed: Boolean, val clampedSpans: Int)

    private fun spanOf(i: Item) = if (i is Item.Widget) i.span else Span(1, 1)

    fun fits(pages: List<Page>, dst: Grid): Boolean = pages.all { p ->
        p.items.all { i -> val c = i.cell ?: return@all false; val s = spanOf(i); c.x + s.w <= dst.columns && c.y + s.h <= dst.rows }
    }

    fun reflow(pages: List<Page>, dst: Grid): Result {
        if (fits(pages, dst)) return Result(pages.sortedBy { it.index }.map { it.items }, false, 0)
        val ordered = pages.sortedBy { it.index }.flatMap { p -> p.items.sortedWith(compareBy({ it.cell?.y ?: 0 }, { it.cell?.x ?: 0 })) }
        val out = mutableListOf<MutableList<Item>>()
        val occ = mutableListOf<Array<BooleanArray>>()
        var clamped = 0
        fun newPage() { out.add(mutableListOf()); occ.add(Array(dst.rows) { BooleanArray(dst.columns) }) }
        for (item in ordered) {
            var s = spanOf(item)
            val cs = Span(minOf(s.w, dst.columns), minOf(s.h, dst.rows))
            if (cs != s) { clamped++; s = cs }
            val placed = item.let { if (it is Item.Widget) it.copy(span = s) else it }
            var done = false
            var p = 0
            while (!done) {
                if (p >= out.size) newPage()
                loop@ for (y in 0..dst.rows - s.h) for (x in 0..dst.columns - s.w) {
                    if ((0 until s.h).all { dy -> (0 until s.w).all { dx -> !occ[p][y + dy][x + dx] } }) {
                        for (dy in 0 until s.h) for (dx in 0 until s.w) occ[p][y + dy][x + dx] = true
                        out[p] += placed.at(Cell(x, y)); done = true; break@loop
                    }
                }
                if (!done) p++
            }
        }
        return Result(out, true, clamped)
    }
}
