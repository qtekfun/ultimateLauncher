package com.qtekfun.ultimatelauncher.hidden

import android.os.Process
import android.view.View
import android.widget.Toast
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.views.ActivityContext

/**
 * Entrada «Ocultar» del menú contextual de una app (cajón, escritorio, dock, carpetas) — parche 0182. Sale con el mismo
 * estilo unificado que el resto de filas (el estilo lo aplica `ContextMenuStyle` a todo `ArrowPopup`). Exige un bloqueo
 * de pantalla: sin él no habría forma de proteger la lista y se avisa en lugar de ocultar.
 */
class HideAppShortcut<T : ActivityContext>(target: T, itemInfo: ItemInfo, originalView: View) :
    SystemShortcut<T>(R.drawable.ul_ic_hide, R.string.ul_hide_app, target, itemInfo, originalView) {

    override fun onClick(view: View) {
        dismissTaskMenuView()
        val ctx = mTarget.asContext()
        val component = mItemInfo.targetComponent ?: return
        if (!DeviceAuth.hasScreenLock(ctx)) {
            Toast.makeText(ctx, R.string.ul_hide_needs_lock, Toast.LENGTH_LONG).show()
            return
        }
        HiddenApps.setHidden(ctx, component, true)
        Toast.makeText(ctx, ctx.getString(R.string.ul_hide_done, mItemInfo.title ?: ""), Toast.LENGTH_LONG).show()
    }

    companion object {
        /** ¿Se puede ofrecer «Ocultar» para este elemento? Solo apps del perfil principal con componente conocido. */
        @JvmStatic fun applies(itemInfo: ItemInfo): Boolean =
            itemInfo.itemType == ITEM_TYPE_APPLICATION &&
                itemInfo.targetComponent != null &&
                itemInfo.user == Process.myUserHandle()

        @JvmField
        val FACTORY: SystemShortcut.Factory<ActivityContext> =
            SystemShortcut.Factory { context, itemInfo, originalView ->
                if (applies(itemInfo)) HideAppShortcut(context, itemInfo, originalView) else null
            }
    }
}
