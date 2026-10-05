// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextMenuStyleTest {
    // Pantalla 1000x2000, tarjeta 400x300, hueco 20, margen 16.
    private fun place(cx: Int, top: Int, bottom: Int, maxH: Int = 0) =
        ContextMenuStyle.place(cx, top, bottom, 400, 300, 0, 0, 1000, 2000, 20, 16, maxH)

    @Test fun centeredAboveWhenItFits() {
        val p = place(500, 1000, 1100)
        assertTrue(p.above); assertTrue(p.fits)
        assertEquals(300, p.x)
        assertEquals(1000 - 20 - 300, p.y)
    }

    @Test fun clampedToLeftAndRightMargins() {
        assertEquals(16, place(50, 1000, 1100).x)
        assertEquals(1000 - 16 - 400, place(980, 1000, 1100).x)
    }

    @Test fun goesBelowWhenNoRoomAbove() {
        val p = place(500, 200, 300)
        assertFalse(p.above); assertTrue(p.fits)
        assertEquals(320, p.y)
    }

    @Test fun maxHeightDecidesSideButNotPosition() {
        // Con altura máxima prevista de 1100 ya no cabe encima aunque la actual (300) sí.
        assertFalse(place(500, 1000, 1100, maxH = 1100).above)
    }

    @Test fun doesNotFitAnywhere() {
        assertFalse(ContextMenuStyle.place(500, 900, 1000, 400, 1900, 0, 0, 1000, 2000, 20, 16).fits)
    }

    @Test fun cardWiderThanScreenIsCentered() {
        assertEquals(-50, ContextMenuStyle.place(500, 1000, 1100, 1100, 300, 0, 0, 1000, 2000, 20, 16).x)
    }

    @Test fun pivotStaysInsideTheCard() {
        assertEquals(0f, ContextMenuStyle.pivotX(10, 100, 400), 0f)
        assertEquals(400f, ContextMenuStyle.pivotX(900, 100, 400), 0f)
        assertEquals(150f, ContextMenuStyle.pivotX(250, 100, 400), 0f)
    }
}
