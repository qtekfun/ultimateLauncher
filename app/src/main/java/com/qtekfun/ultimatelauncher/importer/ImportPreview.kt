package com.qtekfun.ultimatelauncher.importer

import com.qtekfun.ultimatelauncher.layoutsync.ImportPlan
import com.qtekfun.ultimatelauncher.layoutsync.Item

/** Cifras de la vista previa «Se importarán N apps, M carpetas… K omitidos» (se muestran antes de aplicar). */
data class PreviewCounts(val apps: Int, val folders: Int, val folderApps: Int, val widgets: Int, val dock: Int, val pages: Int, val omitted: Int)

object ImportPreview {
    /** `apps` cuenta las apps sueltas del escritorio (las del dock y las de carpetas van aparte). */
    fun counts(plan: ImportPlan, stats: ParseStats): PreviewCounts {
        val flat = plan.pages.flatten()
        return PreviewCounts(
            apps = flat.count { it is Item.App },
            folders = flat.count { it is Item.Folder } + plan.hotseat.count { it.second is Item.Folder },
            folderApps = (flat + plan.hotseat.map { it.second }).filterIsInstance<Item.Folder>().sumOf { it.items.size },
            widgets = flat.count { it is Item.Widget },
            dock = plan.hotseat.size,
            pages = plan.pages.size,
            omitted = plan.omitted.size + stats.invalid + stats.unsupported,
        )
    }
}
