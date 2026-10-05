package com.qtekfun.ultimatelauncher.diag

import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.android.launcher3.BubbleTextView
import com.android.launcher3.BuildConfig
import com.android.launcher3.Launcher

/**
 * Diagnóstico SOLO de compilaciones debug (parche 0044): registra el ciclo de vida del launcher y el estado de los iconos
 * poco después de volver (gesto de inicio). Etiqueta de logcat: ULDIAG. No guarda ni envía nada; sin nombres de apps.
 */
object UlDiag {
    private const val TAG = "ULDIAG"

    @JvmStatic
    fun event(name: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, "${SystemClock.uptimeMillis()} evento=$name")
    }

    /** Estado de los iconos ahora y a +0,5 s, +1,5 s y +4 s. */
    @JvmStatic
    fun snapshotAfterResume(launcher: Launcher) {
        if (!BuildConfig.DEBUG) return
        for (delay in longArrayOf(0, 250, 500, 750, 1000, 1500, 2000, 3000, 4000, 6000)) {
            launcher.window?.decorView?.postDelayed({ dump(launcher, "+${delay}ms") }, delay)
        }
    }

    private fun dump(l: Launcher, label: String) {
        var icons = 0; var noDrawable = 0; var invisible = 0; var zeroAlpha = 0
        fun walk(v: View) {
            if (v is BubbleTextView) {
                icons++
                if (v.icon == null) noDrawable++
                if (v.visibility != View.VISIBLE || !v.isShown) invisible++
                if (v.alpha < 0.05f) zeroAlpha++
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(l.workspace); walk(l.hotseat)
        Log.i(TAG, "${SystemClock.uptimeMillis()} $label iconos=$icons sinDrawable=$noDrawable noVisibles=$invisible alfa0=$zeroAlpha " +
            "wsAlpha=${l.workspace.alpha} wsVis=${l.workspace.visibility} hsAlpha=${l.hotseat.alpha} estado=${l.stateManager.state} " +
            "modeloCargado=${l.isWorkspaceLoading.not()} focus=${l.window?.decorView?.hasWindowFocus()}")
    }
}
