package com.qtekfun.ultimatelauncher.anim

import android.app.ActivityOptions
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.view.View
import android.view.animation.OvershootInterpolator
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherFiles
import java.lang.ref.WeakReference

/**
 * Animaciones de abrir/volver desde el icono (parche 0043).
 *
 * Límite de plataforma: la animación de la VENTANA de la app al cerrarse hacia el icono la hace una transición remota del
 * sistema (permiso de firma), que un launcher normal no puede registrar. Lo que sí se puede:
 *  - abrir con una animación estándar desde los límites del icono (escala o revelado, a elegir en ajustes);
 *  - al volver, animar el propio icono lanzado (pop con rebote) dentro del launcher.
 */
object OpenReturnAnim {
    const val KEY_OPEN_SCALE = "pref_ul_open_scale"   // true: escala desde el icono; false: revelado de AOSP
    const val KEY_RETURN_POP = "pref_ul_return_pop"   // true: el icono lanzado hace «pop» al volver

    private var launched: WeakReference<View>? = null
    private var launchedAt = 0L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    /** Sustituye a ActivityOptions.makeClipRevealAnimation en ActivityContext.getActivityLaunchOptions. */
    @JvmStatic
    fun makeOptions(v: View, left: Int, top: Int, width: Int, height: Int): ActivityOptions =
        if (prefs(v.context).getBoolean(KEY_OPEN_SCALE, true)) ActivityOptions.makeScaleUpAnimation(v, left, top, width, height)
        else ActivityOptions.makeClipRevealAnimation(v, left, top, width, height)

    /** Llamado al lanzar una app desde una vista del launcher. */
    @JvmStatic
    fun remember(v: View?) {
        launched = v?.let { WeakReference(it) }
        launchedAt = SystemClock.uptimeMillis()
    }

    /** Llamado desde Launcher.onResume: si venimos de lanzar una app, el icono lanzado hace «pop». */
    @JvmStatic
    fun playReturn(launcher: Launcher) {
        val v = launched?.get() ?: return
        launched = null
        if (!prefs(launcher).getBoolean(KEY_RETURN_POP, true)) return
        if (SystemClock.uptimeMillis() - launchedAt > 120_000) return
        if (!v.isAttachedToWindow || !v.isShown) return
        v.animate().cancel()
        v.scaleX = 1.32f; v.scaleY = 1.32f; v.alpha = 0.3f
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300L)
            .setInterpolator(OvershootInterpolator(1.3f)).start()
    }
}
