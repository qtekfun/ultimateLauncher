// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.gesture

/** Panel del sistema que abre un deslizamiento hacia abajo en el escritorio. */
enum class Panel { NONE, NOTIFICATIONS, QUICK_SETTINGS }

/**
 * Configuración del gesto (parche 0180).
 * @param enabled interruptor general (apagado por defecto: necesita el permiso EXPAND_STATUS_BAR y una API no pública).
 * @param splitHalves true: mitad izquierda = notificaciones y mitad derecha = ajustes rápidos; false: toda la
 *        pantalla abre notificaciones.
 * @param swapped invierte qué acción hace cada mitad (o, sin dividir, abre ajustes rápidos en toda la pantalla).
 */
data class HomeSwipeConfig(
    val enabled: Boolean = false,
    val splitHalves: Boolean = true,
    val swapped: Boolean = false,
)

/**
 * Lógica pura (sin Android) del gesto «deslizar hacia abajo en el escritorio»: qué panel corresponde a un
 * deslizamiento según el lado donde empezó y si el recorrido o la velocidad bastan. El umbral de arrastre (touch slop)
 * y el reparto con la paginación horizontal los resuelve `SingleAxisSwipeDetector` de Launcher3; aquí solo se decide al
 * soltar. Coordenadas en píxeles, `dy > 0` hacia abajo.
 */
object HomeSwipeLogic {
    /** Recorrido mínimo vertical (dp) para confirmar el gesto sin velocidad. */
    const val MIN_DISTANCE_DP = 56f

    /** Velocidad mínima (dp/s) para confirmar un gesto corto (un «latigazo»). */
    const val MIN_FLING_DP_PER_S = 500f

    /** Panel que corresponde a un gesto que empezó en `downX` de una pantalla de ancho `width` (sin mirar distancia). */
    @JvmStatic
    fun panelForStart(config: HomeSwipeConfig, downX: Float, width: Float): Panel {
        if (!config.enabled) return Panel.NONE
        val rightHalf = width > 0f && downX >= width / 2f
        val notificationsSide = if (!config.splitHalves) true else !rightHalf
        val notifications = notificationsSide != config.swapped
        return if (notifications) Panel.NOTIFICATIONS else Panel.QUICK_SETTINGS
    }

    /**
     * ¿Se confirma el gesto al soltar? Debe ser hacia abajo, predominantemente vertical y con recorrido suficiente o
     * velocidad suficiente. `vy` en px/s (positivo = hacia abajo).
     */
    @JvmStatic
    fun isConfirmed(dx: Float, dy: Float, vy: Float, minDistancePx: Float, minFlingPxPerS: Float): Boolean {
        if (dy <= 0f) return false
        if (Math.abs(dx) > dy) return false
        return dy >= minDistancePx || vy >= minFlingPxPerS
    }

    /** Resultado final: panel a abrir (o NONE). */
    @JvmStatic
    fun classify(
        config: HomeSwipeConfig, downX: Float, width: Float, dx: Float, dy: Float, vy: Float,
        minDistancePx: Float, minFlingPxPerS: Float,
    ): Panel {
        val panel = panelForStart(config, downX, width)
        if (panel == Panel.NONE) return Panel.NONE
        return if (isConfirmed(dx, dy, vy, minDistancePx, minFlingPxPerS)) panel else Panel.NONE
    }
}
