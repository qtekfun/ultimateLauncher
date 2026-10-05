package com.qtekfun.ultimatelauncher.clockwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockWidgetLogicTest {
    @Test fun twoByTwoUsesSquare() = assertEquals(ClockLayout.SQUARE, ClockWidgetLogic.choose(146f, 146f))
    @Test fun fourByTwoUsesWide() = assertEquals(ClockLayout.WIDE, ClockWidgetLogic.choose(300f, 146f))
    @Test fun fourByOneUsesCompact() = assertEquals(ClockLayout.COMPACT, ClockWidgetLogic.choose(300f, 60f))
    @Test fun twoByOneUsesCompact() = assertEquals(ClockLayout.COMPACT, ClockWidgetLogic.choose(146f, 60f))
    @Test fun narrowTallUsesSquare() = assertEquals(ClockLayout.SQUARE, ClockWidgetLogic.choose(110f, 300f))
    @Test fun smallerThanAnyFallsBackToSmallest() = assertEquals(ClockLayout.COMPACT, ClockWidgetLogic.choose(50f, 20f))
    @Test fun exactBoundariesFit() {
        assertEquals(ClockLayout.WIDE, ClockWidgetLogic.choose(200f, 100f))
        assertEquals(ClockLayout.SQUARE, ClockWidgetLogic.choose(199f, 100f))
        assertEquals(ClockLayout.COMPACT, ClockWidgetLogic.choose(200f, 99f))
    }

    @Test fun datePatternUsesResolver() =
        assertEquals("EEE, MMM d", ClockWidgetLogic.datePattern(ClockWidgetLogic.SKELETON_DAY_DATE) { "EEE, MMM d" })

    @Test fun datePatternFallsBackOnBlank() {
        assertEquals(ClockWidgetLogic.FALLBACK_DATE_PATTERN, ClockWidgetLogic.datePattern("x") { "  " })
        assertEquals(ClockWidgetLogic.FALLBACK_DATE_PATTERN, ClockWidgetLogic.datePattern("x") { null })
    }
}
