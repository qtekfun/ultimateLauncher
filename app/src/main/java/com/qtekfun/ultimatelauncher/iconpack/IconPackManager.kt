package com.qtekfun.ultimatelauncher.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.util.Log
import com.android.launcher3.LauncherFiles
import com.android.launcher3.graphics.ThemeManager
import com.android.launcher3.icons.PackIcon
import org.xmlpull.v1.XmlPullParser
import kotlin.math.roundToInt

/**
 * Paquetes de iconos de terceros (estándar de launchers: ADW, Nova, Apex). Solo se leen apps ya instaladas en el
 * dispositivo (sin red). Esta clase:
 *  - descubre los packs con `queryIntentActivities` (las acciones están en `<queries>` del manifiesto),
 *  - guarda el pack elegido y el interruptor «fondo del pack para iconos sin tema»,
 *  - da el icono del pack para una actividad ([applyPack], llamado desde `IconProvider.getIcon`, parche 0171),
 *  - y avisa al launcher para invalidar y recargar los iconos ([notifyChanged]).
 * La caché de iconos del launcher distingue packs por [stateToken] (entra en el identificador de frescura).
 */
class IconPackManager private constructor(private val app: Context) {
    class PackInfo(val packageName: String, val label: String)

    private class Loaded(val packageName: String, val version: Long, val res: Resources, val data: AppFilter.Data)

    private val lock = Any()
    private var loaded: Loaded? = null
    private var loadedKey: String? = null

