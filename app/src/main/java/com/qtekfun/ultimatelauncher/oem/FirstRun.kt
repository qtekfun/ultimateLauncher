package com.qtekfun.ultimatelauncher.oem

import android.app.Activity
import android.content.Context
import android.content.Intent

/** Lanza el asistente la primera vez que arranca el launcher (docs/05, RF-40). */
object FirstRun {
    private const val PREFS = "ul_firstrun"
    private const val KEY_DONE = "done"

    fun isDone(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)
    fun markDone(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()

    /** Gancho de una línea desde Launcher.onCreate (parche 0010). */
    @JvmStatic
    fun maybeShow(activity: Activity) {
        if (!isDone(activity)) activity.startActivity(Intent(activity, FirstRunActivity::class.java))
    }
}
