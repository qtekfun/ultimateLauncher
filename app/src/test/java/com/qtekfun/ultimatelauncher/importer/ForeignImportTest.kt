// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.importer

import com.qtekfun.ultimatelauncher.layoutsync.DeviceState
import com.qtekfun.ultimatelauncher.layoutsync.Cell
import com.qtekfun.ultimatelauncher.layoutsync.GridReflow
import com.qtekfun.ultimatelauncher.layoutsync.Grid
import com.qtekfun.ultimatelauncher.layoutsync.ImportPlanner
import com.qtekfun.ultimatelauncher.layoutsync.Item
import com.qtekfun.ultimatelauncher.layoutsync.Page
import com.qtekfun.ultimatelauncher.layoutsync.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Pruebas del importador con bases SQLite SINTÉTICAS generadas aquí (ningún dato personal ni de ningún launcher real). */
class ForeignImportTest {
    @get:Rule val tmp = TemporaryFolder()

    // ---- utilidades ------------------------------------------------------------------------------------------
    private fun intentOf(pkg: String, cls: String, extra: String = "") =
        "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=$pkg/${cls.replace("$", "%24")}$extra;end"

    private class Row(val id: Int, val type: Int, val container: Int, val screen: Int, val x: Int, val y: Int, val sx: Int = 1, val sy: Int = 1,
                      val intent: String? = null, val title: String = "", val provider: String? = null, val profile: Int = 0, val rank: Int = 0)

    /** Crea un launcher.db con el esquema de Launcher3 (las columnas que usa el importador, más un BLOB `icon` antiguo). */
    private fun makeDb(rows: List<Row>, screens: List<Pair<Int, Int>>? = null, name: String = "launcher.db"): File {
        val f = File(tmp.root, name)
        DriverManager.getConnection("jdbc:sqlite:${f.path}").use { c ->
            c.createStatement().use {
                it.execute("CREATE TABLE favorites (_id INTEGER PRIMARY KEY, title TEXT, intent TEXT, container INTEGER, screen INTEGER, cellX INTEGER, cellY INTEGER, " +
                    "spanX INTEGER, spanY INTEGER, itemType INTEGER, appWidgetProvider TEXT, profileId INTEGER, rank INTEGER, icon BLOB)")
                if (screens != null) it.execute("CREATE TABLE workspaceScreens (_id INTEGER PRIMARY KEY, screenRank INTEGER)")
            }
            c.prepareStatement("INSERT INTO favorites VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)").use { p ->
                for (r in rows) {
                    p.setInt(1, r.id); p.setString(2, r.title); p.setString(3, r.intent); p.setInt(4, r.container); p.setInt(5, r.screen)
                    p.setInt(6, r.x); p.setInt(7, r.y); p.setInt(8, r.sx); p.setInt(9, r.sy); p.setInt(10, r.type); p.setString(11, r.provider)
                    p.setInt(12, r.profile); p.setInt(13, r.rank); p.setBytes(14, ByteArray(100)); p.executeUpdate()
                }
            }
            screens?.forEach { (id, rank) -> c.createStatement().use { it.execute("INSERT INTO workspaceScreens VALUES ($id,$rank)") } }
        }
        return f
    }

    private class JdbcReader(file: File) : TableReader, AutoCloseable {
        val c: Connection = DriverManager.getConnection("jdbc:sqlite:${file.path}")
        override fun columns(table: String): Set<String> = c.createStatement().use { s ->
            s.executeQuery("PRAGMA table_info($table)").use { rs -> val out = mutableSetOf<String>(); while (rs.next()) out += rs.getString("name"); out } }
        override fun rows(table: String, wanted: List<String>, maxRows: Int): List<Map<String, Any?>> = c.createStatement().use { s ->
            s.executeQuery("SELECT ${wanted.joinToString()} FROM $table LIMIT $maxRows").use { rs ->
                val out = mutableListOf<Map<String, Any?>>()
                while (rs.next()) out += wanted.associateWith { rs.getObject(it) }
                out } }
        override fun close() = c.close()
    }

    private fun parse(f: File, limits: ImportLimits = ImportLimits()) = JdbcReader(f).use { ForeignLayoutParser.parse(it, limits, "2026-01-01T00:00:00Z") }

