package com.qtekfun.ultimatelauncher.hidden

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import com.android.launcher3.LauncherFiles
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo

/**
 * Acceso a las apps ocultas (parches 0181–0183). Se guardan en las preferencias privadas del launcher; la lógica de
 * formato y filtrado está en [HiddenAppsLogic]. Decisión (docs/DECISIONS.md): ocultar solo quita la app del cajón y de su
 * búsqueda; los iconos que el usuario ya tenga en el escritorio, carpetas o dock se mantienen y se abren con normalidad.
 * Solo se pueden ocultar apps del perfil principal (no del trabajo ni del espacio privado, que ya tienen su protección).
 */
object HiddenApps {
    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    @JvmStatic fun hiddenSet(context: Context): Set<String> = try {
        HiddenAppsLogic.parse(prefs(context).getString(HiddenAppsLogic.PREF_KEY, null))
    } catch (e: Exception) { emptySet() }

    @JvmStatic fun keyOf(component: ComponentName?): String? = component?.flattenToString()

    /** ¿Esta app del cajón está oculta? Solo cuenta el perfil principal. */
    @JvmStatic fun isHidden(hidden: Set<String>, info: ItemInfo): Boolean {
        if (hidden.isEmpty() || info.user != Process.myUserHandle()) return false
        return HiddenAppsLogic.isHidden(hidden, keyOf(info.targetComponent))
    }

    /** Apps visibles (para la búsqueda del cajón, que trabaja con la lista del modelo y no con la del almacén). */
    @JvmStatic fun visibleApps(context: Context, apps: List<AppInfo>): List<AppInfo> {
        val hidden = hiddenSet(context)
        if (hidden.isEmpty()) return apps
        return apps.filter { !isHidden(hidden, it) }
    }

    @JvmStatic fun setHidden(context: Context, component: ComponentName, hidden: Boolean) {
        val key = keyOf(component) ?: return
        val now = hiddenSet(context)
        val next = if (hidden) HiddenAppsLogic.withHidden(now, key) else HiddenAppsLogic.withShown(now, key)
        if (next == now) return
        prefs(context).edit().putString(HiddenAppsLogic.PREF_KEY, HiddenAppsLogic.serialize(next)).apply()
    }

    /**
     * Avisa (en el hilo principal) cuando cambia la lista. Quien llame debe conservar el objeto devuelto mientras lo
     * necesite: Android solo guarda una referencia débil al oyente.
     */
    @JvmStatic fun observe(context: Context, onChange: Runnable): SharedPreferences.OnSharedPreferenceChangeListener {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == HiddenAppsLogic.PREF_KEY) onChange.run()
        }
        prefs(context).registerOnSharedPreferenceChangeListener(l)
        return l
    }
}
