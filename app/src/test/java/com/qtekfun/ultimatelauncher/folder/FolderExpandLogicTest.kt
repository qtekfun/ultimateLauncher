package com.qtekfun.ultimatelauncher.folder

import com.qtekfun.ultimatelauncher.folder.FolderExpandLogic as L
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderExpandLogicTest {
    @Test fun slotPlanByCount() {
        assertEquals(L.SlotPlan(2, 2, 0), L.slotPlan(2))
        assertEquals(L.SlotPlan(2, 4, 0), L.slotPlan(4))
        assertEquals(L.SlotPlan(3, 5, 0), L.slotPlan(5))
        assertEquals(L.SlotPlan(3, 9, 0), L.slotPlan(9))
        val big = L.slotPlan(12)
        assertEquals(L.SlotPlan(3, 8, 4), big)
        assertEquals(9, big.used)
        assertEquals(0, L.slotPlan(-3).shown)
    }

    @Test fun tileIsSquareCenteredAndLeavesRoomForLabel() {
        val t = L.tileFor(width = 400, height = 450, labelHeight = 50, gap = 10, sidePad = 10, topPad = 10)
        assertEquals(380, t.side)               // min(380, 450-10-50-10=380)
        assertEquals(10, t.left)
        assertTrue(t.bottom + 10 + 50 <= 450)
        val narrow = L.tileFor(200, 600, 40, 8, 4, 4)
        assertEquals(192, narrow.side)
        assertEquals(4, narrow.left)
        assertEquals(0, L.tileFor(10, 10, 40, 8, 4, 4).side)   // sin sitio: lado 0, no negativo
    }

    @Test fun cellsStayInsideTileAndDoNotOverlap() {
        val tile = L.Tile(10, 10, 300)
        for (n in listOf(1, 4, 7, 9, 15)) {
            val plan = L.slotPlan(n)
            val cells = L.slotCells(plan, tile)
            assertEquals(plan.used, cells.size)
            cells.forEach { assertTrue(it.left >= tile.left && it.right <= tile.right && it.top >= tile.top && it.bottom <= tile.bottom) }
            for (i in cells.indices) for (j in i + 1 until cells.size) {
                val a = cells[i]; val b = cells[j]
                assertTrue(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top)
            }
        }
    }

    @Test fun iconRectIsCappedAndCentered() {
        val cell = L.IntRect(0, 0, 100, 100)
        val big = L.iconRect(cell, maxIcon = 500)
        assertEquals(86, big.width)
        val small = L.iconRect(cell, maxIcon = 40)
        assertEquals(40, small.width)
        assertEquals(30, small.left)
        assertEquals(30, small.top)
    }

    @Test fun hitTestFindsAppOverflowOrNothing() {
        val tile = L.Tile(0, 0, 300)
        val plan = L.slotPlan(12)
        val cells = L.slotCells(plan, tile)
        assertEquals(0, L.hitTest(plan, cells, cells[0].left + 1f, cells[0].top + 1f))
        assertEquals(7, L.hitTest(plan, cells, cells[7].left + 1f, cells[7].top + 1f))
        assertEquals(L.OVERFLOW, L.hitTest(plan, cells, cells[8].left + 1f, cells[8].top + 1f))
        assertEquals(L.NONE, L.hitTest(plan, cells, 1f, 1f))            // margen de la baldosa
        assertEquals(L.NONE, L.hitTest(plan, cells, 1000f, 1000f))
        // 2 apps en rejilla 2x2: la tercera celda no existe
        val p2 = L.slotPlan(2); val c2 = L.slotCells(p2, tile)
        assertEquals(2, c2.size)
        assertEquals(L.NONE, L.hitTest(p2, c2, tile.left + 250f, tile.top + 250f))
    }

    @Test fun sanitizedSpanOnlyAccepts2x2InsideTheGrid() {
        assertEquals(2, L.sanitizedSpan(2, 2, 0, 0, true, 5, 7))
        assertEquals(2, L.sanitizedSpan(2, 2, 3, 5, true, 5, 7))       // última esquina válida
        assertEquals(1, L.sanitizedSpan(2, 2, 4, 5, true, 5, 7))       // se sale por la derecha
        assertEquals(1, L.sanitizedSpan(2, 2, 3, 6, true, 5, 7))       // se sale por abajo
        assertEquals(1, L.sanitizedSpan(2, 2, 0, 0, false, 5, 7))      // hotseat/carpeta
        assertEquals(1, L.sanitizedSpan(2, 1, 0, 0, true, 5, 7))
        assertEquals(1, L.sanitizedSpan(3, 3, 0, 0, true, 5, 7))
        assertEquals(1, L.sanitizedSpan(1, 1, 0, 0, true, 5, 7))
        assertEquals(1, L.sanitizedSpan(2, 2, -1, 0, true, 5, 7))
    }

    @Test fun anchorPrefersOwnCellThenShiftsAwayFromCollisions() {
        val occ = HashSet<Pair<Int, Int>>()
        val busy = { x: Int, y: Int -> occ.contains(x to y) }
        assertArrayEquals(intArrayOf(1, 1), L.findExpandAnchor(5, 7, 1, 1, busy))
        occ.add(2 to 2)                                              // choca a la derecha -> esquina a la izquierda
        assertArrayEquals(intArrayOf(0, 1), L.findExpandAnchor(5, 7, 1, 1, busy))
        occ.add(0 to 2)                                              // y a la izquierda -> sube
        assertArrayEquals(intArrayOf(1, 0), L.findExpandAnchor(5, 7, 1, 1, busy))
        occ.add(1 to 0)                                              // y arriba -> no hay hueco sin reordenar
        assertNull(L.findExpandAnchor(5, 7, 1, 1, busy))
    }

    @Test fun anchorAtGridEdgeShiftsInsideOrFails() {
        val none = { _: Int, _: Int -> false }
        assertArrayEquals(intArrayOf(3, 5), L.findExpandAnchor(5, 7, 4, 6, none))   // esquina inferior derecha
        assertArrayEquals(intArrayOf(0, 0), L.findExpandAnchor(5, 7, 0, 0, none))
        val all = { _: Int, _: Int -> true }
        assertNull(L.findExpandAnchor(5, 7, 2, 2, all))
        assertNull(L.findExpandAnchor(1, 1, 0, 0, none))                             // rejilla de 1x1: no cabe
    }

    @Test fun clampKeepsAnchorInsideGrid() {
        assertArrayEquals(intArrayOf(3, 5), L.clampAnchor(5, 7, 4, 6))
        assertArrayEquals(intArrayOf(2, 3), L.clampAnchor(5, 7, 2, 3))
        assertArrayEquals(intArrayOf(0, 0), L.clampAnchor(1, 1, 0, 0))
    }
}
