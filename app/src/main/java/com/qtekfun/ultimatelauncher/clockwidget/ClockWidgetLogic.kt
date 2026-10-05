// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.clockwidget

/**
 * Lógica pura (sin Android) del widget «Reloj digital»: qué diseño corresponde a un tamaño y qué patrón
 * de fecha se usa. Mantenerla aquí permite probarla con JUnit sin dispositivo.
 */
enum class ClockLayout { COMPACT, SQUARE, WIDE }

object ClockWidgetLogic {
    /** Tamaño mínimo (dp) a partir del cual se usa cada diseño. Son las claves de `RemoteViews(Map<SizeF, RemoteViews>)`. */
    val minSizes: Map<ClockLayout, Pair<Float, Float>> = linkedMapOf(
        ClockLayout.COMPACT to (100f to 40f),   // 2x1 o 4x1: solo la hora
        ClockLayout.SQUARE to (100f to 100f),   // 2x2: hora y fecha debajo
        ClockLayout.WIDE to (260f to 100f),     // 4x2 (un 3x3 de ~220 dp se queda en el cuadrado): hora a la izquierda, día y fecha a la derecha
    )

    /**
     * Réplica de lo que hace el sistema con el mapa de tamaños: entre los diseños que caben se queda con el de
     * mayor área; si ninguno cabe, con el más pequeño. Sirve para probar que las claves están bien elegidas.
     */
    fun choose(widthDp: Float, heightDp: Float): ClockLayout {
        val fitting = minSizes.filter { (_, s) -> widthDp >= s.first && heightDp >= s.second }
        if (fitting.isEmpty()) return minSizes.minBy { (_, s) -> s.first * s.second }.key
        return fitting.maxBy { (_, s) -> s.first * s.second }.key
    }

    /** Lado (dp) de la tarjeta cuadrada del reloj 2x2: el menor de los dos lados del área. */
    fun squareSide(widthDp: Float, heightDp: Float): Float = minOf(widthDp, heightDp)

    /**
     * Diseño del reloj de tarjeta cuadrada: si el área es al menos 100x100 dp, SQUARE (nunca WIDE: ese es otro
     * widget); si es más baja o estrecha, COMPACT a rectángulo completo (2x1, 4x1).
     */
    fun chooseSquare(widthDp: Float, heightDp: Float): ClockLayout {
        val sq = minSizes.getValue(ClockLayout.SQUARE)
        return if (widthDp >= sq.first && heightDp >= sq.second) ClockLayout.SQUARE else ClockLayout.COMPACT
    }

    /** Esqueletos de fecha que se pasan a `DateFormat.getBestDateTimePattern` (localizan orden y separadores). */
    const val SKELETON_DAY_DATE = "EEEMMMd"   // «lun, 5 oct» / «Mon, Oct 5»
    const val SKELETON_WEEKDAY = "EEEE"       // «lunes»
    const val SKELETON_DAY_MONTH = "MMMMd"    // «5 de octubre» / «October 5»

    /** Patrón de reserva si el sistema devuelve algo vacío (no debería ocurrir). */
    const val FALLBACK_DATE_PATTERN = "EEE d MMM"

    /** Pide el mejor patrón para [skeleton] con [resolver] (en producción `DateFormat.getBestDateTimePattern`). */
    fun datePattern(skeleton: String, resolver: (String) -> String?): String =
        resolver(skeleton)?.takeIf { it.isNotBlank() } ?: FALLBACK_DATE_PATTERN

    /** Formato de la hora: sin «a. m./p. m.» en 12 h (como un reloj digital minimalista) y con cero inicial en 24 h. */
    const val TIME_FORMAT_12 = "h:mm"
    const val TIME_FORMAT_24 = "HH:mm"
}
