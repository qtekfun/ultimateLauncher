// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dualapps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserManager
import android.view.View
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherFiles
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR

/** Otra copia de una app: componente lanzable y perfil donde vive. */
data class DualTarget(val component: ComponentName, val user: UserHandle, val kind: ProfileKind)

/**
 * Dual apps (docs/10): encuentra la otra copia de una app (perfil principal ↔ Private Space o perfil clonado), la abre
 * y la añade al escritorio. La lógica sin Android está en [DualAppsLogic]. Nada sale del almacenamiento privado del
 * launcher ni va a los registros; la única marca guardada es la última app **emparejada** abierta (para el mosaico de
 * Ajustes rápidos), y no entra en copias ni exportaciones.
 */
object DualApps {
    const val KEY_ENABLED = "pref_ul_dual_apps"
    const val KEY_SWIPE = "pref_ul_dual_swipe"
    const val KEY_HIDE = "pref_ul_dual_hide"
    const val KEY_LAST = "pref_ul_dual_last"

    /**
     * ¿Se pueden fijar apps del Private Space en el escritorio? AOSP las marca como no fijables
     * (`FLAG_NOT_PINNABLE`); el parche 0220 consulta este valor en `AppInfo`. Se actualiza en [init] y al cambiar los
     * ajustes.
     */
    @Volatile @JvmField var pinPrivateApps: Boolean = Build.VERSION.SDK_INT >= DualAppsLogic.MIN_SDK

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    @JvmStatic fun config(context: Context): DualAppsConfig = try {
        val p = prefs(context)
        DualAppsConfig(
            enabled = p.getBoolean(KEY_ENABLED, true),
            swipe = p.getBoolean(KEY_SWIPE, true),
            hidePairs = p.getBoolean(KEY_HIDE, false),
        )
    } catch (e: Exception) { DualAppsConfig() }

