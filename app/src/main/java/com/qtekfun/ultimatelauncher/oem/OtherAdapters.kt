package com.qtekfun.ultimatelauncher.oem

import android.content.Context
import android.content.Intent

/**
 * Adaptadores de otras familias de ROM (docs/05). Las rutas concretas viven en assets/oem-intents.json y están marcadas como
 * NO verificadas salvo que se hayan comprobado en un dispositivo real. Todas añaden además la reserva genérica
 * (diálogo de exclusión de batería y detalles de la app).
 */
internal fun matchesBrand(device: DeviceInfo, vararg brands: String): Boolean {
    val s = brands.toSet()
    return device.manufacturer.lowercase() in s || device.brand.lowercase() in s
}

abstract class FamilyAdapter(private val context: Context?, override val id: String) : OemAdapter {
    protected val generic = GenericAdapter(context)
    override fun autostartIntents(): List<Intent> = context?.let { OemIntentConfig.load(it, id, "autostart") } ?: emptyList()
    override fun batteryIntents(): List<Intent> =
        (context?.let { OemIntentConfig.load(it, id, "battery") } ?: emptyList()) + generic.batteryIntents()
    override fun defaultLauncherHelp() = generic.defaultLauncherHelp()
}

/** vivo / iQOO (Funtouch OS, OriginOS). */
class VivoAdapter(context: Context? = null) : FamilyAdapter(context, "vivo") {
    override fun matches(device: DeviceInfo) = matchesBrand(device, "vivo", "iqoo")
    override fun knownIssues() = listOf(KnownIssue("background-kill", HelpText(mapOf(
        "en" to "vivo closes background apps aggressively: allow background activity and auto-start for UltimateLauncher.",
        "es" to "vivo cierra las apps en segundo plano de forma agresiva: permite la actividad en segundo plano y el inicio automático para UltimateLauncher."))))
}

/** Xiaomi / Redmi / POCO (HyperOS, MIUI). */
class HyperOsAdapter(context: Context? = null) : FamilyAdapter(context, "hyperos") {
    override fun matches(device: DeviceInfo) = matchesBrand(device, "xiaomi", "redmi", "poco")
    override fun knownIssues() = listOf(KnownIssue("background-kill", HelpText(mapOf(
        "en" to "HyperOS needs Auto-start enabled and Battery saver set to «No restrictions» for UltimateLauncher.",
        "es" to "HyperOS necesita el Inicio automático activado y el ahorro de batería en «Sin restricciones» para UltimateLauncher."))))
}

/** Honor (MagicOS) y Huawei (EMUI/HarmonyOS con Android): comparten el gestor de teléfono. */
class MagicOsAdapter(context: Context? = null) : FamilyAdapter(context, "magicos") {
    override fun matches(device: DeviceInfo) = matchesBrand(device, "honor", "huawei")
    override fun knownIssues() = listOf(KnownIssue("background-kill", HelpText(mapOf(
        "en" to "Set UltimateLauncher to «Manage manually» in Battery > App launch and allow auto-launch, secondary launch and run in background.",
        "es" to "Pon UltimateLauncher en «Administrar manualmente» en Batería > Inicio de aplicaciones y permite inicio automático, inicio secundario y ejecución en segundo plano."))))
}
