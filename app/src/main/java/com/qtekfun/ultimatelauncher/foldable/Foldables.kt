// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.foldable

import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import com.android.launcher3.R

/**
 * Detección de plegables (docs/11). Pantallas internas del dispositivo, también las apagadas:
 *  - Android 17+: `DisplayManager.DISPLAY_CATEGORY_BUILT_IN_DISPLAYS` (API pública; incluye las apagadas).
 *  - Antes: no hay API pública, así que se recuerdan los tamaños físicos de las pantallas en las que se ha abierto el
 *    launcher. Un plegable con Android 12–16 se reconoce tras plegarlo o desplegarlo una vez.
 * Se usa el tamaño físico del modo de pantalla, no el de la ventana: `wm size` o la escala de pantalla no crean
 * pantallas falsas. Solo se guardan tamaños en píxeles, en el almacenamiento privado del launcher, sin copia ni red.
 */
object Foldables {
    private const val PREFS = "ul_displays"
    private const val KEY_SEEN = "seen"
    private const val BUILT_IN_API = 37

    @Volatile private var cached: Boolean? = null

    /** Tamaño físico de `display`, o null si no se puede leer. */
    @JvmStatic
    fun physicalSize(display: Display?): PanelSize? {
        val mode = display?.mode ?: return null
        if (mode.physicalWidth <= 0 || mode.physicalHeight <= 0) return null
        return PanelSize.of(mode.physicalWidth, mode.physicalHeight)
    }

    /** Pantallas internas conocidas (tamaño físico), incluida la activa. */
    @JvmStatic
    fun builtInSizes(context: Context): List<PanelSize> {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= BUILT_IN_API) {
            val dm = app.getSystemService(DisplayManager::class.java)
            val platform = try {
                dm?.getDisplays(DisplayManager.DISPLAY_CATEGORY_BUILT_IN_DISPLAYS).orEmpty()
                    .mapNotNull { physicalSize(it) }.distinct()
            } catch (_: RuntimeException) { emptyList() }
            if (platform.isNotEmpty()) return platform
        }
        return FoldLogic.decode(prefs(app).getString(KEY_SEEN, null))
    }

    /** Recuerda la pantalla activa (solo Android < 17, lista de reserva). Solo la pantalla interna por defecto. */
    @JvmStatic
    fun remember(context: Context, display: Display?) {
        if (Build.VERSION.SDK_INT >= BUILT_IN_API) return // lo da el sistema: no se guarda nada
        if (display == null || display.displayId != Display.DEFAULT_DISPLAY) return
        val size = physicalSize(display) ?: return
        val p = prefs(context.applicationContext)
        val before = FoldLogic.decode(p.getString(KEY_SEEN, null))
        val after = FoldLogic.remember(before, size)
        if (after.toSet() != before.toSet()) {
            p.edit().putString(KEY_SEEN, FoldLogic.encode(after)).apply()
            cached = null
        }
    }

    /** ¿Es un plegable? (pantalla de móvil y pantalla grande en el mismo dispositivo). */
    @JvmStatic
    fun isFoldable(context: Context): Boolean =
        cached ?: FoldLogic.isFoldable(builtInSizes(context), DisplayMetrics.DENSITY_DEVICE_STABLE).also { cached = it }

    /**
     * ¿Tiene el dispositivo dock de tablet en alguna pantalla? Tablets y plegables (en su pantalla interior). Decide si
     * se crea el dock, se guardan recientes y se ven sus ajustes, también desde la pantalla exterior.
     */
    @JvmStatic
    fun hasTabletDock(context: Context): Boolean =
        context.resources.getBoolean(R.bool.ul_huawei_dock) || isFoldable(context)

    /**
     * ¿Dock de tablet en ESTA pantalla? Se decide por la pantalla del perfil (`DeviceProperties.isLargeScreen`), no por
     * los recursos del contexto: en un plegable pueden ser todavía los de la otra pantalla.
     */
    @JvmStatic
    fun tabletDock(isLargeScreen: Boolean): Boolean = isLargeScreen

    @Volatile private var tabletRes: Pair<Int, Resources>? = null

    /**
     * Recursos de pantalla grande (`values-sw600dp`): medidas del dock de tablet sea cual sea la pantalla activa. En
     * móvil esas medidas valen 0, y el dock de un plegable puede crearse con el teléfono cerrado.
     */
    @JvmStatic
    fun tabletRes(context: Context): Resources {
        val config = context.resources.configuration
        tabletRes?.let { (density, res) -> if (density == config.densityDpi) return res }
        val c = android.content.res.Configuration(config).apply {
            smallestScreenWidthDp = maxOf(smallestScreenWidthDp, FoldLogic.TABLET_MIN_DP.toInt())
        }
        val res = context.applicationContext.createConfigurationContext(c).resources
        tabletRes = config.densityDpi to res
        return res
    }

    /**
     * Máscara de icono de tablet (parche 0018). En un plegable no se usa: la forma del icono no debe cambiar al plegar
     * y la pantalla exterior es de móvil, que usa la máscara del sistema.
     */
    @JvmStatic
    fun tabletIconMask(context: Context): String =
        if (isFoldable(context)) "" else context.resources.getString(R.string.ul_icon_mask)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
