// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.dock

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentsLogicTest {
    private val a = "com.a/.Main"
    private val b = "com.b/.Main"
    private val c = "com.c/.Main"
    private val d = "com.d/.Main"

    @Test fun recordPutsNewestFirstWithoutDuplicates() {
        assertEquals(listOf(b, a, c), RecentsLogic.record(listOf(a, b, c), b, 4))
        assertEquals(listOf(c, a, b), RecentsLogic.record(listOf(a, b, c), c, 4))
    }

    @Test fun recordTrimsToMax() =
        assertEquals(listOf(d, a, b), RecentsLogic.record(listOf(a, b, c), d, 3))

    @Test fun removeDropsOnlyThatEntry() =
        assertEquals(listOf(a, c), RecentsLogic.remove(listOf(a, b, c), b))

    @Test fun removeMissingEntryKeepsList() =
        assertEquals(listOf(a, b), RecentsLogic.remove(listOf(a, b), c))

    @Test fun pruneRemovesUninstalled() =
        assertEquals(listOf(a, c), RecentsLogic.prune(listOf(a, b, c)) { it != b })

    @Test fun pruneKeepsOrder() =
        assertEquals(listOf(c, a), RecentsLogic.prune(listOf(c, b, a)) { it != b })

    @Test fun parseIgnoresBlanksAndRoundTrips() {
        assertEquals(emptyList<String>(), RecentsLogic.parse(""))
        assertEquals(listOf(a, b), RecentsLogic.parse(RecentsLogic.serialize(listOf(a, b))))
    }
}
