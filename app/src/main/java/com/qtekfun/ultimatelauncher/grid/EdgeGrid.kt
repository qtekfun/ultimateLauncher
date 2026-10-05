package com.qtekfun.ultimatelauncher.grid

import android.content.Context
import com.android.launcher3.LauncherFiles

/**
 * Opción «Iconos hasta el borde» (parche 0120). Solo tablet (lado corto >= 600 dp, una sola página por panel).
 * Apagada = comportamiento medido de Huawei (margen lateral de 120 dp en horizontal). Encendida = la rejilla reparte
 * TODO el ancho disponible entre las columnas y solo deja un mini hueco de EDGE_MARGIN_DP a cada lado.
 */
object EdgeGrid {
    const val KEY = "pref_ul_edge_grid"
    const val MIN_SHORT_SIDE_DP = 600
    /** Mini hueco lateral: el mismo que el del dock al borde inferior (assets/themes/huawei-tablet-medido.json, dock.bottomMarginDp = 12). */
    const val EDGE_MARGIN_DP = 12f

    @JvmStatic fun enabled(context: Context): Boolean = try {
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE).getBoolean(KEY, false)
    } catch (e: Exception) { false }

    /** Solo se aplica en tabletas (lado corto >= 600 dp), nunca en teléfonos ni en plegables de dos paneles. */
    @JvmStatic fun eligible(shortSideDp: Float, isTwoPanels: Boolean, isVerticalLayout: Boolean): Boolean =
        shortSideDp >= MIN_SHORT_SIDE_DP && !isTwoPanels && !isVerticalLayout

    /**
     * Ancho de celda para que `numColumns` celdas (más los `numColumns - 1` huecos de `borderSpaceXPx`) de cada
     * panel ocupen `availableWidthPx / panelCount` sin margen. Se redondea hacia abajo: el sobrante es < numColumns px.
     */
    @JvmStatic fun cellWidthPx(availableWidthPx: Int, panelCount: Int, numColumns: Int, borderSpaceXPx: Int): Int {
        if (availableWidthPx <= 0 || panelCount <= 0 || numColumns <= 0) return 0
        val panelWidth = availableWidthPx / panelCount
        return maxOf(0, (panelWidth - borderSpaceXPx * (numColumns - 1)) / numColumns)
    }
}
