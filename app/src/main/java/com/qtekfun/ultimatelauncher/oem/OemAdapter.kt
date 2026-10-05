// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.content.Intent

/** Texto de ayuda por idioma. Las claves son códigos ISO de dos letras; "en" es la reserva. */
data class HelpText(val byLang: Map<String, String>) {
    fun forLang(lang: String): String = byLang[lang] ?: byLang["en"] ?: byLang.values.firstOrNull().orEmpty()
}

data class KnownIssue(val id: String, val text: HelpText)

/** Información de dispositivo separada de android.os.Build para poder probar la selección sin Android. */
data class DeviceInfo(val manufacturer: String, val brand: String, val romProps: Map<String, String> = emptyMap())

/** Adaptador por familia de ROM (docs/05). */
interface OemAdapter {
    val id: String
    fun matches(device: DeviceInfo): Boolean
    /** Intents candidatos de autoarranque; se usa el primero que resuelva. */
    fun autostartIntents(): List<Intent>
    fun batteryIntents(): List<Intent>
    fun defaultLauncherHelp(): HelpText
    fun knownIssues(): List<KnownIssue>
}