    private val phoneRows = listOf(
        Row(1, 0, -100, 0, 0, 0, intent = intentOf("org.fake.mail", "org.fake.mail.Main")),
        Row(2, 0, -100, 0, 1, 0, intent = intentOf("org.fake.maps", "org.fake.maps.Main")),
        Row(3, 2, -100, 0, 2, 0, title = "Juegos"),
        Row(4, 0, 3, 0, 0, 0, intent = intentOf("org.fake.chess", "org.fake.chess.Main"), rank = 1),
        Row(5, 0, 3, 0, 1, 0, intent = intentOf("org.fake.cards", "org.fake.cards.Main"), rank = 0),
        Row(6, 4, -100, 1, 0, 1, sx = 4, sy = 2, provider = "org.fake.clock/org.fake.clock.Widget"),
        Row(7, 0, -101, 0, 0, 0, intent = intentOf("org.fake.phone", "org.fake.phone.Dialer")),
        Row(8, 0, -101, 1, 1, 0, intent = intentOf("org.fake.cam", "org.fake.cam.Cam")),
        Row(9, 6, -100, 1, 3, 3, intent = "#Intent;package=org.fake.chat;S.shortcut_id=abc;end"),
        Row(10, 1, -100, 1, 2, 3, intent = "#Intent;action=android.intent.action.VIEW;package=org.fake.web;end"),
        Row(11, 0, -102, 0, 0, 0, intent = intentOf("org.fake.predicted", "org.fake.predicted.Main")), // predicción: se ignora
        Row(12, 99, -100, 0, 3, 3), // tipo desconocido
    )
    private val device = DeviceState(
        mapOf("org.fake.mail" to listOf("org.fake.mail.Main"), "org.fake.maps" to listOf("org.fake.maps.Main"), "org.fake.chess" to listOf("org.fake.chess.Main"),
            "org.fake.phone" to listOf("org.fake.phone.Dialer"), "org.fake.cam" to listOf("org.fake.cam.Cam")),
        setOf("org.fake.clock/org.fake.clock.Widget"), Grid(5, 6), 5, false)

    // ---- parser ----------------------------------------------------------------------------------------------
    @Test fun parsesModernSchemaWithoutWorkspaceScreens() {
        val (snap, stats) = parse(makeDb(phoneRows))
        assertEquals(2, snap.pages.size)
        assertEquals(listOf(0, 1), snap.hotseat.map { it.first })
        val p0 = snap.pages[0].items
        assertEquals(Item.App("org.fake.mail", "org.fake.mail.Main", "personal", Cell(0, 0)), p0[0])
        val folder = p0.filterIsInstance<Item.Folder>().single()
        assertEquals("Juegos", folder.title)
        assertEquals(listOf("org.fake.cards", "org.fake.chess"), folder.items.map { it.pkg }) // por rank
        assertEquals(Item.Widget("org.fake.clock/org.fake.clock.Widget", Span(4, 2), Cell(0, 1)), snap.pages[1].items.first())
        assertEquals(2, snap.pages[1].items.count { it is Item.Shortcut })
        assertEquals(1, stats.unsupported) // tipo 99
        assertEquals(0, stats.invalid)
        assertEquals(Grid(4, 4), stats.inferredGrid)
    }

    @Test fun workspaceScreensRankDefinesPageOrder() {
        val rows = listOf(
            Row(1, 0, -100, 10, 0, 0, intent = intentOf("org.fake.a", "org.fake.a.M")),
            Row(2, 0, -100, 20, 0, 0, intent = intentOf("org.fake.b", "org.fake.b.M")))
        val (snap, _) = parse(makeDb(rows, screens = listOf(10 to 5, 20 to 1)))
        assertEquals(listOf("org.fake.b", "org.fake.a"), snap.pages.map { (it.items.single() as Item.App).pkg })
    }

    @Test fun legacyShortcutWithLauncherIntentIsAnApp() {
        val rows = listOf(Row(1, 1, -100, 0, 0, 0, intent = intentOf("org.fake.old", "org.fake.old.Main")))
        val (snap, _) = parse(makeDb(rows))
        assertTrue(snap.pages.single().items.single() is Item.App)
    }

    @Test fun invalidRowsAreCountedNotTrusted() {
        val rows = listOf(
            Row(1, 0, -100, 0, 500, 0, intent = intentOf("org.fake.a", "org.fake.a.M")),          // celda fuera de límites
            Row(2, 0, -100, 0, 0, 0, intent = "basura sin formato"),                              // intent ilegible
            Row(3, 0, -100, 0, 1, 0, intent = "#Intent;component=../../etc/passwd;end"),          // componente inválido
            Row(4, 4, -100, 0, 2, 0, sx = 999, sy = 1, provider = "a.b/a.b.W"),                   // span absurdo
            Row(5, 0, 77, 0, 0, 0, intent = intentOf("org.fake.o", "org.fake.o.M")),              // hijo de carpeta inexistente
            Row(6, 0, -100, 0, 3, 0, intent = intentOf("org.fake.ok", "org.fake.ok.M")))
        val (snap, stats) = parse(makeDb(rows))
        assertEquals(5, stats.invalid)
        assertEquals(1, snap.pages.single().items.size)
    }