    private fun prefs() = app.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)

    /** Packs instalados (por cualquiera de las acciones conocidas), sin repetir paquete y por orden alfabético. */
    fun installedPacks(): List<PackInfo> {
        val pm = app.packageManager
        val seen = LinkedHashMap<String, PackInfo>()
        for (action in ACTIONS) {
            @Suppress("DEPRECATION")
            val list = runCatching { pm.queryIntentActivities(Intent(action), 0) }.getOrDefault(emptyList())
            for (ri in list) {
                val pkg = ri.activityInfo?.packageName ?: continue
                seen.getOrPut(pkg) { PackInfo(pkg, ri.loadLabel(pm)?.toString()?.ifBlank { null } ?: pkg) }
            }
        }
        return seen.values.sortedBy { it.label.lowercase() }
    }

    /** Paquete del pack elegido si sigue instalado; null = predeterminado. */
    fun selectedPackage(): String? {
        val pkg = prefs().getString(KEY_PACK, "").orEmpty()
        if (pkg.isEmpty()) return null
        return if (installedPacks().any { it.packageName == pkg }) pkg else null
    }

    fun applyBackToUnthemed(): Boolean = prefs().getBoolean(KEY_BACK, true)

    fun setSelected(packageName: String?) {
        prefs().edit().putString(KEY_PACK, packageName.orEmpty()).apply()
        notifyChanged()
    }

    /** Pide al launcher invalidar la caché de iconos y recargar (misma cadena que un cambio de tema: ThemeManager -> IconCache). */
    fun notifyChanged() {
        synchronized(lock) { loaded = null; loadedKey = null; versions.clear() }
        ThemeManager.INSTANCE.get(app).ulNotifyIconSourceChanged()
    }

    /**
     * Texto que entra en el identificador de frescura de la caché de iconos: cambia si cambia el pack, su versión instalada
     * o el interruptor del fondo. Vacío cuando no hay pack (la caché de siempre).
     */
    fun stateToken(): String {
        val pkg = prefs().getString(KEY_PACK, "").orEmpty()
        if (pkg.isEmpty()) return ""
        val version = packageVersion(pkg) ?: return ""
        return "pack:$pkg:$version:${if (applyBackToUnthemed()) 1 else 0}"
    }

    /** Versión instalada del pack (última actualización), memorizada hasta el siguiente [notifyChanged]: evita una llamada al sistema por icono. */
    private val versions = HashMap<String, Long?>()

    private fun packageVersion(pkg: String): Long? = synchronized(lock) {
        if (versions.containsKey(pkg)) versions[pkg]
        else runCatching {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(pkg, 0).lastUpdateTime
        }.getOrNull().also { versions[pkg] = it }
    }

    // ------------------------------------------------------------------ icono de una actividad

    /**
     * Devuelve el icono que debe usar el launcher para `info`: el del pack si lo trae, el original compuesto con
     * fondo/máscara del pack (si el interruptor está activo y el pack trae fondo) o el original.
     */
    fun applyPack(info: ComponentInfo, original: Drawable?, dpi: Int): Drawable? {
        val pack = current() ?: return original
        return try {
            val comp = "${info.packageName}/${info.name}"
            pack.data.icons[comp]?.let { name ->
                loadDrawable(pack, name, dpi)?.let { return wrap(it) }
            }
            if (original == null || !applyBackToUnthemed() || pack.data.backFor(info.packageName) == null) original
            else compose(pack, info.packageName, original, dpi) ?: original
        } catch (e: Exception) {
            Log.w(TAG, "icono del pack ${pack.packageName} para ${info.packageName}: ${e.message}")
            original
        }
    }

    private fun current(): Loaded? {
        val pkg = prefs().getString(KEY_PACK, "").orEmpty()
        if (pkg.isEmpty()) return null
        val version = packageVersion(pkg) ?: return null
        val key = "$pkg:$version"
        synchronized(lock) {
            if (loadedKey == key) return loaded
            loaded = load(pkg, version)
            loadedKey = key
            return loaded
        }
    }

    private fun load(pkg: String, version: Long): Loaded? = try {
        val res = app.packageManager.getResourcesForApplication(pkg)
        var data: AppFilter.Data? = null
        // 1) assets/appfilter.xml (texto)  2) res/xml/appfilter (XML compilado)
        runCatching {
            res.assets.open("appfilter.xml").use { ins ->
                val bytes = ins.readNBytes(AppFilter.MAX_BYTES + 1)
                data = AppFilter.parseText(String(bytes, Charsets.UTF_8))
            }
        }
        if (data == null || data!!.isEmpty) {
            val id = res.getIdentifier("appfilter", "xml", pkg)
            if (id != 0) runCatching { data = parseCompiled(res, id) }
        }
        data?.takeIf { !it.isEmpty }?.let { Loaded(pkg, version, res, it) }
    } catch (e: Exception) {
        Log.w(TAG, "no se pudo leer el pack $pkg: ${e.message}")
        null
    }

    private fun parseCompiled(res: Resources, id: Int): AppFilter.Data {
        res.getXml(id).use { p ->
            val tags = ArrayList<AppFilter.Tag>()
            var ev = p.eventType
            var guard = 0
            while (ev != XmlPullParser.END_DOCUMENT && guard++ < AppFilter.MAX_ITEMS * 4) {
                if (ev == XmlPullParser.START_TAG) {
                    val attrs = LinkedHashMap<String, String>()
                    for (i in 0 until minOf(p.attributeCount, 32)) {
                        var v = p.getAttributeValue(i) ?: continue
                        val rid = p.getAttributeResourceValue(i, 0)
                        if (rid != 0 && v.startsWith("@")) v = runCatching { res.getResourceEntryName(rid) }.getOrDefault(v)
                        if (v.length <= AppFilter.MAX_ATTR_LEN) attrs[p.getAttributeName(i).lowercase()] = v
                    }
                    tags.add(AppFilter.Tag(p.name.lowercase(), attrs))
                }
                ev = p.next()
            }
            return AppFilter.parseTags(tags.iterator())
        }
    }

    private fun loadDrawable(pack: Loaded, name: String, dpi: Int): Drawable? {
        val res = pack.res
        var id = res.getIdentifier(name, "drawable", pack.packageName)
        if (id == 0) id = res.getIdentifier(name, "mipmap", pack.packageName)
        if (id == 0) return null
        return try { res.getDrawableForDensity(id, dpi, null) } catch (e: Resources.NotFoundException) { null }
    }

    /** Los iconos que no son adaptativos se marcan para que la fábrica de iconos no los envuelva si ya traen su propia forma. */
    private fun wrap(d: Drawable): Drawable = if (d is AdaptiveIconDrawable) d else PackIconDrawable(d)

    // ------------------------------------------------------------------ composición para apps sin icono en el pack

    /** Lado de la composición: suficiente para cualquier icono del launcher y barato en memoria (256x256x4 = 256 KB). */
    private val side = 256

    private fun compose(pack: Loaded, pkg: String, original: Drawable, dpi: Int): Drawable? {
        val d = pack.data
        val backName = d.backFor(pkg) ?: return null
        val back = loadDrawable(pack, backName, dpi)
        val mask = d.mask?.let { loadDrawable(pack, it, dpi) }
        val upon = d.upon?.let { loadDrawable(pack, it, dpi) }
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        back?.let { draw(it, c) }
        // Capa del icono original, a la escala del pack, recortada por la máscara (DST_OUT: se quita donde la máscara es opaca).
        val layer = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        drawOriginal(original, lc, d.scale, hasMask = mask != null)
        if (mask != null) {
            val p = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
            val m = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            draw(mask, Canvas(m))
            lc.drawBitmap(m, 0f, 0f, p)
            m.recycle()
        }
        c.drawBitmap(layer, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        layer.recycle()
        upon?.let { draw(it, c) }
        return PackIconDrawable(BitmapDrawable(app.resources, out))
    }

    /** Dibuja `d` llenando todo el lienzo de la composición. */
    private fun draw(d: Drawable, c: Canvas) {
        val old = d.copyBounds()
        d.setBounds(0, 0, side, side)
        d.draw(c)
        d.bounds = old
    }

    /**
     * Dibuja el icono original a `scale` del lado. Un adaptativo se dibuja sin la forma del sistema (capas completas
     * recortadas al cuadrado visible) cuando el pack trae máscara, para que la máscara del pack decida la forma;
     * sin máscara se dibuja tal cual (con la forma del sistema).
     */
    private fun drawOriginal(original: Drawable, c: Canvas, scale: Float, hasMask: Boolean) {
        val s = side * scale
        val o = (side - s) / 2f
        val old = original.copyBounds()
        if (original is AdaptiveIconDrawable && hasMask) {
            val big = s * 1.5f // el área visible (72 dp) es 2/3 del lienzo de 108 dp
            val bo = (side - big) / 2f
            c.save()
            c.clipRect(o, o, o + s, o + s)
            for (layer in listOfNotNull(original.background, original.foreground)) {
                val lb = layer.copyBounds()
                layer.setBounds(bo.roundToInt(), bo.roundToInt(), (bo + big).roundToInt(), (bo + big).roundToInt())
                layer.draw(c)
                layer.bounds = lb
            }
            c.restore()
        } else {
            original.setBounds(o.roundToInt(), o.roundToInt(), (o + s).roundToInt(), (o + s).roundToInt())
            original.draw(c)
        }
        original.bounds = old
    }

    /** Icono de pack ya con forma propia: la fábrica de iconos lo trata como [PackIcon] (parche 0171). */
    class PackIconDrawable(d: Drawable) : DrawableWrapper(d), PackIcon

    companion object {
        const val KEY_PACK = "pref_ul_icon_pack"
        const val KEY_BACK = "pref_ul_icon_pack_back"
        private const val TAG = "UlIconPack"

        /** Acciones estándar con las que un pack se anuncia (todas declaradas en `<queries>`, no son permisos). */
        val ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
            "com.anddoes.launcher.THEME",
        )

        @Volatile private var instance: IconPackManager? = null

        @JvmStatic
        fun get(context: Context): IconPackManager =
            instance ?: synchronized(this) { instance ?: IconPackManager(context.applicationContext).also { instance = it } }
    }
}
