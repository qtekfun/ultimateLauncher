// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.grid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeGridTest {
    @Test fun matePadLandscapeFillsWidth() {
        // 2800 px, 7 columnas, sin separación: 400 px por celda = 2800 exactos
        assertEquals(400, EdgeGrid.cellWidthPx(2800, 1, 7, 0))
    }

    @Test fun matePadPortraitFillsWidth() {
        // 1840 / 7 = 262,86 -> 262; sobran 6 px (< numColumns)
        val w = EdgeGrid.cellWidthPx(1840, 1, 7, 0)
        assertEquals(262, w)
        assertTrue(1840 - w * 7 < 7)
    }

    @Test fun borderSpaceIsSubtracted() {
        val w = EdgeGrid.cellWidthPx(2800, 1, 7, 10)
        assertTrue(w * 7 + 10 * 6 <= 2800)
        assertTrue(2800 - (w * 7 + 10 * 6) < 7)
    }

    @Test fun twoPanelsSplitWidth() {
        assertEquals(200, EdgeGrid.cellWidthPx(2800, 2, 7, 0))
    }

    @Test fun degenerateInputsGiveZero() {
        assertEquals(0, EdgeGrid.cellWidthPx(0, 1, 7, 0))
        assertEquals(0, EdgeGrid.cellWidthPx(2800, 0, 7, 0))
        assertEquals(0, EdgeGrid.cellWidthPx(2800, 1, 0, 0))
    }

    @Test fun onlyTabletsAreEligible() {
        assertTrue(EdgeGrid.eligible(818f, false, false))
        assertTrue(EdgeGrid.eligible(600f, false, false))
        assertFalse(EdgeGrid.eligible(391f, false, false)) // teléfono
        assertFalse(EdgeGrid.eligible(818f, true, false)) // plegable de dos paneles
        assertFalse(EdgeGrid.eligible(818f, false, true)) // disposición vertical
    }
}