    @Test fun rowLimitTruncates() {
        val rows = (1..50).map { Row(it, 0, -100, 0, it % 8, it / 8, intent = intentOf("org.fake.p$it", "org.fake.p$it.M")) }
        val (snap, stats) = parse(makeDb(rows), ImportLimits(maxRows = 10))
        assertTrue(stats.truncated)
        assertEquals(10, snap.pages.sumOf { it.items.size })
    }

    @Test fun pageAndFolderLimits() {
        val rows = (0 until 10).map { Row(it + 1, 0, -100, it, 0, 0, intent = intentOf("org.fake.p$it", "org.fake.p$it.M")) } +
            listOf(Row(100, 2, -100, 0, 1, 0, title = "x".repeat(500))) +
            (1..10).map { Row(100 + it, 0, 100, 0, 0, 0, intent = intentOf("org.fake.f$it", "org.fake.f$it.M"), rank = it) }
        val (snap, stats) = parse(makeDb(rows), ImportLimits(maxPages = 3, maxFolderItems = 4, maxTitle = 20))
        assertEquals(3, snap.pages.size)
        assertTrue(stats.truncated)
        val f = snap.pages[0].items.filterIsInstance<Item.Folder>().single()
        assertEquals(4, f.items.size)
        assertEquals(20, f.title.length)
    }

    @Test fun missingFavoritesTableFails() {
        val f = File(tmp.root, "otro.db")
        DriverManager.getConnection("jdbc:sqlite:${f.path}").use { it.createStatement().use { s -> s.execute("CREATE TABLE notes (id INTEGER)") } }
        try { parse(f); fail() } catch (e: ForeignLayoutException) { /* esperado */ }
    }

    @Test fun workProfileRowsAreMarkedWork() {
        val rows = listOf(Row(1, 0, -100, 0, 0, 0, intent = intentOf("org.fake.w", "org.fake.w.M"), profile = 10))
        assertEquals("work", (parse(makeDb(rows)).first.pages.single().items.single() as Item.App).profile)
    }

    // ---- intent ----------------------------------------------------------------------------------------------
    @Test fun intentUriParsing() {
        val p = IntentUri.parse(intentOf("com.x.y", "com.x.y.Main\$Inner"))!!
        assertEquals("com.x.y" to "com.x.y.Main\$Inner", p.component)
        assertEquals("android.intent.action.MAIN", p.action)
        assertEquals("com.x.y" to "com.x.y.Cls", IntentUri.parse("#Intent;component=com.x.y/.Cls;end")!!.component)
        assertNull(IntentUri.parse("component=a/b"))
        assertNull(IntentUri.splitComponent("a b/c"))
        assertEquals("a.b/a.b.C", IntentUri.normalizeComponent("a.b/.C"))
        assertEquals("val;ue", IntentUri.parse("#Intent;S.k=val%3Bue;end")!!.extras["k"])
    }

    // ---- catálogo --------------------------------------------------------------------------------------------
    @Test fun shippedCatalogParsesAndDetects() {
        val cat = ImportSourceCatalog.parse(File("src/main/assets/launcher-import-sources.json").readText())
        assertTrue(cat.all { s -> s.routes.all { !it.verified } }) // nada está verificado en un dispositivo real todavía
        assertTrue(cat.filter { it.closed }.all { it.routes.isEmpty() })
        val found = ImportSourceCatalog.detect(cat, listOf("com.teslacoilsw.launcher", "com.miui.home", "org.rare.home", "com.qtekfun.ultimatelauncher"), "com.qtekfun.ultimatelauncher")
        assertEquals(setOf("nova", "miui", ImportSourceCatalog.GENERIC_ID), found.map { it.source.id }.toSet())
        assertEquals(3, found.size)
        assertEquals("org.rare.home.settings", cat.first { it.id == ImportSourceCatalog.GENERIC_ID }.routes.single().authorityFor("org.rare.home"))
        assertEquals(listOf(".lawnchairbackup", ".novabackup"), ImportSourceCatalog.backupExtensions(cat).sorted())
    }

