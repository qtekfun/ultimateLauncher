package com.qtekfun.ultimatelauncher.dock

import android.content.Context
import android.content.SharedPreferences
import com.android.launcher3.LauncherFiles

/** Ajustes del dock de tablet (pantalla de ajustes del launcher, parche 0041). */
object DockPrefs {
    const val KEY_STYLE = "pref_ul_dock_style"       // pills | subtle | none
    const val KEY_RECENTS = "pref_ul_dock_recents"   // mostrar y registrar últimas apps usadas

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    fun style(context: Context): String = prefs(context).getString(KEY_STYLE, "pills") ?: "pills"
    fun recentsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RECENTS, true)

    /** Opacidad del fondo de las píldoras según el estilo (0 = sin fondo). */
    fun pillAlpha(context: Context): Int = when (style(context)) {
        "none" -> 0
        "subtle" -> 0x40
        else -> 0xB8
    }
}
