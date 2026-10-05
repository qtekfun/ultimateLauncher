// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dock

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockLogicTest {
    @Test fun occupiedRangeCoversGaps() {
        assertArrayEquals(intArrayOf(1, 5), DockLogic.occupiedRange(listOf(1, 2, 5)))
    }

    @Test fun occupiedRangeEmptyIsZero() {
        assertArrayEquals(intArrayOf(0, 0), DockLogic.occupiedRange(emptyList()))
    }

    @Test fun occupiedRangeUsesSpans() {
        assertArrayEquals(intArrayOf(2, 4), DockLogic.occupiedRange(listOf(2), listOf(3)))
    }

    @Test fun compactPacksLeftKeepingOrder() {
        // Fijos en 1, 4 y 5 (el usuario sacó los de 0, 2 y 3): pasan a 0, 1 y 2.
        assertEquals(listOf(0, 1, 2), DockLogic.compactTargets(listOf(1, 4, 5), 6))
    }

    @Test fun compactKeepsTheOrderOfUnsortedInput() {
        // La lista llega en orden de vistas, no de huecos: cada fijo conserva su posición relativa.
        assertEquals(listOf(2, 0, 1), DockLogic.compactTargets(listOf(5, 1, 3), 6))
    }

    @Test fun compactIsIdempotent() {
        val once = DockLogic.compactTargets(listOf(2, 3, 5), 6)
        assertEquals(once, DockLogic.compactTargets(once, 6))
        assertFalse(DockLogic.needsCompaction(once, 6))
    }

    @Test fun alreadyPackedNeedsNothing() {
        assertFalse(DockLogic.needsCompaction(listOf(0, 1, 2), 6))
        assertFalse(DockLogic.needsCompaction(emptyList(), 6))
        assertEquals(emptyList<IntRange>(), DockLogic.gapRanges(listOf(0, 1, 2), 6))
    }

    @Test fun freeSpaceAfterTheLastIsNotAGap() {
        assertEquals(emptyList<IntRange>(), DockLogic.gapRanges(listOf(0, 1), 6))
    }

    @Test fun gapRangesFindLeadingAndInnerGaps() {
        assertEquals(listOf(0..1, 3..4), DockLogic.gapRanges(listOf(2, 5), 6))
    }

    @Test fun fullDockHasNoGaps() {
        val full = (0 until 6).toList()
        assertEquals(full, DockLogic.compactTargets(full, 6))
        assertTrue(DockLogic.gapRanges(full, 6).isEmpty())
    }

    @Test fun invalidInputIsLeftUntouched() {
        assertEquals(listOf(1, 1), DockLogic.compactTargets(listOf(1, 1), 6)) // duplicados
        assertEquals(listOf(0, 9), DockLogic.compactTargets(listOf(0, 9), 6)) // fuera de rango
        assertEquals(listOf(-1, 2), DockLogic.compactTargets(listOf(-1, 2), 6))
        assertEquals(listOf(3), DockLogic.compactTargets(listOf(3), 0))
        assertFalse(DockLogic.needsCompaction(listOf(1, 1), 6))
        assertTrue(DockLogic.gapRanges(listOf(0, 9), 6).isEmpty())
    }
}
