// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.orientation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import com.qtekfun.ultimatelauncher.RotationPref

/**
 * Lógica pura de la orientación de las pantallas propias (Ajustes, selector de fondos, copia, importación, apps ocultas,
 * asistente...). El problema: el launcher decide su orientación con `RotationHelper` (parche 0061) y en tablet
 * ([isLargeScreen]) deja `UNSPECIFIED`, pero una pantalla secundaria sin orientación declarada se abría en vertical cuando la
 * rotación automática del sistema estaba apagada y el usuario sostenía la tablet en horizontal. Aquí se decide la misma
 * orientación que lleva el launcher, devolviendo una constante de [ActivityInfo].
 */
object ScreenOrientationLogic {
    /** Lado corto mínimo (dp) para considerar «pantalla grande»; el mismo umbral que usa Launcher3 para tablet. */
    const val LARGE_SCREEN_SMALLEST_WIDTH_DP = 600

    @JvmStatic fun isLargeScreen(smallestWidthDp: Int) = smallestWidthDp >= LARGE_SCREEN_SMALLEST_WIDTH_DP

    /**
     * @param largeScreen lado corto >= 600 dp (tablet / plegable abierto)
     * @param autoRotateOn ajuste del sistema «Girar automáticamente» (`ACCELEROMETER_ROTATION`)
     * @param allowRotation interruptor «Girar la pantalla de inicio» (solo cuenta en móvil, como en el launcher)
     * @param launcherLandscape orientación que tiene ahora el launcher: true horizontal, false vertical, null si aún no se sabe
     */
    @JvmStatic
    fun choose(largeScreen: Boolean, autoRotateOn: Boolean, allowRotation: Boolean, launcherLandscape: Boolean?): Int = when {
        // Móvil: igual que el launcher. Con el giro permitido sigue al sistema; si no, vertical fijo.
        !largeScreen -> if (allowRotation) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        // Tablet con rotación automática: la del sistema (el sensor decide), igual que el launcher.
        autoRotateOn -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // Tablet con la rotación bloqueada por el usuario: heredar la orientación del launcher, no la del sistema.
        launcherLandscape == true -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        launcherLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

/**
 * Aplica [ScreenOrientationLogic] a TODAS las actividades del proceso excepto el propio launcher. Se engancha una vez desde
 * `LauncherApplication` (parche 0190) con `onActivityPreCreated`, así no hay que tocar cada pantalla ni cada punto que las abre.
 */
object ScreenOrientation {
    private const val LAUNCHER_CLASS = "com.android.launcher3.Launcher"

    /** Última orientación conocida del launcher (la escribe `RotationHelper`, parche 0190). Mismo proceso, sin persistir. */
    @Volatile private var launcherLandscape: Boolean? = null
    @Volatile private var installed = false

    @JvmStatic fun rememberLauncherOrientation(landscape: Boolean) { launcherLandscape = landscape }

    @JvmStatic fun install(app: Application) {
        if (installed) return
        installed = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity.javaClass.name == LAUNCHER_CLASS) return
                try {
                    activity.requestedOrientation = orientationFor(activity)
                } catch (e: Exception) {
                    // Una pantalla sin la orientación heredada es preferible a un cierre.
                    android.util.Log.w("ULOrientation", "no se pudo fijar la orientación: $e")
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    @JvmStatic fun orientationFor(context: Context): Int {
        val large = ScreenOrientationLogic.isLargeScreen(context.resources.configuration.smallestScreenWidthDp)
        val autoRotate = try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        } catch (e: Exception) { true }
        return ScreenOrientationLogic.choose(large, autoRotate, RotationPref.allowed(context), launcherLandscape)
    }
}
