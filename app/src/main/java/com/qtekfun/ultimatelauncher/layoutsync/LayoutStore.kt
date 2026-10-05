package com.qtekfun.ultimatelauncher.layoutsync

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherFiles
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.model.data.LauncherAppWidgetInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Lee y escribe la tabla `favorites` de Launcher3 (misma base de datos que usa el modelo) y fuerza una recarga. */
class LayoutStore(private val context: Context) {
    private val state get() = LauncherAppState.getInstance(context)
    private val idp get() = LauncherAppState.getIDP(context)
    private val launcherApps get() = context.getSystemService(LauncherApps::class.java)

    /** Llamar fuera del hilo principal. */
    fun export(): Snapshot {
        val ownSerial = android.os.UserManager::class.java.let { context.getSystemService(it).getSerialNumberForUser(Process.myUserHandle()) }
        data class Row(val id: Int, val type: Int, val container: Int, val screen: Int, val cx: Int, val cy: Int, val sx: Int, val sy: Int,
                       val intent: String?, val title: String?, val provider: String?, val profile: Long, val rank: Int)
        val rows = mutableListOf<Row>()
        state.model.modelDbController.query(null, null, null, null).use { c ->
            fun i(n: String) = c.getColumnIndexOrThrow(n)
            while (c.moveToNext()) rows += Row(c.getInt(i(Favorites._ID)), c.getInt(i(Favorites.ITEM_TYPE)), c.getInt(i(Favorites.CONTAINER)),
                c.getInt(i(Favorites.SCREEN)), c.getInt(i(Favorites.CELLX)), c.getInt(i(Favorites.CELLY)), c.getInt(i(Favorites.SPANX)),
                c.getInt(i(Favorites.SPANY)), c.getString(i(Favorites.INTENT)), c.getString(i(Favorites.TITLE)),
                c.getString(i(Favorites.APPWIDGET_PROVIDER)), c.getLong(i(Favorites.PROFILE_ID)), c.getInt(i(Favorites.RANK)))
        }
        fun app(r: Row, cell: Cell?): Item.App? {
            val cn = r.intent?.let { runCatching { Intent.parseUri(it, 0).component }.getOrNull() } ?: return null
            return Item.App(cn.packageName, cn.className, if (r.profile == ownSerial) "personal" else "work", cell)
        }
        fun item(r: Row): Item? = when (r.type) {
            Favorites.ITEM_TYPE_APPLICATION -> app(r, Cell(r.cx, r.cy))
            Favorites.ITEM_TYPE_FOLDER -> Item.Folder(r.title.orEmpty(), rows.filter { it.container == r.id && it.type == Favorites.ITEM_TYPE_APPLICATION }
                .sortedBy { it.rank }.mapNotNull { app(it, null) }, Cell(r.cx, r.cy))
            Favorites.ITEM_TYPE_APPWIDGET -> r.provider?.let { Item.Widget(it, Span(r.sx, r.sy), Cell(r.cx, r.cy)) }
            Favorites.ITEM_TYPE_SHORTCUT, Favorites.ITEM_TYPE_DEEP_SHORTCUT -> {
                val intent = r.intent?.let { runCatching { Intent.parseUri(it, 0) }.getOrNull() }
                intent?.let { Item.Shortcut(it.`package` ?: it.component?.packageName ?: "?", it.getStringExtra("shortcut_id") ?: "", Cell(r.cx, r.cy)) }
            }
            else -> null
        }
        val hot = rows.filter { it.container == Favorites.CONTAINER_HOTSEAT }.mapNotNull { r -> item(r)?.let { r.screen to it.at(null) } }
        val pages = rows.filter { it.container == Favorites.CONTAINER_DESKTOP }.groupBy { it.screen }.toSortedMap().values
            .mapIndexed { idx, rs -> Page(idx, rs.mapNotNull { item(it) }) }
        val dm = context.resources.displayMetrics
        val sw = context.resources.configuration.smallestScreenWidthDp
        return Snapshot(now(), Device(if (sw >= 600) "tablet" else "phone", Build.BRAND, Build.MODEL, Build.VERSION.SDK_INT, dm.densityDpi,
            context.resources.configuration.screenWidthDp, context.resources.configuration.screenHeightDp),
            Settings(Grid(idp.numColumns, idp.numRows), if (sw >= 600) "tablet" else "phone"), hot, pages, BackupPrefs.filter(launcherPrefs().all))
    }

    private fun launcherPrefs() = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    /** Escribe los ajustes ya filtrados con `commit()` (síncrono: el modelo y el launcher los leen al recargar). */
    private fun applyPrefs(prefs: Map<String, Any>) {
        val ed = launcherPrefs().edit()
        for ((k, v) in BackupPrefs.filter(prefs)) when (v) {
            is Boolean -> ed.putBoolean(k, v); is Int -> ed.putInt(k, v); is String -> ed.putString(k, v)
        }
        ed.commit()
    }

    /** Apps lanzables del perfil personal (sin esta app), con su fecha de instalación, para «Colocar mis apps por orden». */
    fun installedApps(): List<AppEntry> = launcherApps.getActivityList(null, Process.myUserHandle())
        .filter { it.componentName.packageName != context.packageName }
        .map { AppEntry(it.componentName.packageName, it.componentName.className, it.label.toString(), it.firstInstallTime) }

