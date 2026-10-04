package com.qtekfun.ultimatelauncher.oem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemAdapterSelectionTest {
    @Test fun oppoSelectsColorOs() =
        assertEquals("coloros", OemAdapters.select(DeviceInfo("OPPO", "OPPO")).id)

    @Test fun oneplusAndRealmeSelectColorOs() {
        assertEquals("coloros", OemAdapters.select(DeviceInfo("OnePlus", "OnePlus")).id)
        assertEquals("coloros", OemAdapters.select(DeviceInfo("realme", "realme")).id)
    }

    @Test fun romPropertySelectsColorOsEvenWithUnknownBrand() =
        assertEquals("coloros", OemAdapters.select(DeviceInfo("X", "X", mapOf("ro.build.version.oplusrom" to "V16"))).id)

    @Test fun unknownBrandFallsBackToGeneric() =
        assertEquals("generic", OemAdapters.select(DeviceInfo("Google", "google")).id)

    @Test fun genericAlwaysMatches() = assertTrue(GenericAdapter().matches(DeviceInfo("", "")))

    @Test fun helpTextFallsBackToEnglish() =
        assertTrue(GenericAdapter().defaultLauncherHelp().forLang("fr").startsWith("Open Settings"))
}
