package com.qtekfun.ultimatelauncher.layoutsync

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Esquema v2 (ajustes del launcher en la copia), retrocompatibilidad con v1 y «Colocar mis apps por orden». */
class BackupSchemaTest {
    private val dev = Device("tablet", "HUAWEI", "BAH", 33, 280, 800, 1280)
    private fun snap(prefs: Map<String, Any> = emptyMap(), pages: List<Page> = emptyList()) =
        Snapshot("2026-10-05T00:00:00Z", dev, Settings(Grid(5, 7)), emptyList(), pages, prefs)

    @Test fun prefsRoundTrip() {
        val prefs = mapOf("pref_ul_edge_grid" to true, "pref_ul_folder_style" to false, "pref_add_icon_to_home" to true)
        val s = snap(prefs)
        val back = LayoutJson.fromJson(LayoutJson.toJson(s))
        assertEquals(prefs, back.prefs); assertEquals(s, back)
        assertTrue(LayoutJson.toJson(s).contains("\"schema\": 2"))
    }

    @Test fun unknownKeyAndWrongTypeAreIgnoredOnRestore() {
        val json = LayoutJson.toJson(snap(mapOf("pref_ul_edge_grid" to true)))
            .replace("\"pref_ul_edge_grid\": true", "\"pref_ul_edge_grid\": true, \"pref_ul_folder_style\": \"yes\", \"pref_ul_open_scale\": 3, " +
                "\"evil_key\": true, \"idp_grid_name\": \"x\", \"pref_ul_return_pop\": null")
        assertEquals(mapOf("pref_ul_edge_grid" to true), LayoutJson.fromJson(json).prefs)
    }

    @Test fun v1FileStillReadsWithoutPrefs() {
        val v1 = org.json.JSONObject(LayoutJson.toJson(snap(mapOf("pref_ul_edge_grid" to true)))).also { it.put("schema", 1); it.remove("prefs") }.toString()
        val s = LayoutJson.fromJson(v1)
        assertTrue(s.prefs.isEmpty()); assertEquals(Grid(5, 7), s.settings.grid)
    }

    @Test fun planCarriesPrefsAndSummaryMentionsThem() {
        val st = DeviceState(emptyMap(), emptySet(), Grid(5, 7), 5, false)
        val plan = ImportPlanner.plan(snap(mapOf("pref_ul_dock_recents" to false)), st)
        assertEquals(mapOf("pref_ul_dock_recents" to false), plan.prefs)
        assertTrue(plan.summary().contains("1 ajuste"))
        assertTrue(ImportPlanner.plan(snap(), st).prefs.isEmpty())
    }

    @Test fun filterRejectsEverythingOutsideTheWhitelist() {
        val out = BackupPrefs.filter(mapOf("pref_ul_edge_grid" to 1, "pref_ul_dock_subtle" to true, "other" to true))
        assertEquals(mapOf("pref_ul_dock_subtle" to true), out)
    }

    /** Guardia: todo interruptor persistente `pref_ul_*` de launcher_preferences.xml debe estar en la lista blanca. */
    @Test fun whitelistCoversEveryPersistentUlPreference() {
        val f = listOf("../launcher3-base/res/xml/launcher_preferences.xml", "launcher3-base/res/xml/launcher_preferences.xml").map(::File).first { it.exists() }
        val xml = f.readText()
        val blocks = Regex("<SwitchPreference[^>]*?>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { it.value }
        val keys = blocks.mapNotNull { Regex("android:key=\"([^\"]+)\"").find(it)?.groupValues?.get(1) }.filter { it.startsWith("pref_ul_") }.toList()
        assertTrue(keys.isNotEmpty())
        keys.forEach { assertTrue("$it falta en BackupPrefs.SPEC", it in BackupPrefs.SPEC) }
    }

    // --- Colocar mis apps por orden ---
    private fun a(p: String, label: String, t: Long) = AppEntry(p, "$p.Main", label, t)
    private val apps = listOf(a("c", "Cámara", 30), a("a", "Ajustes", 10), a("z", "Zeta", 20), a("b", "banco", 40), a("e", "Énfasis", 5))

    @Test fun alphabeticalOrderIgnoresCaseAndAccents() {
        val s = AppsByOrder.sorted(apps, AppOrder.ALPHABETICAL, Locale("es"))
        assertEquals(listOf("Ajustes", "banco", "Cámara", "Énfasis", "Zeta"), s.map { it.label })
    }

    @Test fun installOrderPutsOldestFirst() {
        assertEquals(listOf("e", "a", "z", "c", "b"), AppsByOrder.sorted(apps, AppOrder.INSTALL_DATE).map { it.pkg })
    }

    @Test fun pagesSplitByPerPageAndFillRowsLeftToRight() {
        val p = AppsByOrder.pages(apps, AppOrder.INSTALL_DATE, 3, Grid(2, 3))
        assertEquals(2, p.size); assertEquals(3, p[0].items.size); assertEquals(2, p[1].items.size)
        assertEquals(listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1)), p[0].items.map { it.cell })
    }

    @Test fun perPageIsClampedToGridCapacity() {
        val many = (0 until 30).map { a("p$it", "App$it", it.toLong()) }
        val p = AppsByOrder.pages(many, AppOrder.INSTALL_DATE, 999, Grid(4, 5))
        assertEquals(20, p[0].items.size); assertEquals(2, p.size)
        assertTrue(AppsByOrder.pages(many, AppOrder.INSTALL_DATE, 0, Grid(4, 5)).all { it.items.size == 1 })
    }

    @Test fun appsAlreadyInDockAreNotRepeatedAndPlanKeepsDock() {
        val dock = listOf(0 to (Item.App("a", "a.Main") as Item))
        val s = AppsByOrder.snapshot(apps, AppOrder.ALPHABETICAL, 20, Grid(4, 5), dock, dev)
        val st = DeviceState(apps.associate { it.pkg to listOf(it.activity) }, emptySet(), Grid(4, 5), 5, false)
        val plan = ImportPlanner.plan(s, st)
        assertEquals(1, plan.hotseat.size)
        assertFalse(plan.pages.flatten().any { it is Item.App && it.pkg == "a" })
        assertEquals(4, plan.pages.sumOf { it.size }); assertTrue(plan.omitted.isEmpty())
    }

    @Test fun suggestedNameUsesIsoDate() {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-05")!!
        assertEquals("ultimatelauncher-2026-10-05.json", BackupPrefs.suggestedName(Date(d.time)))
    }
}
