package com.qtekfun.ultimatelauncher.layoutsync

import java.text.Collator
import java.util.Locale

/** Una app lanzable instalada (para «Colocar mis apps por orden»). */
data class AppEntry(val pkg: String, val activity: String, val label: String, val firstInstallTime: Long)

enum class AppOrder { ALPHABETICAL, INSTALL_DATE }

/**
 * «Colocar mis apps por orden» (docs/06): para launchers cerrados de los que no se puede leer nada sin root. Genera una
 * disposición nueva con las apps instaladas ordenadas y repartidas en páginas de [perPage] apps (por orden de lectura
 * sobre la rejilla de destino). Parte pura (sin Android); la lectura de apps vive en `LayoutStore.installedApps()`.
 * No importa carpetas, dock ni páginas del launcher viejo: eso no es legible. El dock actual se conserva.
 */
object AppsByOrder {
    fun sorted(apps: List<AppEntry>, order: AppOrder, locale: Locale = Locale.getDefault()): List<AppEntry> = when (order) {
        AppOrder.ALPHABETICAL -> { val c = Collator.getInstance(locale)
            apps.sortedWith(Comparator<AppEntry> { a, b -> c.compare(a.label, b.label) }.thenBy { it.pkg }) }
        // Las más antiguas primero (orden en que las fue instalando); a igualdad, alfabético por paquete para ser estable.
        AppOrder.INSTALL_DATE -> apps.sortedWith(compareBy<AppEntry>({ it.firstInstallTime }, { it.pkg }, { it.activity }))
    }

    /** Máximo de apps por página que admite la rejilla. */
    fun capacity(grid: Grid) = (grid.columns * grid.rows).coerceAtLeast(1)

    /**
     * Páginas con [perPage] apps cada una (acotado a [1, capacity]); el último hueco de cada página queda libre.
     * Las apps ya presentes en el dock actual ([dock]) no se repiten en las páginas.
     */
    fun pages(apps: List<AppEntry>, order: AppOrder, perPage: Int, grid: Grid, dock: List<Pair<Int, Item>> = emptyList(),
              locale: Locale = Locale.getDefault()): List<Page> {
        val n = perPage.coerceIn(1, capacity(grid))
        val inDock = dock.flatMap { (_, it) -> when (it) { is Item.App -> listOf(it.pkg to it.activity)
            is Item.Folder -> it.items.map { a -> a.pkg to a.activity }; else -> emptyList() } }.toSet()
        val list = sorted(apps.filter { (it.pkg to it.activity) !in inDock }, order, locale)
        return list.chunked(n).mapIndexed { idx, chunk ->
            Page(idx, chunk.mapIndexed { k, a -> Item.App(a.pkg, a.activity, "personal", Cell(k % grid.columns, k / grid.columns)) })
        }
    }

    /** Instantánea sintética para pasarla por [ImportPlanner] (así se reutilizan comprobaciones de instalado y rejilla). */
    fun snapshot(apps: List<AppEntry>, order: AppOrder, perPage: Int, grid: Grid, dock: List<Pair<Int, Item>>, device: Device): Snapshot =
        Snapshot("", device, Settings(grid), dock, pages(apps, order, perPage, grid, dock))
}