    /** Dock actual (hueco → elemento) para conservarlo al colocar las apps por orden. */
    fun currentHotseat(): List<Pair<Int, Item>> = export().hotseat

    fun deviceState(): DeviceState {
        val launchable = launcherApps.getActivityList(null, Process.myUserHandle()).groupBy({ it.componentName.packageName }, { it.componentName.className })
        val providers = AppWidgetManager.getInstance(context).installedProviders.map { it.provider.flattenToString() }.toSet()
        return DeviceState(launchable, providers, Grid(idp.numColumns, idp.numRows), idp.numDatabaseHotseatIcons, false)
    }

    /** Copia automática del layout actual y reemplazo por el plan. Llamar fuera del hilo principal. */
    fun apply(plan: ImportPlan): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val backup = File(dir, "layout-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json")
        backup.writeText(LayoutJson.toJson(export()))
        if (plan.prefs.isNotEmpty()) applyPrefs(plan.prefs)
        val db = state.model.modelDbController
        val ownSerial = context.getSystemService(android.os.UserManager::class.java).getSerialNumberForUser(Process.myUserHandle())
        db.delete(null, null)
        fun label(a: Item.App) = runCatching { launcherApps.getActivityList(a.pkg, Process.myUserHandle()).firstOrNull()?.label?.toString() }.getOrNull() ?: a.pkg
        fun appValues(a: Item.App, container: Int, screen: Int, x: Int, y: Int, rank: Int) = ContentValues().apply {
            put(Favorites._ID, db.generateNewItemId()); put(Favorites.TITLE, label(a))
            put(Favorites.INTENT, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(a.pkg, a.activity)).setFlags(0x10200000).toUri(0))
            put(Favorites.CONTAINER, container); put(Favorites.SCREEN, screen); put(Favorites.CELLX, x); put(Favorites.CELLY, y)
            put(Favorites.SPANX, 1); put(Favorites.SPANY, 1); put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPLICATION)
            put(Favorites.PROFILE_ID, ownSerial); put(Favorites.RANK, rank); put(Favorites.RESTORED, 0)
        }
        // Widgets sin permiso nuevo: se escribe la fila con appWidgetId=-1 y FLAG_ID_NOT_VALID|FLAG_PROVIDER_NOT_READY, igual que
        // AutoInstallsLayout. Al recargar, WidgetInflater intenta reservar el id y enlazarlo; si no puede (sin BIND_APPWIDGET)
        // lo deja como widget pendiente y el toque lanza el diálogo de enlace del sistema. No se copian ni datos ni configuración.
        fun widgetValues(w: Item.Widget, screen: Int, c: Cell) = ContentValues().apply {
            put(Favorites._ID, db.generateNewItemId()); put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP)
            put(Favorites.SCREEN, screen); put(Favorites.CELLX, c.x); put(Favorites.CELLY, c.y)
            put(Favorites.SPANX, w.span.w); put(Favorites.SPANY, w.span.h); put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPWIDGET)
            put(Favorites.APPWIDGET_PROVIDER, w.provider); put(Favorites.APPWIDGET_ID, -1)
            put(Favorites.PROFILE_ID, ownSerial); put(Favorites.RANK, 0)
            put(Favorites.RESTORED, LauncherAppWidgetInfo.FLAG_ID_NOT_VALID or LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)
        }
        fun insertFolder(f: Item.Folder, container: Int, screen: Int, x: Int, y: Int) {
            val fid = db.generateNewItemId()
            db.insert(ContentValues().apply {
                put(Favorites._ID, fid); put(Favorites.TITLE, f.title); put(Favorites.CONTAINER, container)
                put(Favorites.SCREEN, screen); put(Favorites.CELLX, x); put(Favorites.CELLY, y); put(Favorites.SPANX, 1); put(Favorites.SPANY, 1)
                put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_FOLDER); put(Favorites.PROFILE_ID, ownSerial); put(Favorites.RANK, 0); put(Favorites.RESTORED, 0)
            })
            f.items.forEachIndexed { rank, a -> db.insert(appValues(a, fid, 0, rank % 3, rank / 3, rank)) }
        }
        plan.hotseat.forEach { (slot, it) -> when (it) {
            is Item.App -> db.insert(appValues(it, Favorites.CONTAINER_HOTSEAT, slot, slot, 0, 0))
            is Item.Folder -> insertFolder(it, Favorites.CONTAINER_HOTSEAT, slot, slot, 0)
            else -> Unit
        } }
        plan.pages.forEachIndexed { screen, items -> items.forEach { it ->
            val c = it.cell ?: Cell(0, 0)
            when (it) {
                is Item.App -> db.insert(appValues(it, Favorites.CONTAINER_DESKTOP, screen, c.x, c.y, 0))
                is Item.Folder -> insertFolder(it, Favorites.CONTAINER_DESKTOP, screen, c.x, c.y)
                is Item.Widget -> db.insert(widgetValues(it, screen, c))
                else -> Unit // accesos directos: no se restauran en v1 (se avisa en el resumen)
            }
        } }
        state.model.forceReload("layoutsync")
        // Reconstruye los perfiles de dispositivo para que «iconos hasta el borde» y similares se relean (mismo gancho que Ajustes).
        if (plan.prefs.isNotEmpty()) idp.ulReloadGrid()
        return backup
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
}
