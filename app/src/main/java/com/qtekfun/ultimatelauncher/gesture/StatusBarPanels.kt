// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Abre el panel de notificaciones o de ajustes rápidos. NO existe API pública para ello (solo los métodos ocultos
 * `StatusBarManager.expandNotificationsPanel()` / `expandSettingsPanel()`, "unsupported" en la lista de APIs no SDK;
 * el servicio de accesibilidad sería la alternativa pública pero es un permiso peligroso y queda descartado, ver
 * docs/DECISIONS.md). Requiere el permiso normal `android.permission.EXPAND_STATUS_BAR`, concedido al instalar.
 * Todo va protegido: si falta el permiso o el método no existe o falla (otra versión o fabricante), `expand` devuelve
 * false y el launcher avisa al usuario sin cerrarse.
 */
object StatusBarPanels {
    private const val TAG = "UlStatusBarPanels"
    private const val PERMISSION = "android.permission.EXPAND_STATUS_BAR"

    /** Nombre del método oculto por panel. */
    @JvmStatic fun methodName(panel: Panel): String? = when (panel) {
        Panel.NOTIFICATIONS -> "expandNotificationsPanel"
        Panel.QUICK_SETTINGS -> "expandSettingsPanel"
        Panel.NONE -> null
    }

    @JvmStatic fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("WrongConstant", "PrivateApi", "DiscouragedPrivateApi")
    @JvmStatic fun expand(context: Context, panel: Panel): Boolean {
        val name = methodName(panel) ?: return false
        if (!hasPermission(context)) return false
        return try {
            val service = context.getSystemService("statusbar") ?: return false
            val cls = service.javaClass
            try {
                cls.getMethod(name).invoke(service)
                true
            } catch (e: NoSuchMethodException) {
                // Algunas versiones solo tienen la variante con subpanel (String); null = el panel por defecto.
                cls.getMethod(name, String::class.java).invoke(service, null as String?)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "no se pudo abrir el panel ($name): ${e.javaClass.simpleName}")
            false
        }
    }
}
