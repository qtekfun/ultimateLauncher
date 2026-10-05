// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.content.Context
import android.content.Intent

/** OPPO / OnePlus / realme (ColorOS, OxygenOS, realme UI). Rutas en assets/oem-intents.json. */
class ColorOsAdapter(private val context: Context? = null) : OemAdapter {
    private val generic = GenericAdapter(context)
    override val id = "coloros"
    override fun matches(device: DeviceInfo): Boolean {
        val brands = setOf("oppo", "oneplus", "realme")
        return device.manufacturer.lowercase() in brands || device.brand.lowercase() in brands ||
            device.romProps.keys.any { it.startsWith("ro.build.version.oplusrom") || it.startsWith("ro.build.version.opporom") }
    }
    override fun autostartIntents(): List<Intent> =
        context?.let { OemIntentConfig.load(it, id, "autostart") } ?: emptyList()
    override fun batteryIntents(): List<Intent> =
        (context?.let { OemIntentConfig.load(it, id, "battery") } ?: emptyList()) + generic.batteryIntents()
    override fun defaultLauncherHelp() = generic.defaultLauncherHelp()
    override fun knownIssues() = listOf(
        KnownIssue("recents-gestures", HelpText(mapOf(
            "en" to "Recents and gesture animations still belong to the system launcher; they cannot be replaced without root.",
            "es" to "Los recientes y las animaciones de gestos siguen siendo del launcher del sistema; no se pueden sustituir sin root.",
        ))),
        KnownIssue("background-kill", HelpText(mapOf(
            "en" to "If the launcher restarts often, allow background activity for it in App info > Battery.",
            "es" to "Si el launcher se reinicia a menudo, permite la actividad en segundo plano en Info de la app > Batería.",
        ))),
    )
}
