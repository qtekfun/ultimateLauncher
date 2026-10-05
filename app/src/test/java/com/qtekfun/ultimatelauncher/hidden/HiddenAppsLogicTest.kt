// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.hidden

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenAppsLogicTest {
    private val a = "com.example.mail/com.example.mail.Main"
    private val b = "org.notes/.NotesActivity"

    @Test fun serializeIsSortedDistinctAndRoundTrips() {
        val raw = HiddenAppsLogic.serialize(listOf(b, a, a))
        assertEquals("$a\n$b", raw)
        assertEquals(setOf(a, b), HiddenAppsLogic.parse(raw))
        assertEquals(raw, HiddenAppsLogic.serialize(HiddenAppsLogic.parse(raw)))
    }

    @Test fun emptyAndNullParseToEmpty() {
        assertTrue(HiddenAppsLogic.parse(null).isEmpty())
        assertTrue(HiddenAppsLogic.parse("").isEmpty())
        assertEquals("", HiddenAppsLogic.serialize(emptySet()))
    }

    @Test fun damagedValuesAreIgnoredOnRead() {
        val raw = "\n  \nnoslash\n/leading\ntrailing/\n$a\n  $b  \n"
        assertEquals(setOf(a, b), HiddenAppsLogic.parse(raw))
    }

    @Test fun invalidKeysAreNeverStored() {
        assertEquals("", HiddenAppsLogic.serialize(listOf("", "x", "a b\nc/d")))
        assertEquals(emptySet<String>(), HiddenAppsLogic.withHidden(emptySet(), "nope"))
        assertFalse(HiddenAppsLogic.isValidKey(null))
        assertTrue(HiddenAppsLogic.isValidKey(a))
    }

    @Test fun hideAndShowAreIdempotent() {
        val one = HiddenAppsLogic.withHidden(emptySet(), a)
        assertEquals(one, HiddenAppsLogic.withHidden(one, a))
        assertEquals(emptySet<String>(), HiddenAppsLogic.withShown(one, a))
        assertEquals(one, HiddenAppsLogic.withShown(one, b))
    }

    private data class App(val key: String?, val name: String)

    @Test fun filterVisibleKeepsOrderAndDropsHidden() {
        val apps = listOf(App(a, "Mail"), App(b, "Notes"), App("x.y/.Z", "Zed"), App(null, "NoComponent"))
        val visible = HiddenAppsLogic.filterVisible(apps, setOf(a, b)) { it.key }
        assertEquals(listOf("Zed", "NoComponent"), visible.map { it.name })
        assertEquals(apps, HiddenAppsLogic.filterVisible(apps, emptySet()) { it.key })
    }

    @Test fun onlyHiddenReturnsTheComplement() {
        val apps = listOf(App(a, "Mail"), App(b, "Notes"), App("x.y/.Z", "Zed"))
        assertEquals(listOf("Mail"), HiddenAppsLogic.onlyHidden(apps, setOf(a)) { it.key }.map { it.name })
        assertTrue(HiddenAppsLogic.onlyHidden(apps, emptySet()) { it.key }.isEmpty())
    }
}
