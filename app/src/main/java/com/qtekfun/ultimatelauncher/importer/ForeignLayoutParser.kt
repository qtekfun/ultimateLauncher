// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.importer

import com.qtekfun.ultimatelauncher.layoutsync.Cell
import com.qtekfun.ultimatelauncher.layoutsync.Device
import com.qtekfun.ultimatelauncher.layoutsync.Grid
import com.qtekfun.ultimatelauncher.layoutsync.Item
import com.qtekfun.ultimatelauncher.layoutsync.Page
import com.qtekfun.ultimatelauncher.layoutsync.Settings
import com.qtekfun.ultimatelauncher.layoutsync.Snapshot
import com.qtekfun.ultimatelauncher.layoutsync.Span

/** Acceso de solo lectura a las tablas de un launcher de origen (archivo SQLite o proveedor de contenido). Sin Android aquí. */
interface TableReader {
    /** Nombres de columna de [table]; vacío si no existe. */
    fun columns(table: String): Set<String>
    /** Hasta [maxRows] filas con las columnas pedidas (las inexistentes ya vienen filtradas). Valores: Long, Double, String o null. */
    fun rows(table: String, wanted: List<String>, maxRows: Int): List<Map<String, Any?>>
}

/** Límites contra archivos corruptos o hostiles. Todo lo que los supere se descarta y se cuenta, nunca se asume. */
data class ImportLimits(
    val maxFileBytes: Long = 64L shl 20,
    val maxRows: Int = 20_000,
    val maxPages: Int = 40,
    val maxFolderItems: Int = 100,
    val maxTitle: Int = 64,
    val maxIntentLen: Int = 4096,
    val maxCell: Int = 64,
    val maxSpan: Int = 16,
    val maxHotseatSlots: Int = 16,
    val maxZipEntries: Int = 500,
)

/**
 * Lo que se perdió ANTES de llegar al planificador (el planificador cuenta aparte lo que omite por app no instalada,
 * accesos directos, etc.). `invalid` = fila corrupta o fuera de límites; `unsupported` = tipo de elemento sin equivalente.
 */
data class ParseStats(val rowsRead: Int, val invalid: Int, val unsupported: Int, val truncated: Boolean, val inferredGrid: Grid)

class ForeignLayoutException(message: String) : Exception(message)

/**
 * Convierte la tabla `favorites` (+ `workspaceScreens` si existe) de un launcher derivado de Launcher3 en un [Snapshot].
 * Esquema de AOSP (LauncherSettings.Favorites): itemType 0 app, 1 acceso directo, 2 carpeta, 4 widget, 6 acceso profundo;
 * container -100 escritorio, -101 dock, >0 carpeta. No se interpreta nada más (predicciones, cajón, QSB…).
 */
object ForeignLayoutParser {
    private const val DESKTOP = -100L
    private const val HOTSEAT = -101L
    private val WANTED = listOf("_id", "title", "intent", "container", "screen", "cellX", "cellY", "spanX", "spanY", "itemType", "appWidgetProvider", "profileId", "rank")

    private class Raw(val id: Long, val type: Int, val container: Long, val screen: Long, val cx: Int, val cy: Int, val sx: Int, val sy: Int,
                      val intent: String?, val title: String, val provider: String?, val profile: Long, val rank: Int)

    private fun num(v: Any?): Long? = when (v) { is Long -> v; is Int -> v.toLong(); is Double -> v.toLong(); is String -> v.toLongOrNull(); else -> null }

