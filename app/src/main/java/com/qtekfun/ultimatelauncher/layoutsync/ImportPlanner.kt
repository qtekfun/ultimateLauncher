package com.qtekfun.ultimatelauncher.layoutsync

/** Qué hay instalado en el dispositivo de destino. `launchable`: paquete → actividades de lanzador. */
data class DeviceState(val launchable: Map<String, List<String>>, val widgetProviders: Set<String>, val grid: Grid, val hotseatSlots: Int, val hasWorkProfile: Boolean)

data class Omission(val what: String, val reason: String)

/** Resultado de planificar una importación; el resumen se muestra antes de aplicar (docs/06 punto 6). */
data class ImportPlan(val hotseat: List<Pair<Int, Item>>, val pages: List<List<Item>>, val omitted: List<Omission>,
                      val pendingWidgets: List<Item.Widget>, val changes: List<String>) {
    fun summary(): String = buildString {
        appendLine("Se importarán: ${hotseat.size} del dock y ${pages.sumOf { it.size }} elementos en ${pages.size} página(s).")
        changes.forEach { appendLine("• $it") }
        if (pendingWidgets.isNotEmpty()) appendLine("Widgets que hay que añadir a mano (requieren permiso del sistema): ${pendingWidgets.joinToString { it.provider.substringAfterLast('.') }}")
        if (omitted.isNotEmpty()) { appendLine("No se importarán (${omitted.size}):"); omitted.forEach { appendLine("  – ${it.what}: ${it.reason}") } }
    }.trim()
}

object ImportPlanner {
    fun plan(s: Snapshot, dev: DeviceState): ImportPlan {
        val omitted = mutableListOf<Omission>()
        val pending = mutableListOf<Item.Widget>()
        val changes = mutableListOf<String>()

        fun resolveApp(a: Item.App): Item.App? {
            if (a.profile == "work" && !dev.hasWorkProfile) { omitted += Omission(a.pkg, "perfil de trabajo ausente"); return null }
            if (a.profile == "work") { omitted += Omission(a.pkg, "perfil de trabajo: no se restaura su contenido"); return null }
            val acts = dev.launchable[a.pkg]
            if (acts.isNullOrEmpty()) { omitted += Omission(a.pkg, "app no instalada"); return null }
            return if (a.activity in acts) a else a.copy(activity = acts.first()).also { changes += "${a.pkg}: actividad distinta en este dispositivo" }
        }
        fun resolve(i: Item): Item? = when (i) {
            is Item.App -> resolveApp(i)
            is Item.Folder -> { val kids = i.items.mapNotNull { resolveApp(it) }
                if (kids.isEmpty()) { omitted += Omission(i.label, "sin apps instaladas"); null } else i.copy(items = kids) }
            is Item.Widget -> { if (i.provider.substringBefore('/') !in dev.widgetProviders.map { it.substringBefore('/') }.toSet() && i.provider !in dev.widgetProviders)
                omitted += Omission(i.label, "proveedor ausente (widget de otra marca o app no instalada)") else pending += i; null }
            is Item.Shortcut -> { omitted += Omission(i.label, "los accesos directos no se restauran en v1"); null }
        }

        val pagesIn = s.pages.map { p -> Page(p.index, p.items.mapNotNull { resolve(it) } + pending.filter { false }) }
        // Los widgets se omiten del layout (se listan aparte), pero cuentan para la reubicación solo si se añadieran: no se reservan celdas.
        val hot = s.hotseat.sortedBy { it.first }.mapNotNull { (slot, it) -> resolve(it)?.let { r -> slot to r } }
        val keptHot = hot.filter { it.first < dev.hotseatSlots }
        val overflow = hot.filter { it.first >= dev.hotseatSlots }.map { it.second }
        if (overflow.isNotEmpty()) changes += "${overflow.size} elemento(s) del dock no caben (${dev.hotseatSlots} huecos): pasan a la pantalla de inicio"

        val withOverflow = if (overflow.isEmpty()) pagesIn else pagesIn + Page(pagesIn.size, overflow.map { it.at(Cell(0, 0)) })
        val r = GridReflow.reflow(withOverflow.map { p -> Page(p.index, p.items.map { if (it.cell == null) it.at(Cell(0, 0)) else it }) }, dev.grid)
        if (r.reflowed) changes += "Rejilla ${s.settings.grid.columns}×${s.settings.grid.rows} → ${dev.grid.columns}×${dev.grid.rows}: elementos reubicados por orden de lectura (${r.pages.size} página(s))"
        if (s.device.cls.isNotEmpty() && s.device.cls != (if (dev.grid.columns >= 6) "tablet" else "phone")) changes += "Cambio de clase de pantalla (${s.device.cls}): se ha adaptado a la rejilla de destino"
        return ImportPlan(keptHot, r.pages.filter { it.isNotEmpty() }, omitted, pending, changes)
    }
}
