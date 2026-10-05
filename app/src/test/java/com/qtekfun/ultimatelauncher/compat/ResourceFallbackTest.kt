// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.compat

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R1 (docs/compat-android12-14.md): ningún recurso puede existir SOLO en una configuración `-vNN` con NN > 31, porque en
 * Android 12 (API 31) darían `Resources.NotFoundException` al leerse (p. ej. los `@color/system_*` de values-v34).
 * Lee los directorios `res` del repositorio; no necesita Android.
 */
class ResourceFallbackTest {
    private val repoRoot = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
    private val resDirs = listOf(
        "launcher3-base/res", "systemui-libs/dynamiccolors/res", "systemui-libs/iconloaderlib/res",
        "animprofile/res", "themetokens/res",
    )

    private fun minApi(qualifier: String): Int = Regex("(?:^|-)v(\\d+)").find(qualifier)?.groupValues?.get(1)?.toInt() ?: 0

    private fun definitions(): Map<String, Set<Int>> {
        val out = HashMap<String, MutableSet<Int>>()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        for (r in resDirs) {
            val res = File(repoRoot, r)
            res.listFiles { f -> f.isDirectory }?.forEach { dir ->
                val type = dir.name.substringBefore('-')
                val api = minApi(dir.name.substringAfter('-', ""))
                dir.listFiles()?.forEach { f ->
                    if (type == "values") {
                        val nodes = builder.parse(f).documentElement.childNodes
                        for (i in 0 until nodes.length) {
                            val e = nodes.item(i) as? org.w3c.dom.Element ?: continue
                            val name = e.getAttribute("name").ifEmpty { continue }
                            val t = if (e.tagName == "item") e.getAttribute("type") else e.tagName
                            out.getOrPut("$t/$name") { HashSet() }.add(api)
                        }
                    } else {
                        out.getOrPut("$type/${f.nameWithoutExtension}") { HashSet() }.add(api)
                    }
                }
            }
        }
        return out
    }

    @Test fun noResourceExistsOnlyAboveApi31() {
        val onlyNew = definitions().filter { (_, apis) -> apis.none { it <= 31 } }.keys.sorted()
        assertTrue("Recursos sin alternativa en API 31 (${onlyNew.size}): ${onlyNew.take(20)}", onlyNew.isEmpty())
    }

    @Test fun fallbackColorsCoverEveryV34SystemColor() {
        val defs = definitions()
        val v34 = defs.filter { (k, apis) -> k.startsWith("color/system_") && 34 in apis }.keys
        assertTrue("No se encontraron colores system_* de values-v34 (¿ruta del repositorio?)", v34.isNotEmpty())
        val missing = v34.filter { k -> defs.getValue(k).none { it <= 31 } }
        assertTrue("Colores system_* sin alternativa <34: $missing", missing.isEmpty())
    }
}
