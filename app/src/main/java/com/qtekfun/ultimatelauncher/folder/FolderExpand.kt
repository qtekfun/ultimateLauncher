package com.qtekfun.ultimatelauncher.folder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.android.launcher3.CellLayout
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherFiles
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_FOLDER
import com.android.launcher3.R
import com.android.launcher3.celllayout.CellLayoutLayoutParams
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.FastBitmapDrawable
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.popup.PopupCategory
import com.android.launcher3.popup.PopupData
import com.android.launcher3.touch.ItemClickHandler
import com.qtekfun.ultimatelauncher.folder.FolderExpandLogic as Logic
import java.util.WeakHashMap

/**
 * Carpetas ampliables del escritorio (parches 0140–0143): una carpeta puede ocupar 2x2 celdas y mostrar sus apps como
 * iconos sobre la baldosa de cristal de la carpeta cerrada; un toque en un icono lanza la app, en el resto abre la
 * carpeta. El tamaño vive en spanX/spanY de la fila de la carpeta (columnas ya existentes de `favorites`).
 * La lógica sin Android está en [FolderExpandLogic].
 */
object FolderExpand {
    const val KEY = "pref_ul_folder_expand"

    private const val GAP_DP = 4f
    private const val SIDE_PAD_DP = 2f
    private const val TOP_PAD_DP = 2f
    /** Tamaño objetivo del icono en la baldosa: 0,8 veces el del escritorio. */
    private const val ICON_SCALE = 0.8f

    /** Última posición táctil sobre cada carpeta (coordenadas de la vista); NaN si el clic no viene de un toque. */
    private class Touch(var x: Float = Float.NaN, var y: Float = Float.NaN)
    private val touches = WeakHashMap<FolderIcon, Touch>()

    /** Iconos ya creados por carpeta; se rehacen si cambia el bitmap de la app (icono actualizado). */
    private class Cached(val bitmap: BitmapInfo, val drawable: FastBitmapDrawable)
    private val icons = WeakHashMap<FolderIcon, MutableMap<ItemInfo, Cached>>()

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val paintChip = Paint(Paint.ANTI_ALIAS_FLAG)

