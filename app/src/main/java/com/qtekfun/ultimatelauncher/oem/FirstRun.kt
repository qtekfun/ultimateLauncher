// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.app.Activity
import android.content.Context
import android.content.Intent

/** Lanza el asistente la primera vez que arranca el launcher (docs/05, RF-40). */
object FirstRun {
    private const val PREFS = "ul_firstrun"
    private const val KEY_DONE = "done"
    private const val KEY_NAG = "battery_nag_at"

    fun isDone(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)
    fun markDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()

    /** Gancho de una línea desde Launcher.onCreate (parche 0010). */
    @JvmStatic
    fun maybeShow(activity: Activity) {
        if (!isDone(activity)) {
            activity.startActivity(Intent(activity, FirstRunActivity::class.java))
            return
        }
        // Ya completado, pero si el sistema aún puede matar el launcher (optimización de batería activa), se recuerda como
        // máximo cada 3 días en cualquier fabricante: es la causa de los iconos en blanco al volver.
        val pm = activity.getSystemService(android.os.PowerManager::class.java)
        val exempt = pm?.isIgnoringBatteryOptimizations(activity.packageName) == true
        val p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!exempt && now - p.getLong(KEY_NAG, 0L) > 3L * 24 * 3600 * 1000) {
            p.edit().putLong(KEY_NAG, now).apply()
            activity.startActivity(Intent(activity, FirstRunActivity::class.java))
        }
    }
}
