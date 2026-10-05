// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.orientation

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOrientationLogicTest {
    private fun choose(large: Boolean, auto: Boolean, allow: Boolean, land: Boolean?) =
        ScreenOrientationLogic.choose(large, auto, allow, land)

    @Test fun largeScreenThreshold() {
        assertFalse(ScreenOrientationLogic.isLargeScreen(599))
        assertTrue(ScreenOrientationLogic.isLargeScreen(600))
        assertTrue(ScreenOrientationLogic.isLargeScreen(800))
    }

    @Test fun tabletLockedRotationInheritsLandscapeFromLauncher() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, choose(true, false, false, true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, choose(true, false, true, true))
    }

    @Test fun tabletLockedRotationInheritsPortraitFromLauncher() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, choose(true, false, false, false))
    }

    @Test fun tabletLockedRotationWithoutHintFollowsSystem() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, choose(true, false, false, null))
    }

    @Test fun tabletAutoRotateFollowsSensorLikeLauncher() {
        for (land in listOf(true, false, null))
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, choose(true, true, false, land))
    }

    @Test fun phoneRespectsHomeRotationPreference() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR, choose(false, true, false, null))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR, choose(false, false, false, true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, choose(false, true, true, null))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, choose(false, false, true, false))
    }

    @Test fun phoneIgnoresLauncherHint() {
        assertEquals(choose(false, false, false, true), choose(false, false, false, false))
    }
}
