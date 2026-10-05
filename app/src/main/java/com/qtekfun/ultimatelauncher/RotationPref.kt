package com.qtekfun.ultimatelauncher

import android.content.Context
import com.android.launcher3.LauncherFiles

/** Interruptor «Girar la pantalla de inicio» (parche 0061). Apagado por defecto: inicio vertical como OPPO. */
object RotationPref {
    const val KEY = "pref_ul_allow_rotation"

    @JvmStatic fun allowed(context: Context): Boolean = try {
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE).getBoolean(KEY, false)
    } catch (e: Exception) { false }
}