    /** ¿Existe Dual apps en este dispositivo y usuario? (API 35+, fuera de un perfil). */
    @JvmStatic fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < DualAppsLogic.MIN_SDK) return false
        val um = context.getSystemService(UserManager::class.java) ?: return false
        return DualAppsLogic.isSupported(Build.VERSION.SDK_INT, !um.isProfile)
    }

    /** ¿Están activos los accesos a la otra copia? */
    @JvmStatic fun isActive(context: Context): Boolean = isSupported(context) && config(context).active

    /**
     * Llamado desde `Launcher.onCreate` (parche 0220): fija [pinPrivateApps] y escucha los ajustes. Si cambia lo que
     * se puede fijar, recarga el modelo para que las apps del cajón recalculen sus marcas.
     */
    @JvmStatic fun init(context: Context) {
        val app = context.applicationContext
        pinPrivateApps = isActive(app)
        if (listener != null) return
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ENABLED || key == KEY_HIDE) {
                val pin = isActive(app)
                if (!pin) forgetLast(app)
                if (pin != pinPrivateApps) {
                    pinPrivateApps = pin
                    LauncherAppState.getInstance(app).model.forceReload("UltimateLauncher dual apps")
                }
            }
        }
        listener = l
        prefs(app).registerOnSharedPreferenceChangeListener(l)
    }

    @JvmStatic fun kindOf(context: Context, user: UserHandle): ProfileKind {
        val info = UserCache.INSTANCE.get(context).getUserInfo(user)
        return when {
            info.isPrivate -> ProfileKind.PRIVATE
            info.isCloned -> ProfileKind.CLONED
            info.isWork -> ProfileKind.WORK
            info.isMain -> ProfileKind.MAIN
            else -> ProfileKind.OTHER
        }
    }

    /** Otra copia de un icono de app, o null si no hay par o Dual apps no está activo. */
    @JvmStatic fun counterpart(context: Context, info: ItemInfo?): DualTarget? {
        if (info == null || info.itemType != ITEM_TYPE_APPLICATION) return null
        val cn = info.targetComponent ?: return null
        return counterpart(context, cn.packageName, cn.className, kindOf(context, info.user))
    }

    /** Otra copia del paquete `pkg` abierto desde un perfil de tipo `from`. */
    @JvmStatic fun counterpart(context: Context, pkg: String, className: String?, from: ProfileKind): DualTarget? {
        if (!isActive(context)) return null
        val la = context.getSystemService(LauncherApps::class.java) ?: return null
        val profiles = UserCache.INSTANCE.get(context).userProfiles
        for (kind in DualAppsLogic.counterpartKinds(from)) {
            for (user in profiles) {
                if (kindOf(context, user) != kind) continue
                val activities = try { la.getActivityList(pkg, user) } catch (e: SecurityException) { emptyList() }
                val name = DualAppsLogic.pickActivity(className, activities.map { it.componentName.className })
                    ?: continue
                return DualTarget(ComponentName(pkg, name), user, kind)
            }
        }
        return null
    }

    /**
     * Abre la otra copia. Si su perfil está bloqueado, el sistema pide la autenticación del Private Space y abre la app
     * al desbloquear (mismo camino que un icono del escritorio en gris).
     */
    @JvmStatic fun launch(context: Context, target: DualTarget, source: View?) {
        if (isQuiet(context, target.user)) {
            unlockThenLaunch(context, target)
            return
        }
        val la = context.getSystemService(LauncherApps::class.java) ?: return
        val bounds = source?.let { v ->
            val pos = IntArray(2)
            v.getLocationOnScreen(pos)
            Rect(pos[0], pos[1], pos[0] + v.width, pos[1] + v.height)
        }
        try {
            la.startMainActivity(target.component, target.user, bounds, null)
            remember(context, target.component.packageName, target.kind)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.ul_dual_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Coloca la otra copia en la primera celda libre del escritorio, por el mismo camino que «Añadir a pantalla de
     * inicio» de AOSP. (La cola de instalación no sirve: descarta las apps del sistema y la primera página.)
     */
    @JvmStatic fun addToHome(launcher: Launcher, target: DualTarget) {
        val app = launcher.appsView.appsStore.getApp(ComponentKey(target.component, target.user))
        if (app == null || !launcher.accessibilityDelegate.addToWorkspace(app, false, null)) {
            Toast.makeText(launcher, R.string.ul_dual_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Gancho de `ItemClickHandler` (parche 0220): si la app abierta tiene par, la recuerda para el mosaico. La
     * comprobación va en segundo plano para no retrasar el lanzamiento.
     */
    @JvmStatic fun onLaunched(context: Context, info: ItemInfo?) {
        if (info == null || info.itemType != ITEM_TYPE_APPLICATION) return
        val cn = info.targetComponent ?: return
        val user = info.user
        val app = context.applicationContext
        if (!isActive(app)) return
        UI_HELPER_EXECUTOR.execute {
            val kind = kindOf(app, user)
            if (counterpart(app, cn.packageName, cn.className, kind) != null) remember(app, cn.packageName, kind)
        }
    }

    private fun isQuiet(context: Context, user: UserHandle): Boolean = try {
        context.getSystemService(UserManager::class.java)?.isQuietModeEnabled(user) == true
    } catch (_: Exception) { false }

    /**
     * Gancho de `ItemClickHandler` antes de lanzar (parche 0220). Con el Private Space bloqueado, Android no pide el
     * desbloqueo por sí mismo (su `UnlaunchableAppActivity` solo atiende perfiles de trabajo): se pide aquí y la app se
     * abre al desbloquear. Devuelve true si se encarga del lanzamiento.
     */
    @JvmStatic fun interceptLaunch(context: Context, info: ItemInfo?): Boolean {
        onLaunched(context, info)
        if (info == null || Build.VERSION.SDK_INT < DualAppsLogic.MIN_SDK) return false
        val cn = info.targetComponent ?: return false
        if (kindOf(context, info.user) != ProfileKind.PRIVATE || !isQuiet(context, info.user)) return false
        unlockThenLaunch(context, DualTarget(cn, info.user, ProfileKind.PRIVATE))
        return true
    }

    /** Pide desbloquear el perfil (autenticación del sistema) y abre `target` cuando quede disponible (máx. 2 min). */
    private fun unlockThenLaunch(context: Context, target: DualTarget) {
        val app = context.applicationContext
        val handler = Handler(Looper.getMainLooper())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val user = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_USER, UserHandle::class.java)
                if (user != target.user) return
                cleanup(this)
                launch(app, target, null)
            }

            fun cleanup(r: BroadcastReceiver) {
                handler.removeCallbacksAndMessages(r)
                try { app.unregisterReceiver(r) } catch (_: IllegalArgumentException) {}
            }
        }
        app.registerReceiver(receiver, IntentFilter(Intent.ACTION_PROFILE_AVAILABLE), Context.RECEIVER_NOT_EXPORTED)
        handler.postAtTime({ receiver.cleanup(receiver) }, receiver, SystemClock.uptimeMillis() + UNLOCK_TIMEOUT_MS)
        val asked = try {
            app.getSystemService(UserManager::class.java)?.requestQuietModeEnabled(false, target.user)
            true
        } catch (_: Exception) { false }
        if (!asked) {
            receiver.cleanup(receiver)
            Toast.makeText(app, R.string.ul_dual_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private const val UNLOCK_TIMEOUT_MS = 120_000L

    /** Recuerda la copia que se acaba de abrir (el mosaico alterna así entre las dos). */
    @JvmStatic fun rememberTarget(context: Context, target: DualTarget) =
        remember(context, target.component.packageName, target.kind)

    private fun remember(context: Context, pkg: String, kind: ProfileKind) {
        val raw = DualAppsLogic.encodeLast(pkg, kind) ?: return
        try { prefs(context).edit().putString(KEY_LAST, raw).apply() } catch (_: Exception) {}
    }

    /** Última app emparejada abierta (paquete y perfil), o null. */
    @JvmStatic fun last(context: Context): Pair<String, ProfileKind>? =
        try { DualAppsLogic.decodeLast(prefs(context).getString(KEY_LAST, null)) } catch (_: Exception) { null }

    /** Olvida la última app (al desactivar Dual apps u «ocultar pares»). */
    @JvmStatic fun forgetLast(context: Context) {
        try { prefs(context).edit().remove(KEY_LAST).apply() } catch (_: Exception) {}
    }
}
