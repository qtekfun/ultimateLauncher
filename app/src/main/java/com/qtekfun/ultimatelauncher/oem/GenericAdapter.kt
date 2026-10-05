// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Siempre presente: usa solo intents estándar de Android. */
class GenericAdapter(private val context: Context? = null) : OemAdapter {
    override val id = "generic"
    override fun matches(device: DeviceInfo) = true
    override fun autostartIntents(): List<Intent> = emptyList()
    override fun batteryIntents(): List<Intent> = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetails(),
    )
    override fun defaultLauncherHelp() = HelpText(mapOf(
        "en" to "Open Settings > Apps > Default apps > Home app and choose UltimateLauncher.",
        "es" to "Abre Ajustes > Aplicaciones > Aplicaciones predeterminadas > App de inicio y elige UltimateLauncher.",
    ))
    override fun knownIssues(): List<KnownIssue> = emptyList()
    private fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:" + (context?.packageName ?: "com.qtekfun.ultimatelauncher")))
}
