// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

import android.view.View
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.views.ActivityContext

/**
 * Entradas de Dual apps en el menú contextual (docs/10, «Interacción»; parche 0220), arriba del todo:
 *  - «Abrir en la otra cuenta» en cualquier icono de una app emparejada (escritorio, dock, carpetas, cajón).
 *  - «Añadir cuenta 2 al escritorio» en la copia principal, si la otra copia aún no está en el escritorio.
 * Salen con el estilo unificado de menús (parche 0150). La búsqueda del par es una consulta a `LauncherApps` por
 * perfil, igual que hace AOSP con «Instalar en el espacio privado».
 */
class OpenOtherCopyShortcut<T : ActivityContext>(
    target: T, itemInfo: ItemInfo, originalView: View, private val other: DualTarget,
) : SystemShortcut<T>(R.drawable.ul_ic_dual, R.string.ul_dual_open_other, target, itemInfo, originalView) {

    override fun onClick(view: View) {
        AbstractFloatingView.closeAllOpenViews(mTarget)
        DualApps.launch(mTarget.asContext(), other, mOriginalView)
    }

    companion object {
        @JvmField
        val FACTORY: SystemShortcut.Factory<ActivityContext> =
            SystemShortcut.Factory { context, itemInfo, originalView ->
                val other = DualApps.counterpart(context.asContext(), itemInfo)
                if (other != null && originalView != null) OpenOtherCopyShortcut(context, itemInfo, originalView, other) else null
            }
    }
}

class AddOtherCopyShortcut<T : ActivityContext>(
    target: T, itemInfo: ItemInfo, originalView: View, private val other: DualTarget,
) : SystemShortcut<T>(R.drawable.ic_plus, R.string.ul_dual_add_other, target, itemInfo, originalView) {

    override fun onClick(view: View) {
        AbstractFloatingView.closeAllOpenViews(mTarget)
        (mTarget as? com.android.launcher3.Launcher)?.let { DualApps.addToHome(it, other) }
    }

    companion object {
        @JvmField
        val FACTORY: SystemShortcut.Factory<ActivityContext> =
            SystemShortcut.Factory { context, itemInfo, originalView ->
                if (originalView == null || context !is com.android.launcher3.Launcher) return@Factory null
                val ctx = context.asContext()
                val other = DualApps.counterpart(ctx, itemInfo) ?: return@Factory null
                val onHome = DualHome.contains(context, other)
                if (DualAppsLogic.offerAddToHome(DualApps.kindOf(ctx, itemInfo.user), other.kind, onHome)) {
                    AddOtherCopyShortcut(context, itemInfo, originalView, other)
                } else null
            }
    }
}

/** ¿Está ya la otra copia en el escritorio o el dock (también dentro de carpetas)? */
internal object DualHome {
    fun contains(context: ActivityContext, target: DualTarget): Boolean {
        val launcher = context as? com.android.launcher3.Launcher ?: return false
        return launcher.workspace.mapOverItems { info, _ ->
            info != null && info.user == target.user &&
                info.targetComponent?.packageName == target.component.packageName
        } != null
    }
}
