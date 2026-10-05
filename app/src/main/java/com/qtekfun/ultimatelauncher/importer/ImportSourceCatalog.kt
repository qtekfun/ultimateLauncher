package com.qtekfun.ultimatelauncher.importer

import org.json.JSONObject

/** Una vía de importación de un launcher de origen (assets/launcher-import-sources.json). */
data class ImportRoute(val type: String, val authority: String?, val extensions: List<String>, val verified: Boolean, val noteEs: String, val noteEn: String) {
    val isProvider get() = type == "provider"
    val isBackup get() = type == "backup"
    fun note(lang: String) = if (lang == "es") noteEs else noteEn
    /** Sustituye `{package}` por el paquete real del launcher instalado. */
    fun authorityFor(pkg: String) = authority?.replace("{package}", pkg)
}

data class ImportSource(val id: String, val label: String, val packages: List<String>, val closed: Boolean, val routes: List<ImportRoute>)

/** Un launcher instalado en este dispositivo y la fuente del catálogo que le corresponde. */
data class DetectedLauncher(val source: ImportSource, val pkg: String)

/**
 * Catálogo de launchers de origen. Datos, no código (como oem-intents.json): las vías no verificadas se marcan
 * `verified=false` y el asistente lo dice. Parte pura (sin Android) para poder probarla.
 */
object ImportSourceCatalog {
    const val GENERIC_ID = "generic-launcher3"

    fun parse(json: String): List<ImportSource> {
        val arr = JSONObject(json).getJSONArray("sources")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pk = o.getJSONArray("packages").let { a -> (0 until a.length()).map { a.getString(it) } }
            val rs = o.getJSONArray("routes").let { a -> (0 until a.length()).map { j ->
                val r = a.getJSONObject(j)
                val ext = r.optJSONArray("extensions")?.let { e -> (0 until e.length()).map { e.getString(it).lowercase() } } ?: emptyList()
                val note = r.optJSONObject("note")
                ImportRoute(r.getString("type"), r.optString("authority").takeIf { it.isNotEmpty() }, ext, r.optBoolean("verified", false),
                    note?.optString("es").orEmpty(), note?.optString("en").orEmpty())
            } }
            ImportSource(o.getString("id"), o.getString("label"), pk, o.optBoolean("closed", false), rs)
        }
    }

    /**
     * Empareja los launchers instalados (paquetes con intent HOME, sin nosotros) con el catálogo. Los que no están listados
     * y no son del sistema cerrado reciben la fuente genérica de Launcher3 (autoridad «<paquete>.settings»).
     */
    fun detect(catalog: List<ImportSource>, homePackages: Collection<String>, ownPackage: String): List<DetectedLauncher> {
        val generic = catalog.firstOrNull { it.id == GENERIC_ID }
        return homePackages.filter { it != ownPackage && !it.startsWith("$ownPackage.") }.distinct().sorted().mapNotNull { pkg ->
            val known = catalog.firstOrNull { pkg in it.packages }
            when {
                known != null -> DetectedLauncher(known, pkg)
                generic != null -> DetectedLauncher(generic, pkg)
                else -> null
            }
        }
    }

    /** Fuentes con vía de copia de seguridad (para explicar qué archivos se admiten). */
    fun backupExtensions(catalog: List<ImportSource>): List<String> =
        catalog.flatMap { s -> s.routes.filter { it.isBackup }.flatMap { it.extensions } }.distinct()
}
