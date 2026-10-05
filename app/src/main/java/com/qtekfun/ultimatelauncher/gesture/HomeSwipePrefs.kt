// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.gesture

import android.content.Context
import com.android.launcher3.LauncherFiles

/** Preferencias del gesto de deslizar hacia abajo (parche 0180). Claves estables en las preferencias del launcher. */
object HomeSwipePrefs {
    const val KEY_ENABLED = "pref_ul_swipe_down"
    const val KEY_SPLIT = "pref_ul_swipe_split"
    const val KEY_SWAP = "pref_ul_swipe_swap"

    @JvmStatic fun read(context: Context): HomeSwipeConfig = try {
        val p = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        HomeSwipeConfig(
            enabled = p.getBoolean(KEY_ENABLED, false),
            splitHalves = p.getBoolean(KEY_SPLIT, true),
            swapped = p.getBoolean(KEY_SWAP, false),
        )
    } catch (e: Exception) { HomeSwipeConfig() }
}
