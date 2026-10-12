// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.foldable

/** Tamaño físico de una pantalla en píxeles, normalizado con el lado corto primero. */
data class PanelSize(val short: Int, val long: Int) {
    override fun toString() = "${short}x$long"

    companion object {
        fun of(w: Int, h: Int) = PanelSize(minOf(w, h), maxOf(w, h))

        fun parse(raw: String): PanelSize? {
            val parts = raw.split('x')
            if (parts.size != 2) return null
            val a = parts[0].toIntOrNull() ?: return null
            val b = parts[1].toIntOrNull() ?: return null
            return if (a > 0 && b > 0 && a <= MAX_PX && b <= MAX_PX) of(a, b) else null
        }

        private const val MAX_PX = 16384
    }
}

/**
 * Lógica pura (sin Android) de plegables (docs/11). Un plegable es un dispositivo con, a la vez, una pantalla interna de
 * móvil (lado corto < 600 dp) y otra grande (>= 600 dp): el mismo criterio que `LauncherDisplayInfo.deviceType` de AOSP
 * para `TYPE_MULTI_DISPLAY`, que esta base sin Quickstep no podía aplicar porque solo veía la pantalla activa.
 */
object FoldLogic {
    /** Lado corto mínimo (dp) de una pantalla grande; igual que `WindowManagerProxy.MIN_TABLET_WIDTH`. */
    const val TABLET_MIN_DP = 600f

    /** Pantallas recordadas como máximo (lista de reserva para Android < 17). */
    const val MAX_REMEMBERED = 4

    @JvmStatic
    fun smallestDp(size: PanelSize, densityDpi: Int): Float = size.short * 160f / densityDpi

    /** ¿Hay a la vez una pantalla de móvil y una grande? */
    @JvmStatic
    fun isFoldable(sizes: Collection<PanelSize>, densityDpi: Int): Boolean {
        if (densityDpi <= 0) return false
        val dps = sizes.map { smallestDp(it, densityDpi) }
        return dps.any { it < TABLET_MIN_DP } && dps.any { it >= TABLET_MIN_DP }
    }

    /** Lista recordada tras ver `seen`: la más reciente primero, sin repetir y con [MAX_REMEMBERED] como máximo. */
    @JvmStatic
    fun remember(previous: List<PanelSize>, seen: PanelSize): List<PanelSize> =
        (listOf(seen) + previous.filter { it != seen }).take(MAX_REMEMBERED)

    @JvmStatic
    fun encode(sizes: List<PanelSize>): String = sizes.joinToString(",")

    @JvmStatic
    fun decode(raw: String?): List<PanelSize> =
        raw.orEmpty().split(',').mapNotNull { PanelSize.parse(it.trim()) }.distinct().take(MAX_REMEMBERED)
}
