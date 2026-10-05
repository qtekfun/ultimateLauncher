package com.qtekfun.ultimatelauncher.iconpack

import android.content.Context
import androidx.preference.Preference
import com.android.launcher3.R
import com.qtekfun.ultimatelauncher.ui.ContextMenuStyle

/** Enlace entre la pantalla de ajustes de inicio y [IconPackManager] (parche 0173). */
object IconPackSettings {
    /** `pref_ul_icon_pack`: muestra el pack actual como resumen y abre el selector (diálogo con el estilo unificado). */
    @JvmStatic
    fun initPicker(preference: Preference, context: Context): Boolean {
        val mgr = IconPackManager.get(context)
        fun refreshSummary() {
            val sel = mgr.selectedPackage()
            preference.summary = if (sel == null) context.getString(R.string.ul_icon_pack_default)
            else mgr.installedPacks().firstOrNull { it.packageName == sel }?.label ?: sel
        }
        refreshSummary()
        preference.setOnPreferenceClickListener {
            val packs = mgr.installedPacks()
            val current = mgr.selectedPackage()
            val labels = ArrayList<CharSequence>().apply {
                add(context.getString(R.string.ul_icon_pack_default))
                packs.forEach { add(it.label) }
            }
            val selected = if (current == null) 0 else 1 + packs.indexOfFirst { it.packageName == current }.coerceAtLeast(-1)
            val title = context.getString(R.string.ul_pref_icon_pack)
            if (packs.isEmpty()) {
                // Sin packs instalados: se explica en el propio diálogo (única fila «Predeterminado» + aviso en el resumen).
                preference.summary = context.getString(R.string.ul_icon_pack_none)
            }
            ContextMenuStyle.showChoiceDialog(context, title, labels, selected) { idx ->
                mgr.setSelected(if (idx == 0) null else packs[idx - 1].packageName)
                refreshSummary()
            }
            true
        }
        return true
    }

    /** `pref_ul_icon_pack_back`: al cambiar el interruptor se recargan los iconos. */
    @JvmStatic
    fun initBackSwitch(preference: Preference, context: Context): Boolean {
        preference.setOnPreferenceChangeListener { _, _ ->
            // El valor se guarda DESPUÉS de este callback: se avisa en el siguiente ciclo del hilo principal.
            android.os.Handler(android.os.Looper.getMainLooper()).post { IconPackManager.get(context).notifyChanged() }
            true
        }
        return true
    }
}
