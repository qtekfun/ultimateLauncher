// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.drawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzBarLogicTest {
    // Medidas de OPPO en 1080x2400 (3,5 px/dp): 24 letras, paso 48, primera letra en 984.
    private val pitch = 48
    private val top = 984

    @Test fun portraitKeepsMeasuredDesign() {
        val f = AzBarLogic.fit(2400, 24, pitch, top, 196, 56)
        assertEquals(AzBarLogic.Fit(top, pitch, 1f), f)
    }

    @Test fun unmeasuredParentKeepsDesign() {
        assertEquals(AzBarLogic.Fit(top, pitch, 1f), AzBarLogic.fit(0, 24, pitch, top, 196, 56))
    }

    @Test fun landscapeTabletMovesBarUpWhenItFits() {
        // 1840 de alto: 24*48 = 1152; con 196 de hueco la barra puede acabar en 1644 -> empieza en 492.
        val f = AzBarLogic.fit(1840, 24, pitch, top, 196, 56)
        assertEquals(492, f.top)
        assertEquals(pitch, f.pitch)
        assertEquals(1f, f.textScale, 0f)
        assertTrue(f.top + 24 * f.pitch <= 1840 - 196)
    }

    @Test fun landscapePhoneShrinksPitchAndText() {
        val f = AzBarLogic.fit(1080, 24, pitch, top, 196, 56)
        assertEquals(56, f.top)
        assertTrue(f.pitch < pitch)
        assertTrue(f.top + 24 * f.pitch <= 1080 - 196)
        assertEquals(f.pitch.toFloat() / pitch, f.textScale, 1e-6f)
    }

    @Test fun neverBelowOnePixelOrOverflowingBottom() {
        for (h in listOf(100, 300, 600, 900, 1200, 1800, 2400, 3000)) {
            val f = AzBarLogic.fit(h, 27, pitch, top, 196, 56)
            assertTrue(f.pitch >= 1)
            assertTrue(f.textScale in 0f..1f)
            if (h - 196 - 56 >= 27) assertTrue("h=$h", f.top + 27 * f.pitch <= h - 196)
        }
    }

    @Test fun fewLettersNeverGrow() {
        val f = AzBarLogic.fit(1080, 5, pitch, top, 196, 56)
        assertEquals(pitch, f.pitch)
        assertEquals(1f, f.textScale, 0f)
    }
}