    fun parse(reader: TableReader, limits: ImportLimits = ImportLimits(), createdAt: String = ""): Pair<Snapshot, ParseStats> {
        val cols = reader.columns("favorites")
        if (cols.isEmpty() || "itemType" !in cols || "container" !in cols) throw ForeignLayoutException("sin tabla «favorites» reconocible")
        val rows = reader.rows("favorites", WANTED.filter { it in cols }, limits.maxRows + 1)
        var truncated = rows.size > limits.maxRows
        var invalid = 0
        var unsupported = 0

        val screenRank: Map<Long, Long> = reader.columns("workspaceScreens").let { c ->
            if ("_id" in c && "screenRank" in c) reader.rows("workspaceScreens", listOf("_id", "screenRank"), 1000)
                .mapNotNull { r -> val i = num(r["_id"]); val k = num(r["screenRank"]); if (i != null && k != null) i to k else null }.toMap()
            else emptyMap()
        }

        val raws = ArrayList<Raw>(minOf(rows.size, limits.maxRows))
        for ((n, r) in rows.withIndex()) {
            if (n >= limits.maxRows) break
            val type = num(r["itemType"])?.toInt(); val container = num(r["container"])
            if (type == null || container == null) { invalid++; continue }
            val cx = num(r["cellX"])?.toInt() ?: 0; val cy = num(r["cellY"])?.toInt() ?: 0
            val sx = num(r["spanX"])?.toInt() ?: 1; val sy = num(r["spanY"])?.toInt() ?: 1
            val intent = (r["intent"] as? String)?.takeIf { it.length <= limits.maxIntentLen }
            raws += Raw(num(r["_id"]) ?: -(n + 1L), type, container, num(r["screen"]) ?: 0L, cx, cy, sx, sy, intent,
                (r["title"] as? String).orEmpty().take(limits.maxTitle), (r["appWidgetProvider"] as? String)?.takeIf { it.length <= limits.maxIntentLen },
                num(r["profileId"]) ?: 0L, num(r["rank"])?.toInt() ?: 0)
        }

        fun cellOk(x: Int, y: Int) = x in 0 until limits.maxCell && y in 0 until limits.maxCell
        fun profile(r: Raw) = if (r.profile == 0L) "personal" else "work"

        /** App: itemType 0, o acceso directo antiguo (1) cuyo intent es MAIN/LAUNCHER con componente (así guardaban apps los launchers viejos). */
        fun asApp(r: Raw, cell: Cell?): Item.App? {
            val p = IntentUri.parse(r.intent ?: return null) ?: return null
            if (r.type == 1 && !(p.action == "android.intent.action.MAIN" && "android.intent.category.LAUNCHER" in p.categories)) return null
            val cn = p.component ?: return null
            return Item.App(cn.first, cn.second, profile(r), cell)
        }

        val children = raws.filter { it.container > 0 }.groupBy { it.container }
        val folderIds = raws.filter { it.type == 2 }.map { it.id }.toSet()
        // Hijos de carpetas inexistentes: filas huérfanas, corrupción.
        invalid += children.filterKeys { it !in folderIds }.values.sumOf { it.size }

        fun build(r: Raw): Item? {
            val cell = Cell(r.cx, r.cy)
            when (r.type) {
                0, 1 -> {
                    val app = asApp(r, cell)
                    if (app != null) return app
                    if (r.type == 0) { invalid++; return null }
                    val p = r.intent?.let { IntentUri.parse(it) }
                    return Item.Shortcut(p?.pkg ?: p?.component?.first ?: "?", p?.extras?.get("shortcut_id").orEmpty(), cell)
                }
                6 -> { val p = r.intent?.let { IntentUri.parse(it) }; return Item.Shortcut(p?.pkg ?: p?.component?.first ?: "?", p?.extras?.get("shortcut_id").orEmpty(), cell) }
                2 -> {
                    val kids = children[r.id].orEmpty().sortedWith(compareBy({ it.rank }, { it.cy }, { it.cx })).take(limits.maxFolderItems)
                    val apps = kids.mapNotNull { k -> asApp(k, null).also { if (it == null) unsupported++ } }
                    return Item.Folder(r.title, apps, cell)
                }
                4 -> {
                    val prov = r.provider?.let { IntentUri.normalizeComponent(it) }
                    if (prov == null || r.sx !in 1..limits.maxSpan || r.sy !in 1..limits.maxSpan) { invalid++; return null }
                    return Item.Widget(prov, Span(r.sx, r.sy), cell)
                }
                else -> { unsupported++; return null }
            }
        }

        val desktop = raws.filter { it.container == DESKTOP }
        val hotseat = raws.filter { it.container == HOTSEAT }
        // Contenedores con ids negativos distintos (predicciones, cajón, bandejas…) no son datos del usuario: se ignoran sin contar.
        val pageKeys = desktop.map { screenRank[it.screen] ?: it.screen }.distinct().sorted()
        if (pageKeys.size > limits.maxPages) truncated = true
        val keptKeys = pageKeys.take(limits.maxPages)

        var maxX = 1; var maxY = 1
        val pages = keptKeys.mapIndexed { idx, key ->
            val items = desktop.filter { (screenRank[it.screen] ?: it.screen) == key }.mapNotNull { r ->
                if (!cellOk(r.cx, r.cy)) { invalid++; return@mapNotNull null }
                build(r)?.also { maxX = maxOf(maxX, r.cx + (if (r.type == 4) r.sx else 1)); maxY = maxOf(maxY, r.cy + (if (r.type == 4) r.sy else 1)) }
            }
            Page(idx, items)
        }
        val hot = hotseat.sortedWith(compareBy({ it.screen }, { it.cx })).mapNotNull { r ->
            val slot = (if ("screen" in cols) r.screen else r.cx.toLong()).toInt()
            if (slot !in 0 until limits.maxHotseatSlots) { invalid++; return@mapNotNull null }
            build(r)?.let { slot to it.at(null) }
        }.distinctBy { it.first }

        val grid = Grid(maxX, maxY)
        val snap = Snapshot(createdAt, Device("", "", "", 0, 0, 0, 0), Settings(grid, "phone"), hot, pages)
        return snap to ParseStats(rows.size.coerceAtMost(limits.maxRows), invalid, unsupported, truncated, grid)
    }
}

