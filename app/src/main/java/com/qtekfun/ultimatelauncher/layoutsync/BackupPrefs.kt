// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.layoutsync

/**
 * Ajustes del launcher que entran en la copia (esquema v2 de docs/06). Lista blanca EXPLÍCITA con el tipo de cada clave:
 * al guardar solo se leen estas claves de las SharedPreferences del launcher y al restaurar se descarta cualquier clave
 * desconocida o con tipo distinto (un archivo ajeno o manipulado no puede escribir otras preferencias).
 * Parte pura (sin Android) para poder probarla. Los recientes del dock (`ul_recents`) NO entran: son historial de uso.
 */
object BackupPrefs {
    /** Nombre sugerido ultimatelauncher-AAAA-MM-DD.json (fecha local, sin datos del dispositivo). */
    fun suggestedName(now: java.util.Date = java.util.Date()): String =
        "ultimatelauncher-" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(now) + ".json"

    @Volatile private var restores = 0

    /** Se llama al escribir los ajustes de una copia restaurada. Ajustes, abierta en otra tarea, lo compara al volver. */
    @JvmStatic fun noteRestored() { restores++ }

    /** Nº de restauraciones de ajustes desde que arrancó el proceso (la pantalla de Ajustes recuerda el que vio al crearse). */
    @JvmStatic fun restoreCount(): Int = restores

    enum class Kind { BOOL, INT, STRING }

    /** Clave → tipo. Al añadir un interruptor `pref_ul_*` nuevo hay que añadirlo aquí (lo comprueba BackupPrefsTest). */
    val SPEC: Map<String, Kind> = linkedMapOf(
        "pref_add_icon_to_home" to Kind.BOOL,       // AOSP: añadir apps nuevas al inicio
        "pref_ul_dock_background" to Kind.BOOL,     // 0041
        "pref_ul_dock_subtle" to Kind.BOOL,
        "pref_ul_dock_recents" to Kind.BOOL,
        "pref_ul_open_scale" to Kind.BOOL,          // 0043
        "pref_ul_return_pop" to Kind.BOOL,
        "pref_ul_gesture_contract" to Kind.BOOL,    // 0046
        "pref_ul_folder_style" to Kind.BOOL,        // 0048
        "pref_ul_folder_expand" to Kind.BOOL,       // 0143
        "pref_ul_allow_rotation" to Kind.BOOL,      // 0061
        "pref_ul_edge_grid" to Kind.BOOL,           // 0120
        "pref_ul_swipe_down" to Kind.BOOL,          // 0180
        "pref_ul_swipe_split" to Kind.BOOL,
        "pref_ul_swipe_swap" to Kind.BOOL,
        "pref_ul_icon_pack" to Kind.STRING,         // 0171 (paquete elegido; la lista de apps ocultas NO se copia: es sensible)
        "pref_ul_icon_pack_back" to Kind.BOOL,
    )

    /** Filtra un mapa crudo (de SharedPreferences.getAll() o de JSON): conserva solo claves conocidas con el tipo correcto. */
    fun filter(raw: Map<String, Any?>): Map<String, Any> {
        val out = linkedMapOf<String, Any>()
        for ((k, kind) in SPEC) {
            val v = raw[k] ?: continue
            when (kind) {
                Kind.BOOL -> if (v is Boolean) out[k] = v
                Kind.INT -> if (v is Int) out[k] = v else if (v is Long && v in Int.MIN_VALUE..Int.MAX_VALUE) out[k] = v.toInt()
                Kind.STRING -> if (v is String) out[k] = v
            }
        }
        return out
    }
}
