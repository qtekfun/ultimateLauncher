// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.dock

import android.content.Context
import android.content.SharedPreferences
import com.android.launcher3.LauncherFiles

/** Ajustes del dock de tablet (pantalla de ajustes del launcher, parche 0041). */
object DockPrefs {
    const val KEY_BACKGROUND = "pref_ul_dock_background" // true: píldoras; false: solo iconos
    const val KEY_SUBTLE = "pref_ul_dock_subtle"         // true: fondo más translúcido
    const val KEY_RECENTS = "pref_ul_dock_recents"   // mostrar y registrar últimas apps usadas

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    fun recentsEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RECENTS, true)

    /** Opacidad del fondo de las píldoras según el estilo (0 = sin fondo). */
    fun pillAlpha(context: Context): Int {
        val p = prefs(context)
        return when {
            !p.getBoolean(KEY_BACKGROUND, true) -> 0
            p.getBoolean(KEY_SUBTLE, false) -> 0x40
            else -> 0xB8
        }
    }
}
