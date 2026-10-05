// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperLogicTest {
    @Test fun squareToPortraitPhoneCropsSidesCentered() {
        val c = WallpaperLogic.coverCrop(2304, 2304, 1080, 2400)
        assertEquals(2304, c.height)
        assertEquals(0, c.top)
        assertEquals(1036, c.width) // 2304 * 1080 / 2400
        assertEquals((2304 - 1036) / 2, c.left)
    }

    @Test fun squareToLandscapeTabletCropsTopAndBottomCentered() {
        val c = WallpaperLogic.coverCrop(2304, 2304, 2800, 1840)
        assertEquals(2304, c.width)
        assertEquals(0, c.left)
        assertEquals(1514, c.height) // 2304 * 1840 / 2800
        assertEquals((2304 - 1514) / 2, c.top)
    }

    @Test fun cropKeepsTargetAspectWithinOnePixel() {
        for ((dw, dh) in listOf(1080 to 2400, 2800 to 1840, 1840 to 2800, 1200 to 1920, 1600 to 1600)) {
            val c = WallpaperLogic.coverCrop(2304, 2304, dw, dh)
            assertEquals(dw.toDouble() / dh, c.width.toDouble() / c.height, 0.002)
            assertTrue(c.left >= 0 && c.top >= 0 && c.right <= 2304 && c.bottom <= 2304)
        }
    }

    @Test fun wideSourceKeepsFullHeight() {
        val c = WallpaperLogic.coverCrop(4000, 1000, 1000, 1000)
        assertEquals(1000, c.width)
        assertEquals(1000, c.height)
        assertEquals(1500, c.left)
    }

    @Test fun outputSizeIsScreenUnlessTooLarge() {
        assertEquals(1080 to 2400, WallpaperLogic.outputSize(1080, 2400, 3000))
        val (w, h) = WallpaperLogic.outputSize(4000, 2000, 2000)
        assertEquals(2000, w); assertEquals(1000, h)
    }

    @Test fun blurRadiusGrowsWithPercent() {
        assertEquals(0, WallpaperLogic.blurRadius(0, 360))
        assertTrue(WallpaperLogic.blurRadius(10, 360) >= 1)
        assertTrue(WallpaperLogic.blurRadius(100, 360) > WallpaperLogic.blurRadius(50, 360))
    }

    @Test fun boxBlurKeepsUniformImageAndSmoothsEdges() {
        val w = 16; val h = 16
        val flat = IntArray(w * h) { 0xFF336699.toInt() }
        WallpaperLogic.boxBlur(flat, w, h, 3)
        assertTrue(flat.all { it == 0xFF336699.toInt() })

        val checker = IntArray(w * h) { if ((it % w + it / w) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        WallpaperLogic.boxBlur(checker, w, h, 2)
        val mid = checker[8 * w + 8] and 0xFF
        assertTrue("el tablero queda gris: $mid", mid in 90..170)
    }

    @Test fun dimAlphaIsMonotonicAndBounded() {
        assertEquals(0, WallpaperLogic.dimAlpha(0))
        assertEquals(217, WallpaperLogic.dimAlpha(100)) // 85 % de 255
        assertTrue(WallpaperLogic.dimAlpha(50) in 100..120)
        assertEquals(WallpaperLogic.dimAlpha(100), WallpaperLogic.dimAlpha(400))
    }

    @Test fun gridColumnsGrowWithWidth() {
        assertEquals(2, WallpaperLogic.gridColumns(360f))
        assertEquals(5, WallpaperLogic.gridColumns(851f)) // móvil horizontal
        assertEquals(6, WallpaperLogic.gridColumns(1280f)) // tablet horizontal
        assertEquals(2, WallpaperLogic.gridColumns(100f))
    }

    @Test fun cellAspectIsClampedAndHasFallback() {
        assertEquals(1.45f, WallpaperLogic.cellAspect(1080, 2400), 0f)
        assertEquals(0.657f, WallpaperLogic.cellAspect(2800, 1840), 1e-3f)
        assertEquals(0.55f, WallpaperLogic.cellAspect(3000, 1000), 0f)
        assertEquals(1.45f, WallpaperLogic.cellAspect(0, 0), 0f)
    }

    @Test fun panelWidthIsCardInLandscapeAndFullInPortraitPhone() {
        assertEquals(336, WallpaperLogic.panelWidthDp(360f)) // móvil vertical: casi todo el ancho
        assertEquals(460, WallpaperLogic.panelWidthDp(1280f))
        assertEquals(460, WallpaperLogic.panelWidthDp(851f))
    }
}
