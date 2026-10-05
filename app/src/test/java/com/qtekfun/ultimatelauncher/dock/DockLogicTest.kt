package com.qtekfun.ultimatelauncher.dock

import org.junit.Assert.assertArrayEquals
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
}
