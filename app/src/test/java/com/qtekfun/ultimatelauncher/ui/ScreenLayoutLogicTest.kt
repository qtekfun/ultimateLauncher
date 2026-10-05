// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLayoutLogicTest {
    @Test fun phoneUsesAllWidth() = assertEquals(411 * 3, ScreenLayoutLogic.contentWidth(411 * 3, 640 * 3))

    @Test fun tabletLandscapeIsCappedAt640dp() = assertEquals(640 * 2, ScreenLayoutLogic.contentWidth(1280 * 2, 640 * 2))

    @Test fun exactlyAtLimit() = assertEquals(1000, ScreenLayoutLogic.contentWidth(1000, 1000))

    @Test fun zeroMaxMeansNoLimit() = assertEquals(900, ScreenLayoutLogic.contentWidth(900, 0))
}
