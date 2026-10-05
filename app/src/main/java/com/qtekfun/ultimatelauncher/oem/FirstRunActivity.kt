// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.oem

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.android.launcher3.R
import com.qtekfun.ultimatelauncher.ui.ScreenLayout
import java.util.Locale

/**
 * Asistente mínimo de primer arranque: launcher predeterminado, batería y autoarranque, con el adaptador de la marca.
 * Cada paso prueba una lista de intents y usa el primero que se abre; si ninguno, abre los detalles de la app.
 * Sin Compose y sin red. Se puede repetir desde el icono «UltimateLauncher» del cajón.
 */
class FirstRunActivity : Activity() {
    private lateinit var adapter: OemAdapter
    private lateinit var statusHome: TextView
    private lateinit var statusBattery: TextView
    private val lang get() = Locale.getDefault().language

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = OemAdapters.forThisDevice(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        fun text(s: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
            this.text = s; textSize = size; if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, pad / 2, 0, pad / 4)
        }
        fun button(label: String, onClick: () -> Unit) = Button(this).apply { text = label; setOnClickListener { onClick() } }

        root.addView(text(getString(R.string.ul_wizard_title), 24f, true))
        root.addView(text(getString(R.string.ul_wizard_adapter, adapter.id)))

        root.addView(text(getString(R.string.ul_step_home), 18f, true))
        statusHome = text("")
        root.addView(statusHome)
        root.addView(text(adapter.defaultLauncherHelp().forLang(lang)))
        root.addView(button(getString(R.string.ul_btn_home)) { requestHome() })

        root.addView(text(getString(R.string.ul_step_battery), 18f, true))
        root.addView(text(getString(R.string.ul_battery_help)))
        statusBattery = text("")
        root.addView(statusBattery)
        root.addView(button(getString(R.string.ul_btn_battery)) { requestBatteryExemption() })

        root.addView(text(getString(R.string.ul_step_autostart), 18f, true))
        root.addView(text(getString(R.string.ul_autostart_help)))
        root.addView(button(getString(R.string.ul_btn_open)) { openFirst(adapter.autostartIntents()) })

        // Paso opcional: traer la pantalla de inicio de otro launcher (no cambia ningún estado del asistente).
        root.addView(text(getString(R.string.ul_step_import), 18f, true))
        root.addView(text(getString(R.string.ul_import_help)))
        root.addView(button(getString(R.string.ul_btn_import)) {
            startActivity(Intent(this, com.qtekfun.ultimatelauncher.importer.ForeignImportActivity::class.java))
        })

        val issues = adapter.knownIssues()
        if (issues.isNotEmpty()) {
            root.addView(text(getString(R.string.ul_known_issues), 18f, true))
            issues.forEach { root.addView(text("• " + it.text.forLang(lang), 14f)) }
        }
        root.addView(button(getString(R.string.ul_btn_done)) { FirstRun.markDone(this); finish() }.apply {
            (layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.END
        })
        setContentView(ScreenLayout.scrollColumn(this, root))
    }

    override fun onResume() {
        super.onResume()
        statusHome.text = getString(if (isDefaultHome()) R.string.ul_home_yes else R.string.ul_home_no)
        statusBattery.text = getString(if (isBatteryExempt()) R.string.ul_battery_yes else R.string.ul_battery_no)
    }

    private fun isDefaultHome(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && rm.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun isBatteryExempt(): Boolean =
        getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true

    /**
     * Pide al sistema que no optimice la batería de este launcher (diálogo directo, como UltimateDeck). Si no existe el
     * diálogo en la ROM, se abre la lista de optimización y, en último caso, los detalles de la app (reserva de docs/05).
     */
    private fun requestBatteryExemption() {
        if (isBatteryExempt()) return
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        try { startActivity(direct) } catch (e: ActivityNotFoundException) { openFirst(adapter.batteryIntents()) }
    }

    private fun requestHome() {
        val rm = getSystemService(RoleManager::class.java)
        val intent = if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(Settings.ACTION_HOME_SETTINGS)
        try { startActivity(intent) } catch (e: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    /** Usa el primer intent que se pueda abrir; si ninguno, los detalles de la app (reserva de docs/05). */
    private fun openFirst(candidates: List<Intent>) {
        for (i in candidates) {
            try { startActivity(i); return } catch (e: ActivityNotFoundException) { /* siguiente */ } catch (e: SecurityException) { /* siguiente */ }
        }
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}
