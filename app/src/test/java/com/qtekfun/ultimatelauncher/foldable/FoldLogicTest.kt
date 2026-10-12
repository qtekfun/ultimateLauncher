// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.foldable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldLogicTest {
    // Pixel 9 Pro Fold (medido con adb): interior 2076x2152 y exterior 1080x2424, ambas a 390 dpi.
    private val inner = PanelSize.of(2076, 2152)
    private val outer = PanelSize.of(1080, 2424)
    private val pixel8 = PanelSize.of(1080, 2400)
    private val tablet = PanelSize.of(1600, 2560)

    @Test fun fold9ProIsFoldable() {
        assertTrue(FoldLogic.isFoldable(listOf(inner, outer), 390))
        assertTrue(FoldLogic.smallestDp(inner, 390) >= FoldLogic.TABLET_MIN_DP)
        assertTrue(FoldLogic.smallestDp(outer, 390) < FoldLogic.TABLET_MIN_DP)
    }

    @Test fun onlyOneScreenKnownIsNotFoldable() {
        assertFalse(FoldLogic.isFoldable(listOf(inner), 390))
        assertFalse(FoldLogic.isFoldable(listOf(outer), 390))
        assertFalse(FoldLogic.isFoldable(emptyList(), 390))
    }

    @Test fun phonesAndTabletsAreNotFoldable() {
        assertFalse(FoldLogic.isFoldable(listOf(pixel8, PanelSize.of(1440, 3120)), 420))
        assertFalse(FoldLogic.isFoldable(listOf(tablet), 320))
        assertFalse(FoldLogic.isFoldable(listOf(inner, outer), 0))
    }

    @Test fun tabletDockOnlyOnNonFoldableTablets() {
        assertTrue(FoldLogic.tabletDock(resourcesSayTablet = true, foldable = false))
        assertFalse(FoldLogic.tabletDock(resourcesSayTablet = true, foldable = true))
        assertFalse(FoldLogic.tabletDock(resourcesSayTablet = false, foldable = false))
        assertFalse(FoldLogic.tabletDock(resourcesSayTablet = false, foldable = true))
    }

    @Test fun sizeIsNormalized() {
        assertEquals(PanelSize.of(2424, 1080), outer)
        assertEquals("1080x2424", outer.toString())
        assertEquals(outer, PanelSize.parse("2424x1080"))
    }

    @Test fun rememberKeepsMostRecentWithoutRepeats() {
        var list = FoldLogic.remember(emptyList(), outer)
        list = FoldLogic.remember(list, inner)
        list = FoldLogic.remember(list, outer)
        assertEquals(listOf(outer, inner), list)
        val many = (1..10).fold(list) { acc, i -> FoldLogic.remember(acc, PanelSize.of(1000 + i, 2000)) }
        assertEquals(FoldLogic.MAX_REMEMBERED, many.size)
        assertEquals(PanelSize.of(1010, 2000), many.first())
    }

    @Test fun encodeRoundTripsAndRejectsDamage() {
        val list = listOf(outer, inner)
        assertEquals(list, FoldLogic.decode(FoldLogic.encode(list)))
        assertEquals(emptyList<PanelSize>(), FoldLogic.decode(null))
        assertEquals(listOf(outer), FoldLogic.decode("1080x2424,abc,0x10,-5x9,99999x2"))
        assertNull(PanelSize.parse("1080"))
    }
}
