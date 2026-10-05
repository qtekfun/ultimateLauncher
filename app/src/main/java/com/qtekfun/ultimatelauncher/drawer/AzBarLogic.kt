// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.drawer

import kotlin.math.max
import kotlin.math.min

/**
 * Geometría de la barra A-Z del cajón (parches 0023 y 0190). Las medidas de OPPO (paso 48 px, 24 letras, de y=998 a 2129 en
 * 1080x2400) caben en vertical, pero en horizontal la pantalla es más baja que la barra y se cortaba por abajo. [fit] mantiene
 * el diseño medido cuando cabe y, si no, primero sube la barra y después reduce el paso (y el texto en proporción).
 */
object AzBarLogic {
    /** Hueco mínimo que se deja libre bajo la barra (el buscador del cajón va abajo). Valor de diseño, no medición de OPPO. */
    const val BOTTOM_RESERVE_DP = 56f

    /** Margen mínimo sobre la barra, medido desde el borde superior útil. */
    const val MIN_TOP_DP = 16f

    /** Resultado en píxeles: [top] de la primera letra (desde arriba del contenedor), [pitch] por letra y [textScale] (0..1]. */
    data class Fit(val top: Int, val pitch: Int, val textScale: Float)

    /**
     * @param parentHeight alto del contenedor en px (0 si aún no se midió: se devuelve el diseño sin tocar)
     * @param count número de letras
     * @param pitch paso medido (px)
     * @param top posición medida de la primera letra (px)
     * @param bottomReserve hueco libre bajo la barra (px)
     * @param minTop posición mínima de la primera letra (px)
     */
    @JvmStatic
    fun fit(parentHeight: Int, count: Int, pitch: Int, top: Int, bottomReserve: Int, minTop: Int): Fit {
        if (parentHeight <= 0 || count <= 0 || pitch <= 0) return Fit(top, pitch, 1f)
        val bottom = parentHeight - bottomReserve
        val full = count * pitch
        if (top + full <= bottom) return Fit(top, pitch, 1f)
        // 1) Subir la barra todo lo que haga falta (sin pasar de minTop).
        val newTop = min(top, max(minTop, bottom - full))
        if (newTop + full <= bottom) return Fit(newTop, pitch, 1f)
        // 2) Aún no cabe: reducir el paso (mínimo 1 px) y el texto en la misma proporción.
        val p = max(1, (bottom - newTop) / count)
        return Fit(newTop, p, min(1f, p.toFloat() / pitch))
    }
}