    // ---- copias de seguridad ---------------------------------------------------------------------------------
    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bo = ByteArrayOutputStream()
        ZipOutputStream(bo).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return bo.toByteArray()
    }

    @Test fun extractsRawDbAndZippedDb() {
        val db = makeDb(phoneRows).readBytes()
        val raw = BackupExtractor.extractDbs(ByteArrayInputStream(db), File(tmp.root, "o1"))
        assertEquals(1, raw.size)
        val zipped = BackupExtractor.extractDbs(ByteArrayInputStream(zipOf("preferences.xml" to "<a/>".toByteArray(), "z/other.db" to db, "launcher.db" to db, "wallpaper.png" to ByteArray(500))), File(tmp.root, "o2"))
        assertEquals("launcher.db", zipped.first().entryName) // launcher.db primero
        assertEquals(2, zipped.size)
        assertEquals(2, parse(zipped.first().file).first.pages.size)
    }

    @Test fun rejectsBombsAndForeignFiles() {
        val big = zipOf("launcher.db" to ("SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1) + ByteArray(5000)))
        try { BackupExtractor.extractDbs(ByteArrayInputStream(big), File(tmp.root, "b1"), ImportLimits(maxFileBytes = 1000)); fail() } catch (e: BackupTooLargeException) { }
        assertFalse(File(tmp.root, "b1/cand0.db").exists()) // lo escrito a medias se borra
        try { BackupExtractor.extractDbs(ByteArrayInputStream(zipOf("a.txt" to ByteArray(10))), File(tmp.root, "b2")); fail() } catch (e: NoLayoutInBackupException) { }
        try { BackupExtractor.extractDbs(ByteArrayInputStream(ByteArray(50) { 7 }), File(tmp.root, "b3")); fail() } catch (e: NoLayoutInBackupException) { }
        val many = zipOf(*(1..30).map { "f$it.bin" to ByteArray(3) }.toTypedArray())
        try { BackupExtractor.extractDbs(ByteArrayInputStream(many), File(tmp.root, "b4"), ImportLimits(maxZipEntries = 10)); fail() } catch (e: java.io.IOException) { }
    }

    @Test fun sniffsKinds() {
        assertEquals(BackupKind.TEXT, BackupExtractor.open(ByteArrayInputStream("{\"schema\":1}".toByteArray())).first)
        assertEquals(BackupKind.ZIP, BackupExtractor.open(ByteArrayInputStream(zipOf("a" to ByteArray(1)))).first)
        assertEquals(BackupKind.UNKNOWN, BackupExtractor.open(ByteArrayInputStream(ByteArray(0))).first)
    }

    // ---- extremo a extremo: parser -> planificador -> vista previa --------------------------------------------
    @Test fun endToEndPlanAndPreview() {
        val (snap, stats) = parse(makeDb(phoneRows))
        val plan = ImportPlanner.plan(snap, device)
        val c = ImportPreview.counts(plan, stats)
        assertEquals(2, c.apps)           // mail + maps (chess está dentro de la carpeta)
        assertEquals(1, c.folders)
        assertEquals(1, c.folderApps)     // solo chess está instalado
        assertEquals(1, c.widgets)
        assertEquals(2, c.dock)
        assertEquals(1, plan.pendingWidgets.size)
        // omitidos: carpeta (cards no instalada) + 2 accesos directos + tipo desconocido
        assertEquals(plan.omitted.size + 1, c.omitted)
        assertTrue(plan.omitted.any { it.reason.contains("accesos directos") })
        assertNotNull(plan.pages.flatten().filterIsInstance<Item.Widget>().singleOrNull())
    }

    @Test fun overlappingCellsForceReflow() {
        val a = Item.App("a", "a.M", "personal", Cell(0, 0)); val b = Item.App("b", "b.M", "personal", Cell(0, 0))
        assertFalse(GridReflow.fits(listOf(Page(0, listOf(a, b))), Grid(4, 4)))
        val r = GridReflow.reflow(listOf(Page(0, listOf(a, b))), Grid(4, 4))
        assertTrue(r.reflowed)
        assertEquals(setOf(Cell(0, 0), Cell(1, 0)), r.pages.single().map { it.cell }.toSet())
        assertTrue(GridReflow.fits(listOf(Page(0, listOf(a, b.at(Cell(1, 0))))), Grid(4, 4)))
    }

    @Test fun smallerDestinationGridReflows() {
        val rows = (0 until 12).map { Row(it + 1, 0, -100, 0, it % 4, it / 4, intent = intentOf("org.fake.p$it", "org.fake.p$it.M")) }
        val (snap, stats) = parse(makeDb(rows))
        val dev = DeviceState(rows.associate { "org.fake.p${it.id - 1}" to listOf("org.fake.p${it.id - 1}.M") }, emptySet(), Grid(3, 3), 5, false)
        val plan = ImportPlanner.plan(snap, dev)
        assertEquals(2, plan.pages.size) // 12 iconos en 3x3 = 9 + 3
        assertEquals(0, ImportPreview.counts(plan, stats).omitted)
    }
}
