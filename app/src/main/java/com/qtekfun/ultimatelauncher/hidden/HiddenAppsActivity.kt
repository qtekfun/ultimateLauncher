// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.hidden

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.android.launcher3.R
import com.qtekfun.ultimatelauncher.ui.ScreenLayout

/**
 * Pantalla «Apps ocultas» (parche 0183). Exige autenticar con el dispositivo (huella/rostro o PIN, patrón, contraseña)
 * antes de mostrar nada. Se abre desde Ajustes de inicio y desde el menú del fondo del escritorio.
 *  - Lista las apps ocultas: tocar una la abre; «Mostrar de nuevo» la devuelve al cajón.
 *  - «Elegir apps a ocultar» muestra todas las apps y permite marcar varias.
 * Al salir de la pantalla (segundo plano) se cierra: hay que volver a identificarse. La ventana es `FLAG_SECURE`, así que
 * ni las capturas ni la vista de recientes enseñan la lista. Si el usuario quitó su bloqueo de pantalla no hay forma de
 * proteger nada: se avisa y se deja ver la lista para no dejar apps inaccesibles.
 */
class HiddenAppsActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var launcherApps: LauncherApps
    private var picking = false
    private var authSignal: CancellationSignal? = null
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        launcherApps = getSystemService(LauncherApps::class.java)
        title = getString(R.string.ul_hidden_apps)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, dp(24), pad, dp(24))
        }
        // Desplazable, respeta barras/recorte y limita el ancho a ~640 dp en tablet.
        setContentView(ScreenLayout.scrollColumn(this, content))
        if (savedInstanceState == null) startAuth() else finish()
    }

    private fun startAuth() {
        if (!DeviceAuth.hasScreenLock(this)) {
            unlocked = true
            render(noLockNotice = true)
            return
        }
        authSignal = DeviceAuth.authenticate(this, getString(R.string.ul_hidden_apps), getString(R.string.ul_hidden_auth_subtitle)) { ok ->
            if (ok) {
                unlocked = true
                render(noLockNotice = false)
            } else {
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Salir de la pantalla la cierra y descarta el acceso: la próxima vez se vuelve a pedir identificarse.
        if (!isChangingConfigurations) {
            authSignal?.cancel()
            finish()
        }
    }

    private fun render(noLockNotice: Boolean) {
        if (!unlocked) return
        content.removeAllViews()
        content.addView(text(getString(R.string.ul_hidden_apps), 24f, bold = true))
        if (noLockNotice) content.addView(text(getString(R.string.ul_hidden_no_lock_notice), 14f).apply { setPadding(0, dp(8), 0, dp(8)) })

        val hidden = HiddenApps.hiddenSet(this)
        val all = launcherApps.getActivityList(null, Process.myUserHandle())
            .sortedBy { it.label.toString().lowercase() }
        val shown = if (picking) all else HiddenAppsLogic.onlyHidden(all, hidden) { HiddenApps.keyOf(it.componentName) }

        content.addView(Button(this).apply {
            text = getString(if (picking) R.string.ul_hidden_done else R.string.ul_hidden_pick)
            setOnClickListener { picking = !picking; render(noLockNotice) }
        })
        if (picking) content.addView(text(getString(R.string.ul_hidden_pick_help), 14f))
        if (!picking && shown.isEmpty()) content.addView(text(getString(R.string.ul_hidden_empty), 16f).apply { setPadding(0, dp(16), 0, 0) })

        for (info in shown) {
            val key = HiddenApps.keyOf(info.componentName)
            val isHidden = HiddenAppsLogic.isHidden(hidden, key)
            content.addView(row(info, isHidden, noLockNotice))
        }
    }

    private fun row(info: LauncherActivityInfo, isHidden: Boolean, noLockNotice: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            minimumHeight = dp(56)
        }
        val icon: Drawable? = try { info.getIcon(0) } catch (e: Exception) { null }
        row.addView(ImageView(this).apply { setImageDrawable(icon) }, LinearLayout.LayoutParams(dp(40), dp(40)))
        row.addView(text(info.label.toString(), 16f).apply { setPadding(dp(16), 0, dp(8), 0) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (picking) {
            row.addView(Button(this).apply {
                text = getString(if (isHidden) R.string.ul_hidden_unhide else R.string.ul_hide_app)
                setOnClickListener {
                    if (!isHidden && !DeviceAuth.hasScreenLock(this@HiddenAppsActivity)) {
                        Toast.makeText(context, R.string.ul_hide_needs_lock, Toast.LENGTH_LONG).show()
                    } else {
                        HiddenApps.setHidden(context, info.componentName, !isHidden)
                        render(noLockNotice)
                    }
                }
            })
        } else {
            row.setOnClickListener {
                try {
                    launcherApps.startMainActivity(info.componentName, Process.myUserHandle(), null, null)
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.ul_hidden_cannot_open, Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(Button(this).apply {
                text = getString(R.string.ul_hidden_unhide)
                setOnClickListener { HiddenApps.setHidden(context, info.componentName, false); render(noLockNotice) }
            })
        }
        return row
    }

    private fun text(s: CharSequence, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)) setTextColor(getColor(tv.resourceId))
    }

    private fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