/** Lectura mínima de URIs de intent de Android (`#Intent;…;end`, como las guarda Launcher3), sin depender del framework. */
object IntentUri {
    data class Parsed(val action: String?, val categories: List<String>, val component: Pair<String, String>?, val pkg: String?, val extras: Map<String, String>)

    private val PKG = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")
    private val CLS = Regex("[A-Za-z0-9_.$]+")

    private fun decode(s: String): String {
        if ('%' !in s) return s
        val out = java.io.ByteArrayOutputStream(); var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val h = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (h != null) { out.write(h); i += 3; continue }
            }
            out.write(c.toString().toByteArray(Charsets.UTF_8)); i++
        }
        return out.toString("UTF-8")
    }

    /** `pkg/cls` o `pkg/.cls` → (paquete, clase completa); null si no es válido. */
    fun splitComponent(flat: String): Pair<String, String>? {
        val i = flat.indexOf('/'); if (i <= 0 || i == flat.length - 1 || flat.length > 512) return null
        val pkg = flat.substring(0, i); var cls = flat.substring(i + 1)
        if (cls.startsWith(".")) cls = pkg + cls
        return if (PKG.matches(pkg) && CLS.matches(cls)) pkg to cls else null
    }

    /** Forma larga `pkg/clase` (la que usa `ComponentName.flattenToString`), o null. */
    fun normalizeComponent(flat: String): String? = splitComponent(flat)?.let { "${it.first}/${it.second}" }

    fun parse(uri: String): Parsed? {
        val at = uri.indexOf("#Intent;"); if (at < 0) return null
        val body = uri.substring(at + 8).substringBefore(";end")
        var action: String? = null; var comp: Pair<String, String>? = null; var pkg: String? = null
        val cats = mutableListOf<String>(); val extras = mutableMapOf<String, String>()
        for (part in body.split(';')) {
            val eq = part.indexOf('='); if (eq <= 0) continue
            val k = part.substring(0, eq); val v = decode(part.substring(eq + 1))
            when {
                k == "action" -> action = v
                k == "category" -> cats += v
                k == "component" -> comp = splitComponent(v)
                k == "package" -> pkg = v
                k.startsWith("S.") -> extras[k.substring(2)] = v
            }
        }
        return Parsed(action, cats, comp, pkg, extras)
    }
}
