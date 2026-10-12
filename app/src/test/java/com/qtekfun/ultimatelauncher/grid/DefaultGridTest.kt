// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.grid

import com.android.launcher3.InvariantDeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultGridTest {
    @Test fun phoneAndTabletUseMeasuredGrids() {
        assertEquals("ultimate_phone", DefaultGrid.forDeviceType(InvariantDeviceProfile.TYPE_PHONE))
        assertEquals("ultimate_tablet", DefaultGrid.forDeviceType(InvariantDeviceProfile.TYPE_TABLET))
    }

    @Test fun foldableAndDesktopUseAospGrids() {
        assertNull(DefaultGrid.forDeviceType(InvariantDeviceProfile.TYPE_MULTI_DISPLAY))
        assertNull(DefaultGrid.forDeviceType(InvariantDeviceProfile.TYPE_DESKTOP))
    }
}
