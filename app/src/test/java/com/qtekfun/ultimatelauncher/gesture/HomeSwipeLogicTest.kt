// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSwipeLogicTest {
    private val on = HomeSwipeConfig(enabled = true)
    private val width = 1000f
    private val minDist = 150f
    private val minFling = 1500f

    private fun classify(cfg: HomeSwipeConfig, downX: Float, dx: Float, dy: Float, vy: Float = 0f) =
        HomeSwipeLogic.classify(cfg, downX, width, dx, dy, vy, minDist, minFling)

    @Test fun disabledByDefault() {
        assertFalse(HomeSwipeConfig().enabled)
        assertEquals(Panel.NONE, classify(HomeSwipeConfig(), 100f, 0f, 400f))
    }

    @Test fun leftHalfOpensNotificationsRightHalfOpensQuickSettings() {
        assertEquals(Panel.NOTIFICATIONS, classify(on, 100f, 0f, 400f))
        assertEquals(Panel.NOTIFICATIONS, classify(on, 499f, 0f, 400f))
        assertEquals(Panel.QUICK_SETTINGS, classify(on, 500f, 0f, 400f))
        assertEquals(Panel.QUICK_SETTINGS, classify(on, 900f, 0f, 400f))
    }

    @Test fun swappedInvertsTheHalves() {
        val swapped = on.copy(swapped = true)
        assertEquals(Panel.QUICK_SETTINGS, classify(swapped, 100f, 0f, 400f))
        assertEquals(Panel.NOTIFICATIONS, classify(swapped, 900f, 0f, 400f))
    }

    @Test fun withoutSplitWholeScreenIsOneAction() {
        val whole = on.copy(splitHalves = false)
        assertEquals(Panel.NOTIFICATIONS, classify(whole, 100f, 0f, 400f))
        assertEquals(Panel.NOTIFICATIONS, classify(whole, 900f, 0f, 400f))
        val wholeQs = whole.copy(swapped = true)
        assertEquals(Panel.QUICK_SETTINGS, classify(wholeQs, 100f, 0f, 400f))
        assertEquals(Panel.QUICK_SETTINGS, classify(wholeQs, 900f, 0f, 400f))
    }

    @Test fun upwardAndZeroMovementNeverOpenAPanel() {
        assertEquals(Panel.NONE, classify(on, 100f, 0f, -400f, -5000f))
        assertEquals(Panel.NONE, classify(on, 100f, 0f, 0f, 9999f))
    }

    @Test fun shortSlowSwipeIsNotConfirmedButAFlickIs() {
        assertEquals(Panel.NONE, classify(on, 100f, 0f, 100f, 300f))
        assertEquals(Panel.NOTIFICATIONS, classify(on, 100f, 0f, 100f, 2000f))
        assertEquals(Panel.NOTIFICATIONS, classify(on, 100f, 0f, 150f, 0f))
    }

    @Test fun mostlyHorizontalMovementIsLeftToPaging() {
        assertEquals(Panel.NONE, classify(on, 100f, 500f, 400f))
        assertEquals(Panel.NOTIFICATIONS, classify(on, 100f, 300f, 400f))
        assertEquals(Panel.NOTIFICATIONS, classify(on, 100f, -300f, 400f))
    }

    @Test fun zeroWidthDoesNotCrashAndUsesLeftAction() {
        assertEquals(Panel.NOTIFICATIONS, HomeSwipeLogic.panelForStart(on, 10f, 0f))
    }

    @Test fun statusBarMethodNames() {
        assertTrue(StatusBarPanels.methodName(Panel.NOTIFICATIONS) == "expandNotificationsPanel")
        assertTrue(StatusBarPanels.methodName(Panel.QUICK_SETTINGS) == "expandSettingsPanel")
        assertTrue(StatusBarPanels.methodName(Panel.NONE) == null)
    }
}
