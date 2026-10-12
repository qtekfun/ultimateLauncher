// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

/** Tipo de perfil de usuario visto por el launcher (docs/10). */
enum class ProfileKind { MAIN, PRIVATE, CLONED, WORK, OTHER }

/**
 * Ajustes de Dual apps (docs/10, «Ajustes»).
 * @param enabled interruptor global.
 * @param swipe deslizar hacia arriba sobre un icono del par abre la otra copia.
 * @param hidePairs «ocultar pares»: el launcher no ofrece nada de la otra copia (menú, gesto, mosaico) ni deja fijar
 *        apps del Private Space en el escritorio. Sustituye al modo oculto del sistema cuando no se puede leer.
 */
data class DualAppsConfig(
    val enabled: Boolean = true,
    val swipe: Boolean = true,
    val hidePairs: Boolean = false,
) {
    /** ¿Están activos los accesos a la otra copia? */
    val active: Boolean get() = enabled && !hidePairs
}

/**
 * Lógica pura (sin Android) de Dual apps: qué perfil tiene la otra copia de una app, cuándo existe la función y el
 * umbral del gesto. Un **par** es el mismo paquete con actividad lanzable en el perfil principal y en el Private Space
 * (o en un perfil clonado de AOSP, si la ROM lo expone). El perfil de trabajo nunca se empareja.
 */
object DualAppsLogic {
    /** Private Space y `LauncherApps.getLauncherUserInfo` existen desde Android 15. */
    const val MIN_SDK = 35

    /** Recorrido mínimo (dp) hacia arriba para confirmar el gesto sin velocidad. */
    const val MIN_DISTANCE_DP = 48f

    /** Velocidad mínima (dp/s) hacia arriba para confirmar un gesto corto. */
    const val MIN_FLING_DP_PER_S = 500f

    /** ¿Puede funcionar Dual apps? Solo API 35+ y solo en el usuario principal (el Private Space vive ahí). */
    @JvmStatic
    fun isSupported(sdkInt: Int, isMainUser: Boolean): Boolean = sdkInt >= MIN_SDK && isMainUser

    /** Perfiles donde buscar la otra copia, por orden de preferencia. Vacío si el perfil no se empareja. */
    @JvmStatic
    fun counterpartKinds(from: ProfileKind): List<ProfileKind> = when (from) {
        ProfileKind.MAIN -> listOf(ProfileKind.PRIVATE, ProfileKind.CLONED)
        ProfileKind.PRIVATE, ProfileKind.CLONED -> listOf(ProfileKind.MAIN)
        ProfileKind.WORK, ProfileKind.OTHER -> emptyList()
    }

    /**
     * Perfil de la otra copia de `pkg` abierto desde `from`, o null si no hay par.
     * @param launchable paquetes con actividad lanzable por perfil.
     */
    @JvmStatic
    fun counterpart(pkg: String, from: ProfileKind, launchable: Map<ProfileKind, Set<String>>): ProfileKind? {
        if (pkg !in launchable[from].orEmpty()) return null
        return counterpartKinds(from).firstOrNull { pkg in launchable[it].orEmpty() }
    }

    /** Paquetes emparejados: en el perfil principal y en el Private Space o en un perfil clonado. */
    @JvmStatic
    fun pairs(launchable: Map<ProfileKind, Set<String>>): Set<String> {
        val main = launchable[ProfileKind.MAIN].orEmpty()
        val second = launchable[ProfileKind.PRIVATE].orEmpty() + launchable[ProfileKind.CLONED].orEmpty()
        return main.intersect(second)
    }

    /**
     * Actividad de la otra copia: la misma clase si existe allí; si no, la primera lanzable (la app puede declarar
     * actividades de lanzador distintas en cada perfil). Null si la lista está vacía.
     */
    @JvmStatic
    fun pickActivity(sameClass: String?, candidates: List<String>): String? =
        candidates.firstOrNull { it == sameClass } ?: candidates.firstOrNull()

    /** «Añadir cuenta 2 al escritorio»: solo desde la copia principal y si la otra no está ya en el escritorio. */
    @JvmStatic
    fun offerAddToHome(from: ProfileKind, to: ProfileKind?, alreadyOnHome: Boolean): Boolean =
        from == ProfileKind.MAIN && (to == ProfileKind.PRIVATE || to == ProfileKind.CLONED) && !alreadyOnHome

    /**
     * ¿Se confirma el deslizamiento hacia arriba? Convención de `SingleAxisSwipeDetector` vertical: `dy < 0` y `vy < 0`
     * hacia arriba. Debe ser predominantemente vertical y con recorrido o velocidad suficientes.
     */
    @JvmStatic
    fun isSwipeUp(dx: Float, dy: Float, vy: Float, minDistancePx: Float, minFlingPxPerS: Float): Boolean {
        val up = -dy
        if (up <= 0f) return false
        if (Math.abs(dx) > up) return false
        return up >= minDistancePx || -vy >= minFlingPxPerS
    }

    /** Valor guardado de la última app emparejada abierta: `paquete` + salto de línea + perfil. */
    @JvmStatic
    fun encodeLast(pkg: String, kind: ProfileKind): String? =
        if (isValidPackage(pkg) && kind in PAIRABLE) "$pkg\n${kind.name}" else null

    /** Lee el valor de [encodeLast]; null si está vacío o dañado. */
    @JvmStatic
    fun decodeLast(raw: String?): Pair<String, ProfileKind>? {
        val parts = raw?.split('\n') ?: return null
        if (parts.size != 2 || !isValidPackage(parts[0])) return null
        val kind = ProfileKind.entries.firstOrNull { it.name == parts[1] } ?: return null
        return if (kind in PAIRABLE) parts[0] to kind else null
    }

    private val PAIRABLE = setOf(ProfileKind.MAIN, ProfileKind.PRIVATE, ProfileKind.CLONED)
    private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    @JvmStatic
    fun isValidPackage(pkg: String?): Boolean = pkg != null && pkg.length <= 255 && PACKAGE.matches(pkg)
}
