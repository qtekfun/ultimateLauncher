// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.hidden

/**
 * Lógica pura (sin Android) de «Ocultar apps» (parche 0181–0183): el conjunto de apps ocultas son nombres de componente
 * aplanados (`paquete/Clase`, el formato de `ComponentName.flattenToString()`), solo del perfil principal del usuario.
 * No contiene nada más: ni etiquetas ni iconos ni fechas.
 *
 * Formato guardado en las preferencias privadas del launcher (clave [PREF_KEY]): un componente por línea, ordenados y
 * sin repetir. Las líneas vacías o mal formadas se ignoran al leer, de modo que un valor dañado nunca rompe el cajón.
 */
object HiddenAppsLogic {
    const val PREF_KEY = "pref_ul_hidden_apps"
    private const val SEPARATOR = '\n'

    /** Un componente válido es `paquete/Clase` sin saltos de línea ni espacios sobrantes. */
    @JvmStatic fun isValidKey(key: String?): Boolean {
        if (key == null || key.isEmpty() || key != key.trim() || key.indexOf(SEPARATOR) >= 0 || key.indexOf('\r') >= 0) return false
        val slash = key.indexOf('/')
        return slash > 0 && slash < key.length - 1
    }

    @JvmStatic fun parse(raw: String?): Set<String> {
        if (raw.isNullOrEmpty()) return emptySet()
        return raw.split(SEPARATOR).map { it.trim() }.filter { isValidKey(it) }.toSet()
    }

    @JvmStatic fun serialize(keys: Collection<String>): String =
        keys.filter { isValidKey(it) }.toSortedSet().joinToString(SEPARATOR.toString())

    @JvmStatic fun withHidden(keys: Set<String>, key: String): Set<String> =
        if (isValidKey(key)) keys + key else keys

    @JvmStatic fun withShown(keys: Set<String>, key: String): Set<String> = keys - key

    @JvmStatic fun isHidden(keys: Set<String>, key: String?): Boolean = key != null && keys.contains(key)

    /** Quita de `items` los ocultos (`keyOf` devuelve el componente aplanado de cada elemento, o null si no aplica). */
    @JvmStatic fun <T> filterVisible(items: List<T>, keys: Set<String>, keyOf: (T) -> String?): List<T> =
        if (keys.isEmpty()) items else items.filter { !isHidden(keys, keyOf(it)) }

    /** Los ocultos de `items`, para la lista «Apps ocultas». */
    @JvmStatic fun <T> onlyHidden(items: List<T>, keys: Set<String>, keyOf: (T) -> String?): List<T> =
        if (keys.isEmpty()) emptyList() else items.filter { isHidden(keys, keyOf(it)) }
}
