package com.qtekfun.ultimatelauncher.dock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo

/**
 * Últimas apps abiertas DESDE este launcher, para la zona derecha del dock de tablet (estilo Huawei).
 * Privacidad: solo se guardan hasta `ul_dock_recents_max` nombres de componente en preferencias privadas de la app,
 * nunca salen del dispositivo, se pueden borrar con [clear] y no se usa ningún permiso (no UsageStats).
 * Excepción documentada a docs/09 («sin historial de uso»), pedida expresamente por el usuario.
 */
object RecentApps {
    private const val PREFS = "ul_recents"
    private const val KEY = "list"
    const val CHANGED_KEY = KEY

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun max(context: Context) = context.resources.getInteger(R.integer.ul_dock_recents_max)

    /** Llamado al lanzar una actividad desde el launcher (parche 0040). */
    @JvmStatic
    fun record(context: Context, intent: Intent?, item: ItemInfo?) {
        if (max(context) <= 0 || intent == null || !DockPrefs.recentsEnabled(context)) return
        val cn = intent.component ?: return
        if (cn.packageName == context.packageName) return
        if (!intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action != Intent.ACTION_MAIN) return
        if (item != null && item.container == Favorites.CONTAINER_HOTSEAT) return // ya están en la zona fija
        save(context, RecentsLogic.record(raw(context), cn.flattenToString(), max(context)))
    }

    private fun raw(context: Context): List<String> =
        RecentsLogic.parse(prefs(context).getString(KEY, "").orEmpty())

    /** Pulsación larga → «Quitar de recientes». */
    fun remove(context: Context, cn: ComponentName) =
        save(context, RecentsLogic.remove(raw(context), cn.flattenToString()))

    /**
     * Poda los recientes cuyo componente ya no existe como actividad de lanzador (app desinstalada). No se puede podar «lo que ya
     * no está en las tareas recientes» sin un permiso nuevo (GET_TASKS/REAL_GET_TASKS son de firma y UsageStats exige acceso
     * especial; docs/09 prohíbe ambos): para eso están el borrado manual y «Borrar todos los recientes» en los ajustes.
     */
    fun prune(context: Context, exists: (ComponentName) -> Boolean) {
        val before = raw(context)
        val after = RecentsLogic.prune(before) { s -> ComponentName.unflattenFromString(s)?.let(exists) == true }
        if (after != before) save(context, after)
    }

    fun load(context: Context): List<ComponentName> =
        raw(context).mapNotNull { ComponentName.unflattenFromString(it) }

    @JvmStatic
    fun clear(context: Context) = prefs(context).edit().remove(KEY).apply()

    private fun save(context: Context, list: List<String>) =
        prefs(context).edit().putString(KEY, RecentsLogic.serialize(list)).apply()
}
