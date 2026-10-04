package com.qtekfun.ultimatelauncher.layoutsync

import org.json.JSONArray
import org.json.JSONObject

/** Disposición de la pantalla de inicio en el esquema v1 de docs/06. Sin dependencias de Android salvo org.json. */
const val SCHEMA_VERSION = 1

data class Cell(val x: Int, val y: Int)
data class Span(val w: Int, val h: Int)
data class Grid(val columns: Int, val rows: Int)

sealed class Item {
    abstract val cell: Cell?
    /** Etiqueta legible para informes (nunca un dato fuera del propio layout). */
    abstract val label: String

    data class App(val pkg: String, val activity: String, val profile: String = "personal", override val cell: Cell? = null) : Item() {
        override val label get() = pkg
    }
    data class Folder(val title: String, val items: List<App>, override val cell: Cell? = null) : Item() {
        override val label get() = "carpeta «$title»"
    }
    data class Widget(val provider: String, val span: Span, override val cell: Cell? = null) : Item() {
        override val label get() = "widget $provider"
    }
    data class Shortcut(val pkg: String, val id: String, override val cell: Cell? = null) : Item() {
        override val label get() = "acceso $pkg/$id"
    }
    fun at(c: Cell?): Item = when (this) {
        is App -> copy(cell = c); is Folder -> copy(cell = c); is Widget -> copy(cell = c); is Shortcut -> copy(cell = c)
    }
}

data class Device(val cls: String, val brand: String, val model: String, val androidApi: Int, val density: Int, val widthDp: Int, val heightDp: Int)
data class Settings(val grid: Grid, val gridKey: String = "phone", val iconPack: String? = null, val iconShape: String = "",
                    val animationProfile: String = "aosp-por-defecto", val speedMultiplier: Double = 1.0, val theme: String = "oppo-medido")
data class Page(val index: Int, val items: List<Item>)
data class Snapshot(val createdAt: String, val device: Device, val settings: Settings, val hotseat: List<Pair<Int, Item>>, val pages: List<Page>)

class UnsupportedSchemaException(val found: Int) : Exception("Esquema $found mayor que el soportado ($SCHEMA_VERSION)")

object LayoutJson {
    fun toJson(s: Snapshot): String = JSONObject().apply {
        put("schema", SCHEMA_VERSION); put("createdAt", s.createdAt)
        put("device", JSONObject().put("class", s.device.cls).put("brand", s.device.brand).put("model", s.device.model)
            .put("androidApi", s.device.androidApi).put("density", s.device.density).put("widthDp", s.device.widthDp).put("heightDp", s.device.heightDp))
        put("settings", JSONObject().apply {
            put("grids", JSONObject().put(s.settings.gridKey, JSONObject().put("columns", s.settings.grid.columns).put("rows", s.settings.grid.rows)))
            put("iconPack", s.settings.iconPack ?: JSONObject.NULL); put("iconShape", s.settings.iconShape)
            put("animationProfile", s.settings.animationProfile); put("speedMultiplier", s.settings.speedMultiplier); put("theme", s.settings.theme)
        })
        put("hotseat", JSONArray().also { a -> s.hotseat.forEach { (slot, it) -> a.put(JSONObject().put("slot", slot).put("item", item(it))) } })
        put("pages", JSONArray().also { a -> s.pages.forEach { p -> a.put(JSONObject().put("index", p.index).put("items", JSONArray().also { ia -> p.items.forEach { ia.put(item(it)) } })) } })
    }.toString(2)

    private fun cellJson(c: Cell?) = c?.let { JSONObject().put("x", it.x).put("y", it.y) }
    private fun item(i: Item): JSONObject = when (i) {
        is Item.App -> JSONObject().put("type", "app").put("package", i.pkg).put("activity", i.activity).put("profile", i.profile).also { o -> cellJson(i.cell)?.let { o.put("cell", it) } }
        is Item.Folder -> JSONObject().put("type", "folder").put("title", i.title).also { o ->
            cellJson(i.cell)?.let { o.put("cell", it) }; o.put("items", JSONArray().also { a -> i.items.forEach { a.put(item(it)) } }) }
        is Item.Widget -> JSONObject().put("type", "widget").put("provider", i.provider).put("span", JSONObject().put("w", i.span.w).put("h", i.span.h)).also { o -> cellJson(i.cell)?.let { o.put("cell", it) } }
        is Item.Shortcut -> JSONObject().put("type", "shortcut").put("package", i.pkg).put("id", i.id).also { o -> cellJson(i.cell)?.let { o.put("cell", it) } }
    }

    fun fromJson(text: String): Snapshot {
        val o = JSONObject(text)
        val schema = o.optInt("schema", 0)
        if (schema > SCHEMA_VERSION) throw UnsupportedSchemaException(schema)
        val d = o.getJSONObject("device")
        val st = o.getJSONObject("settings")
        val gridsObj = st.getJSONObject("grids")
        val key = gridsObj.keys().asSequence().first()
        val g = gridsObj.getJSONObject(key)
        val settings = Settings(Grid(g.getInt("columns"), g.getInt("rows")), key, st.optString("iconPack").takeIf { it.isNotEmpty() && it != "null" },
            st.optString("iconShape"), st.optString("animationProfile", "aosp-por-defecto"), st.optDouble("speedMultiplier", 1.0), st.optString("theme", "oppo-medido"))
        val hot = o.getJSONArray("hotseat").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { h -> h.getInt("slot") to parse(h.getJSONObject("item")) } } }
        val pages = o.getJSONArray("pages").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { p ->
            Page(p.getInt("index"), p.getJSONArray("items").let { ia -> (0 until ia.length()).map { j -> parse(ia.getJSONObject(j)) } }) } } }
        return Snapshot(o.optString("createdAt"), Device(d.optString("class"), d.optString("brand"), d.optString("model"), d.optInt("androidApi"),
            d.optInt("density"), d.optInt("widthDp"), d.optInt("heightDp")), settings, hot, pages)
    }

    private fun cell(o: JSONObject) = o.optJSONObject("cell")?.let { Cell(it.getInt("x"), it.getInt("y")) }
    private fun parse(o: JSONObject): Item = when (o.getString("type")) {
        "app" -> Item.App(o.getString("package"), o.optString("activity"), o.optString("profile", "personal"), cell(o))
        "folder" -> Item.Folder(o.optString("title"), o.getJSONArray("items").let { a -> (0 until a.length()).map { parse(a.getJSONObject(it)) as Item.App } }, cell(o))
        "widget" -> Item.Widget(o.getString("provider"), o.getJSONObject("span").let { Span(it.getInt("w"), it.getInt("h")) }, cell(o))
        "shortcut" -> Item.Shortcut(o.getString("package"), o.optString("id"), cell(o))
        else -> Item.Shortcut("?", o.getString("type"), cell(o)) // tipo desconocido: se tratará como no importable
    }
}
