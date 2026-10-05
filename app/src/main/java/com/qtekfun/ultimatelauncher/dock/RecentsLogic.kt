// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.dock

/**
 * Lógica pura (sin Android) de la lista de recientes del dock de tablet, para poder probarla en la JVM.
 * Las entradas son componentes serializados («paquete/clase»), más reciente primero.
 */
object RecentsLogic {
    /** Añade [entry] al principio, sin duplicados, recortando a [max]. */
    fun record(list: List<String>, entry: String, max: Int): List<String> =
        (listOf(entry) + list.filter { it != entry }).take(max.coerceAtLeast(0))

    /** Quita [entry] (pulsación larga → «Quitar de recientes»). */
    fun remove(list: List<String>, entry: String): List<String> = list.filter { it != entry }

    /** Quita las entradas cuyo componente ya no existe (app desinstalada o actividad retirada). */
    fun prune(list: List<String>, exists: (String) -> Boolean): List<String> = list.filter(exists)

    fun parse(raw: String): List<String> = raw.split('|').filter { it.isNotBlank() }

    fun serialize(list: List<String>): String = list.joinToString("|")
}
