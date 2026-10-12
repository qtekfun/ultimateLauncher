// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualAppsLogicTest {
    private val mail = "com.example.mail"
    private val notes = "org.notes"
    private val bank = "com.bank.app"

    private val profiles = mapOf(
        ProfileKind.MAIN to setOf(mail, notes, bank),
        ProfileKind.PRIVATE to setOf(mail),
        ProfileKind.WORK to setOf(notes, mail),
    )

    @Test fun onlyApi35InMainUser() {
        assertFalse(DualAppsLogic.isSupported(31, true))
        assertFalse(DualAppsLogic.isSupported(34, true))
        assertTrue(DualAppsLogic.isSupported(35, true))
        assertTrue(DualAppsLogic.isSupported(37, true))
        assertFalse(DualAppsLogic.isSupported(37, false))
    }

    @Test fun mainAndPrivateArePaired() {
        assertEquals(ProfileKind.PRIVATE, DualAppsLogic.counterpart(mail, ProfileKind.MAIN, profiles))
        assertEquals(ProfileKind.MAIN, DualAppsLogic.counterpart(mail, ProfileKind.PRIVATE, profiles))
    }

    @Test fun sameAppInThreeProfilesNeverPairsWithWork() {
        assertEquals(setOf(mail), DualAppsLogic.pairs(profiles))
        assertNull(DualAppsLogic.counterpart(mail, ProfileKind.WORK, profiles))
        assertNull(DualAppsLogic.counterpart(notes, ProfileKind.MAIN, profiles))
    }

    @Test fun appOnlyInOneProfileHasNoPair() {
        assertNull(DualAppsLogic.counterpart(bank, ProfileKind.MAIN, profiles))
        assertNull(DualAppsLogic.counterpart(bank, ProfileKind.PRIVATE, profiles))
    }

    @Test fun installAndUninstallChangePairs() {
        val installed = profiles + (ProfileKind.PRIVATE to setOf(mail, bank))
        assertEquals(setOf(mail, bank), DualAppsLogic.pairs(installed))
        val removed = profiles + (ProfileKind.PRIVATE to emptySet())
        assertTrue(DualAppsLogic.pairs(removed).isEmpty())
        assertNull(DualAppsLogic.counterpart(mail, ProfileKind.MAIN, removed))
    }

    @Test fun lockedProfileStillPairs() {
        // Un perfil bloqueado sigue listando sus actividades: el par existe y el lanzamiento pide desbloqueo.
        assertEquals(ProfileKind.PRIVATE, DualAppsLogic.counterpart(mail, ProfileKind.MAIN, profiles))
    }

    @Test fun privateSpaceIsPreferredOverClone() {
        val both = profiles + (ProfileKind.CLONED to setOf(mail, bank))
        assertEquals(ProfileKind.PRIVATE, DualAppsLogic.counterpart(mail, ProfileKind.MAIN, both))
        assertEquals(ProfileKind.CLONED, DualAppsLogic.counterpart(bank, ProfileKind.MAIN, both))
        assertEquals(ProfileKind.MAIN, DualAppsLogic.counterpart(bank, ProfileKind.CLONED, both))
    }

    @Test fun pickActivityPrefersSameClass() {
        assertEquals("b.Main", DualAppsLogic.pickActivity("b.Main", listOf("a.Other", "b.Main")))
        assertEquals("a.Other", DualAppsLogic.pickActivity("c.Gone", listOf("a.Other", "b.Main")))
        assertNull(DualAppsLogic.pickActivity("b.Main", emptyList()))
    }

    @Test fun addToHomeOnlyFromMainWhenMissing() {
        assertTrue(DualAppsLogic.offerAddToHome(ProfileKind.MAIN, ProfileKind.PRIVATE, false))
        assertFalse(DualAppsLogic.offerAddToHome(ProfileKind.MAIN, ProfileKind.PRIVATE, true))
        assertFalse(DualAppsLogic.offerAddToHome(ProfileKind.PRIVATE, ProfileKind.MAIN, false))
        assertFalse(DualAppsLogic.offerAddToHome(ProfileKind.MAIN, null, false))
    }

    @Test fun swipeUpNeedsVerticalDistanceOrSpeed() {
        assertTrue(DualAppsLogic.isSwipeUp(0f, -60f, 0f, 48f, 500f))
        assertTrue(DualAppsLogic.isSwipeUp(5f, -20f, -900f, 48f, 500f))
        assertFalse(DualAppsLogic.isSwipeUp(0f, -20f, -100f, 48f, 500f))
        assertFalse(DualAppsLogic.isSwipeUp(0f, 60f, 900f, 48f, 500f))
        assertFalse(DualAppsLogic.isSwipeUp(80f, -60f, -900f, 48f, 500f))
    }

    @Test fun lastUsedRoundTripsAndRejectsDamage() {
        val raw = DualAppsLogic.encodeLast(mail, ProfileKind.PRIVATE)
        assertEquals(mail to ProfileKind.PRIVATE, DualAppsLogic.decodeLast(raw))
        assertNull(DualAppsLogic.encodeLast(mail, ProfileKind.WORK))
        assertNull(DualAppsLogic.encodeLast("no package", ProfileKind.MAIN))
        assertNull(DualAppsLogic.decodeLast(null))
        assertNull(DualAppsLogic.decodeLast("$mail\nWORK"))
        assertNull(DualAppsLogic.decodeLast("$mail\nNOPE"))
        assertNull(DualAppsLogic.decodeLast("$mail"))
    }

    @Test fun configActiveRespectsHidePairs() {
        assertTrue(DualAppsConfig().active)
        assertFalse(DualAppsConfig(hidePairs = true).active)
        assertFalse(DualAppsConfig(enabled = false).active)
    }
}
