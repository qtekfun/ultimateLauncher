// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.iconpack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {
    private val sample = """<?xml version="1.0" encoding="utf-8"?>
        <!-- pack sintético de prueba -->
        <resources>
            <iconback img1="back_a" img2="back_b" />
            <iconmask img1="mask" />
            <iconupon img1="@drawable/upon" />
            <scale factor="0.82" />
            <item component="ComponentInfo{com.example.cam/com.example.cam.Main}" drawable="cam" />
            <item component="ComponentInfo{com.example.cal/.CalActivity}" drawable='@drawable/cal.png' />
            <item component="ComponentInfo{com.example.cam/com.example.cam.Main}" drawable="cam_duplicate" />
            <item component="ComponentInfo{sin barra}" drawable="x" />
            <item drawable="sin_componente" />
            <item component="ComponentInfo{a.b/a.b.C}" drawable="" />
            <calendar component="ComponentInfo{com.example.cal/.CalActivity}" prefix="cal_" />
        </resources>"""

    @Test fun readsItemsAndNormalizesComponents() {
        val d = AppFilter.parseText(sample)
        assertEquals(2, d.icons.size)
        assertEquals("cam", d.icons["com.example.cam/com.example.cam.Main"]) // la primera entrada gana
        assertEquals("cal", d.icons["com.example.cal/com.example.cal.CalActivity"])
        assertFalse(d.truncated)
    }

    @Test fun readsBackMaskUponAndScale() {
        val d = AppFilter.parseText(sample)
        assertEquals(listOf("back_a", "back_b"), d.backs)
        assertEquals("mask", d.mask)
        assertEquals("upon", d.upon)
        assertEquals(0.82f, d.scale, 0.0001f)
    }

    @Test fun backChoiceIsStablePerPackage() {
        val d = AppFilter.parseText(sample)
        assertEquals(d.backFor("com.foo"), d.backFor("com.foo"))
        assertTrue(d.backFor("com.foo") in d.backs)
    }

    @Test fun emptyOrGarbageGivesEmptyData() {
        assertTrue(AppFilter.parseText("").isEmpty)
        assertTrue(AppFilter.parseText("esto no es xml <<< >>> &&&").isEmpty)
        assertTrue(AppFilter.parseText("<resources><item component=\"x\" ").isEmpty)
    }

    @Test fun toleratesBomEntitiesCdataAndUnquotedValues() {
        val x = "﻿<resources><![CDATA[<item component=\"no/cuenta\" drawable=\"no\"/>]]>" +
            "<item component=\"ComponentInfo{a.b/a.b.C&amp;D}\" drawable=unquoted />" +
            "<item component=&quot;mal&quot; drawable=\"z\"/></resources>"
        val d = AppFilter.parseText(x)
        assertEquals(mapOf("a.b/a.b.C&D" to "unquoted"), d.icons)
    }

    @Test fun scaleIsClampedAndIgnoredWhenInvalid() {
        assertEquals(1.5f, AppFilter.parseText("<scale factor=\"9\"/>").scale, 0f)
        assertEquals(0.3f, AppFilter.parseText("<scale factor=\"0.01\"/>").scale, 0f)
        assertEquals(1f, AppFilter.parseText("<scale factor=\"abc\"/>").scale, 0f)
        assertEquals(1f, AppFilter.parseText("<scale factor=\"NaN\"/>").scale, 0f)
    }

    @Test fun itemLimitTruncates() {
        val sb = StringBuilder("<resources>")
        for (i in 0 until AppFilter.MAX_ITEMS + 50) sb.append("<item component=\"p$i/p$i.A\" drawable=\"d$i\"/>")
        sb.append("</resources>")
        val d = AppFilter.parseText(sb.toString())
        assertEquals(AppFilter.MAX_ITEMS, d.icons.size)
        assertTrue(d.truncated)
    }

    @Test fun oversizedTextIsClipped() {
        val filler = " ".repeat(AppFilter.MAX_BYTES + 10)
        val d = AppFilter.parseText("<resources>" + filler + "<item component=\"a/a.B\" drawable=\"late\"/></resources>")
        assertTrue(d.truncated)
        assertTrue(d.icons.isEmpty()) // el ítem quedó fuera del tramo leído
    }

    @Test fun overlongAttributesAreDropped() {
        val long = "x".repeat(AppFilter.MAX_ATTR_LEN + 5)
        val d = AppFilter.parseText("<item component=\"a/a.B\" drawable=\"$long\"/>")
        assertTrue(d.icons.isEmpty())
    }

    @Test fun componentNormalization() {
        assertEquals("a.b/a.b.C", AppFilter.normalizeComponent("ComponentInfo{a.b/a.b.C}"))
        assertEquals("a.b/a.b.C", AppFilter.normalizeComponent("a.b/.C"))
        assertEquals("a.b/a.b.C", AppFilter.normalizeComponent("  ComponentInfo{ a.b/a.b.C }  "))
        assertNull(AppFilter.normalizeComponent("a.b"))
        assertNull(AppFilter.normalizeComponent("/x"))
        assertNull(AppFilter.normalizeComponent("a.b/"))
        assertNull(AppFilter.normalizeComponent(null))
    }

    @Test fun tagsFromPullParserAreAccepted() {
        val tags = listOf(
            AppFilter.Tag("item", mapOf("component" to "ComponentInfo{p/p.A}", "drawable" to "ic")),
            AppFilter.Tag("iconback", mapOf("img2" to "b2", "img1" to "b1")),
        )
        val d = AppFilter.parseTags(tags.iterator())
        assertEquals("ic", d.icons["p/p.A"])
        assertEquals(listOf("b1", "b2"), d.backs)
    }
}
