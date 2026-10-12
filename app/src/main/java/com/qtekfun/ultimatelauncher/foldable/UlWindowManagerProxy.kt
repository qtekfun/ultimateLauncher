// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.foldable

import android.content.Context
import android.graphics.Point
import android.util.ArrayMap
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.util.WindowBounds
import com.android.launcher3.util.window.CachedDisplayInfo
import com.android.launcher3.util.window.WindowManagerProxy
import javax.inject.Inject

/**
 * `WindowManagerProxy` con todas las pantallas internas (parche 0230, enlazado en `WindowManagerProxyModule`).
 *
 * La versión de AOSP sin Quickstep solo estima la pantalla activa: un plegable parecía una tablet abierto y un móvil
 * cerrado, nunca `TYPE_MULTI_DISPLAY`, así que no había dos paneles, cada pantalla tenía su rejilla y su base de datos y
 * el dock de tablet aparecía donde no tocaba. Aquí se añaden las demás pantallas internas (ver [Foldables]) cuando el
 * conjunto es de plegable; en móviles y tablets el resultado es el mismo que en AOSP.
 */
@LauncherAppSingleton
class UlWindowManagerProxy @Inject constructor() : WindowManagerProxy() {

    override fun estimateInternalDisplayBounds(
        displayInfoContext: Context,
    ): ArrayMap<CachedDisplayInfo, List<WindowBounds>> {
        val result = super.estimateInternalDisplayBounds(displayInfoContext)
        try {
            val display: Display? = displayInfoContext.display
            Foldables.remember(displayInfoContext, display)
            val current = Foldables.physicalSize(display) ?: return result
            val sizes = Foldables.builtInSizes(displayInfoContext)
            if (!FoldLogic.isFoldable(sizes + current, DisplayMetrics.DENSITY_DEVICE_STABLE)) return result
            for (size in sizes) {
                if (size == current) continue
                // Rotación natural: la estrecha en vertical, como las pantallas de los plegables de referencia.
                val info = CachedDisplayInfo(Point(size.short, size.long), Surface.ROTATION_0)
                if (!result.containsKey(info)) result[info] = estimateWindowBounds(displayInfoContext, info)
            }
        } catch (_: RuntimeException) {
            // Contexto sin pantalla u otra rareza de la ROM: se queda la estimación de AOSP (solo la pantalla activa).
        }
        return result
    }
}