    @JvmStatic fun enabled(context: Context): Boolean = try {
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE).getBoolean(KEY, true)
    } catch (e: Exception) { true }

    /**
     * ¿El dedo está demasiado lejos para soltar en la carpeta de la celda de destino? Con una carpeta ampliada debajo
     * vale cualquier punto de su baldosa (parche 0144); con una normal, el radio estándar de AOSP.
     */
    @JvmStatic fun beyondFolderRadius(target: CellLayout, cell: IntArray, distance: Float): Boolean {
        val v = target.getChildAt(cell[0], cell[1])
        if (v is FolderIcon && isExpanded(v)) return false
        return distance > target.getFolderCreationRadius(cell)
    }

    @JvmStatic fun isExpanded(icon: FolderIcon): Boolean =
        icon.mInfo != null && icon.mInfo.spanX >= Logic.EXPANDED_SPAN && icon.mInfo.spanY >= Logic.EXPANDED_SPAN

    // ---- Modelo: carga desde la base de datos (parche 0140) ----

    /** Tamaño con el que se carga la carpeta: ver [FolderExpandLogic.sanitizedSpan]. */
    @JvmStatic fun loadedSpan(spanX: Int, spanY: Int, cellX: Int, cellY: Int, container: Int, cols: Int, rows: Int): Int =
        Logic.sanitizedSpan(spanX, spanY, cellX, cellY, container == CONTAINER_DESKTOP, cols, rows)

    // ---- Geometría (parche 0141) ----

    private fun labelHeight(icon: FolderIcon): Int {
        val fm = icon.folderName.paint.fontMetrics
        return Math.ceil((fm.bottom - fm.top).toDouble()).toInt()
    }

    private fun tileFor(icon: FolderIcon, w: Int, h: Int): Logic.Tile {
        val d = icon.resources.displayMetrics.density
        val cw = w - icon.paddingLeft - icon.paddingRight
        val t = Logic.tileFor(cw, h - icon.paddingTop - icon.paddingBottom, labelHeight(icon), (GAP_DP * d).toInt(),
            (SIDE_PAD_DP * d).toInt(), (TOP_PAD_DP * d).toInt())
        return Logic.Tile(t.left + icon.paddingLeft, t.top + icon.paddingTop, t.side)
    }

    /** Llamado desde PreviewBackground.setup: devuelve [lado, x, y] de la baldosa o null si la carpeta es 1x1. */
    @JvmStatic fun backgroundTile(owner: Any?, width: Int): IntArray? {
        val icon = owner as? FolderIcon ?: return null
        if (!isExpanded(icon) || icon.measuredHeight <= 0) return null
        val t = tileFor(icon, icon.measuredWidth, icon.measuredHeight)
        return intArrayOf(t.side, t.left, t.top)
    }

    /** Llamado desde FolderIcon.onMeasure: coloca el nombre bajo la baldosa (o lo devuelve a su sitio en 1x1). */
    @JvmStatic fun layoutLabel(icon: FolderIcon, widthSpec: Int, heightSpec: Int) {
        val name = icon.folderName ?: return
        val lp = name.layoutParams as? FrameLayout.LayoutParams ?: return
        val grid = Launcher.getLauncher(icon.context).deviceProfile.workspaceProfile
        val wanted = if (isExpanded(icon)) {
            val t = tileFor(icon, View.MeasureSpec.getSize(widthSpec), View.MeasureSpec.getSize(heightSpec))
            t.bottom + (GAP_DP * icon.resources.displayMetrics.density).toInt() - icon.paddingTop
        } else grid.iconSizePx + grid.iconDrawablePaddingPx
        if (lp.topMargin != wanted) lp.topMargin = wanted
    }

    /** Relleno superior de la celda: 0 en carpetas ampliadas (la baldosa ya ocupa la celda entera), igual si no. */
    @JvmStatic fun cellPaddingY(child: View, original: Int): Int =
        if (child is FolderIcon && isExpanded(child)) 0 else original

    /** Límites de la baldosa para el arrastre y las animaciones; false si la carpeta es 1x1. */
    @JvmStatic fun dotBounds(icon: FolderIcon, out: Rect): Boolean {
        if (!isExpanded(icon) || icon.measuredHeight <= 0) return false
        val t = tileFor(icon, icon.measuredWidth, icon.measuredHeight)
        out.set(t.left, t.top, t.right, t.bottom)
        return true
    }

    // ---- Dibujo (parche 0141) ----

    private fun sortedContents(icon: FolderIcon): List<ItemInfo> = icon.mInfo.getContents().sortedBy { it.rank }

    private fun iconFor(icon: FolderIcon, item: ItemInfo): FastBitmapDrawable? {
        val wii = item as? WorkspaceItemInfo ?: return null
        val map = icons.getOrPut(icon) { HashMap() }
        val c = map[item]
        if (c != null && c.bitmap === wii.bitmap) return c.drawable
        val d = wii.newIcon(icon.context, BitmapInfo.FLAG_THEMED)
        d.callback = icon
        map[item] = Cached(wii.bitmap, d)
        return d
    }

    /** Descarta los iconos en caché (cambia el contenido o el tema). */
    @JvmStatic fun invalidateIcons(icon: FolderIcon) { icons.remove(icon) }

    /** Dibuja las apps sobre la baldosa ya pintada. [bounds] es el rectángulo de la baldosa. */
    @JvmStatic fun drawIcons(icon: FolderIcon, canvas: Canvas, bounds: Rect) {
        val items = sortedContents(icon)
        val plan = Logic.slotPlan(items.size)
        val tile = Logic.Tile(bounds.left, bounds.top, bounds.width())
        val cells = Logic.slotCells(plan, tile)
        val grid = Launcher.getLauncher(icon.context).deviceProfile.workspaceProfile
        val maxIcon = (grid.iconSizePx * ICON_SCALE).toInt()
        for (i in 0 until plan.shown) {
            val d = iconFor(icon, items[i]) ?: continue
            val r = Logic.iconRect(cells[i], maxIcon)
            d.setBounds(r.left, r.top, r.right, r.bottom)
            d.draw(canvas)
        }
        if (plan.overflow > 0) {
            val r = Logic.iconRect(cells[plan.shown], maxIcon)
            paintChip.color = 0x40FFFFFF
            val rf = RectF(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
            canvas.drawRoundRect(rf, r.width * 0.3f, r.width * 0.3f, paintChip)
            paintText.color = Color.WHITE
            paintText.textSize = r.width * 0.38f
            canvas.drawText("+${plan.overflow}", rf.centerX(), rf.centerY() - (paintText.ascent() + paintText.descent()) / 2f, paintText)
        }
    }

    // ---- Toques (parche 0141 y 0142) ----

    /** Guarda dónde se tocó: el clic llega después sin coordenadas. */
    @JvmStatic fun recordTouch(icon: FolderIcon, ev: MotionEvent) {
        val t = touches.getOrPut(icon) { Touch() }
        if (ev.actionMasked == MotionEvent.ACTION_CANCEL) { t.x = Float.NaN; t.y = Float.NaN } else { t.x = ev.x; t.y = ev.y }
    }

    /** Índice de la app tocada (0-based), [FolderExpandLogic.OVERFLOW] o [FolderExpandLogic.NONE]. */
    private fun hit(icon: FolderIcon, items: List<ItemInfo>): Int {
        val t = touches[icon] ?: return Logic.NONE
        if (t.x.isNaN() || icon.measuredHeight <= 0) return Logic.NONE
        val tile = tileFor(icon, icon.measuredWidth, icon.measuredHeight)
        val plan = Logic.slotPlan(items.size)
        return Logic.hitTest(plan, Logic.slotCells(plan, tile), t.x, t.y)
    }

    /**
     * Clic en una carpeta (ItemClickHandler.onClickFolderIcon): si está ampliada y el toque cayó en una app, la lanza y
     * devuelve true; en cualquier otro caso devuelve false y la carpeta se abre como siempre.
     */
    @JvmStatic fun onFolderClick(icon: FolderIcon): Boolean {
        try {
            if (!isExpanded(icon)) return false
            val items = sortedContents(icon)
            val i = hit(icon, items)
            val item = items.getOrNull(i) as? WorkspaceItemInfo ?: return false
            val launcher = Launcher.getLauncher(icon.context)
            ItemClickHandler.onClickAppShortcut(icon, item, launcher)
            return true
        } finally {
            touches[icon]?.let { it.x = Float.NaN; it.y = Float.NaN }
        }
    }

    // ---- Menú de pulsación larga (parche 0142) ----

    private fun canExpand(item: ItemInfo): Boolean =
        item.container == CONTAINER_DESKTOP && item.spanX == 1 && item.spanY == 1

    private val expandData = PopupData(R.drawable.ul_ic_folder_expand, R.string.ul_folder_expand, PopupCategory.SYSTEM_SHORTCUT_FIXED) { ctx, info, view ->
        com.android.launcher3.AbstractFloatingView.closeAllOpenViews(ctx)
        setExpanded(view, info, true)
    }
    private val shrinkData = PopupData(R.drawable.ul_ic_folder_shrink, R.string.ul_folder_shrink, PopupCategory.SYSTEM_SHORTCUT_FIXED) { ctx, info, view ->
        com.android.launcher3.AbstractFloatingView.closeAllOpenViews(ctx)
        setExpanded(view, info, false)
    }

    /** Entradas extra del menú de una carpeta: «Ampliar carpeta» o «Reducir carpeta» (el menú solo se abre con el ajuste activo, ver [wantsPopup]). */
    @JvmStatic fun popupData(item: ItemInfo): List<PopupData> {
        if (item.itemType != ITEM_TYPE_FOLDER || item.container != CONTAINER_DESKTOP) return emptyList()
        return when {
            item.spanX >= Logic.EXPANDED_SPAN -> listOf(shrinkData)
            canExpand(item) -> listOf(expandData)
            else -> emptyList()
        }
    }

    /** Solo para el Workspace: ¿se abre el menú de pulsación larga sobre esta vista? (carpeta del escritorio) */
    @JvmStatic fun wantsPopup(child: View): Boolean {
        val item = child.tag as? FolderInfo ?: return false
        return child is FolderIcon && item.container == CONTAINER_DESKTOP &&
            (enabled(child.context) || item.spanX >= Logic.EXPANDED_SPAN)
    }

    // ---- Cambio de tamaño ----

    /** Ocupación de la hoja sin contar la vista [self]. */
    private fun occupiedExcluding(cl: CellLayout, self: View): Array<BooleanArray> {
        val occ = Array(cl.countX) { BooleanArray(cl.countY) }
        val c = cl.shortcutsAndWidgets
        for (i in 0 until c.childCount) {
            val v = c.getChildAt(i)
            if (v === self) continue
            val lp = v.layoutParams as? CellLayoutLayoutParams ?: continue
            for (x in lp.cellX until minOf(lp.cellX + lp.cellHSpan, cl.countX)) for (y in lp.cellY until minOf(lp.cellY + lp.cellVSpan, cl.countY)) {
                if (x >= 0 && y >= 0) occ[x][y] = true
            }
        }
        return occ
    }

    /** Ampliar a 2x2 o reducir a 1x1 una carpeta del escritorio, reubicando lo que estorbe. */
    @JvmStatic fun setExpanded(view: View, info: ItemInfo, expand: Boolean) {
        val launcher = Launcher.getLauncher(view.context)
        val cl = launcher.getCellLayout(info.container, info.screenId) ?: return
        val lp = view.layoutParams as? CellLayoutLayoutParams ?: return
        val writer = launcher.modelWriter
        if (!expand) {
            cl.markCellsAsUnoccupiedForView(view)
            lp.cellHSpan = 1; lp.cellVSpan = 1
            cl.markCellsAsOccupiedForView(view)
            writer.modifyItemInDatabase(info, info.container, info.screenId, lp.cellX, lp.cellY, 1, 1)
            relayout(view, cl)
            return
        }
        val cols = cl.countX
        val rows = cl.countY
        val occ = occupiedExcluding(cl, view)
        val free = Logic.findExpandAnchor(cols, rows, lp.cellX, lp.cellY) { x, y -> occ[x][y] }
        cl.markCellsAsUnoccupiedForView(view)
        if (free != null) {
            place(view, info, cl, free[0], free[1])
            return
        }
        // Sin hueco libre: mismo reordenado que al redimensionar un widget (los vecinos se desplazan o no cabe).
        val a = Logic.clampAnchor(cols, rows, lp.cellX, lp.cellY)
        lp.cellHSpan = Logic.EXPANDED_SPAN; lp.cellVSpan = Logic.EXPANDED_SPAN
        lp.tmpCellX = a[0]; lp.tmpCellY = a[1]
        val dir = intArrayOf(1, 1)  // empuja hacia derecha y abajo, como el marco de widgets
        val ok = cl.createAreaForResize(a[0], a[1], Logic.EXPANDED_SPAN, Logic.EXPANDED_SPAN, view, dir, true)
        if (ok) {
            lp.cellX = a[0]; lp.cellY = a[1]
            cl.markCellsAsOccupiedForView(view)
            writer.modifyItemInDatabase(info, info.container, info.screenId, a[0], a[1], Logic.EXPANDED_SPAN, Logic.EXPANDED_SPAN)
            relayout(view, cl)
        } else {
            lp.cellHSpan = 1; lp.cellVSpan = 1
            lp.tmpCellX = lp.cellX; lp.tmpCellY = lp.cellY
            cl.markCellsAsOccupiedForView(view)
            Toast.makeText(view.context, R.string.ul_folder_no_room, Toast.LENGTH_SHORT).show()
        }
    }

    private fun place(view: View, info: ItemInfo, cl: CellLayout, ax: Int, ay: Int) {
        val lp = view.layoutParams as CellLayoutLayoutParams
        lp.cellX = ax; lp.cellY = ay
        lp.tmpCellX = ax; lp.tmpCellY = ay
        lp.cellHSpan = Logic.EXPANDED_SPAN; lp.cellVSpan = Logic.EXPANDED_SPAN
        cl.markCellsAsOccupiedForView(view)
        Launcher.getLauncher(view.context).modelWriter.modifyItemInDatabase(
            info, info.container, info.screenId, ax, ay, Logic.EXPANDED_SPAN, Logic.EXPANDED_SPAN)
        relayout(view, cl)
    }

    private fun relayout(view: View, cl: CellLayout) {
        (view as? FolderIcon)?.let { invalidateIcons(it); it.onItemsChanged(false) }
        view.requestLayout()
        cl.shortcutsAndWidgets.requestLayout()
        cl.invalidate()
    }
}
