// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.dock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Process
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.LauncherSettings.Favorites
import com.android.launcher3.graphics.ThemeManager
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.settings.SettingsActivity
import com.android.launcher3.icons.LauncherIcons
import com.qtekfun.ultimatelauncher.ui.ContextMenuStyle

/**
 * Dock de tablet estilo Huawei: dos píldoras (apps fijas del hotseat | asa | últimas apps usadas).
 * La zona fija sigue siendo el Hotseat de Launcher3 (con su base de datos y arrastre); esta vista va DEBAJO del Hotseat,
 * dibuja las píldoras y el asa, y aloja los iconos de recientes. Solo se crea si R.bool.ul_huawei_dock (tablets).
 */
class UlDockView(private val launcher: Launcher) : FrameLayout(launcher) {
    private val res = resources
    private val cell = res.getDimension(R.dimen.ul_dock_cell)
    private val pad = res.getDimension(R.dimen.ul_dock_pill_padding)
    private val pillH = res.getDimension(R.dimen.ul_dock_pill_height)
    private val radius = res.getDimension(R.dimen.ul_dock_pill_radius)
    private val gap = res.getDimension(R.dimen.ul_dock_gap)
    private val iconPx = res.getDimension(R.dimen.ul_dock_icon).toInt()
    private val bottomMargin = res.getDimension(R.dimen.ul_dock_bottom_margin)
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leftRect = RectF()
    private val rightRect = RectF()
    private val handleRect = RectF()
    /** Zona táctil del asa (más ancha que la barra dibujada). Coordenadas locales de esta vista. */
    private val handleHit = RectF()
    /** Vista invisible colocada sobre el asa: sirve de ancla del menú contextual (no recibe toques). */
    private val handleAnchor = View(launcher).apply { isClickable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    private var recents: List<ComponentName> = emptyList()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == RecentApps.CHANGED_KEY) post { refresh() }
    }
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == DockPrefs.KEY_BACKGROUND || key == DockPrefs.KEY_SUBTLE || key == DockPrefs.KEY_RECENTS) post {
            if (key == DockPrefs.KEY_RECENTS && !DockPrefs.recentsEnabled(context)) RecentApps.clear(context)
            refresh()
        }
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        addView(handleAnchor, LayoutParams(1, 1))
    }

    // --- Asa: toque o pulsación larga -> menú contextual (estilo unificado) ---

    private val handleGestures = GestureDetector(launcher, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = handleHit.contains(e.x, e.y)
        override fun onSingleTapUp(e: MotionEvent): Boolean { showHandleMenu(); return true }
        override fun onLongPress(e: MotionEvent) { performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); showHandleMenu() }
    })

    // Solo se queda los toques que caen en el asa; el resto sigue su camino como antes.
    override fun onTouchEvent(event: MotionEvent): Boolean = handleGestures.onTouchEvent(event)

    /** Menú del asa: Ajustes del dock, Borrar los recientes (si hay) y Ocultar/Mostrar recientes. */
    private fun showHandleMenu() {
        val enabled = DockPrefs.recentsEnabled(context)
        val rows = ArrayList<ContextMenuStyle.Row>()
        rows += ContextMenuStyle.Row(R.string.ul_dock_menu_settings, false) { openDockSettings() }
        if (enabled && recents.isNotEmpty()) {
            rows += ContextMenuStyle.Row(R.string.ul_dock_menu_clear_recents, true) { RecentApps.clear(context) }
        }
        // Ocultar = desactivar «Últimas apps en el dock» (borra el historial, igual que el interruptor de Ajustes).
        rows += ContextMenuStyle.Row(if (enabled) R.string.ul_dock_menu_hide_recents else R.string.ul_dock_menu_show_recents, false) {
            DockPrefs.prefs(context).edit().putBoolean(DockPrefs.KEY_RECENTS, !enabled).apply()
        }
        ContextMenuStyle.showRows(handleAnchor, rows) { refresh() }
    }

    private fun openDockSettings() {
        context.startActivity(
            Intent(context, SettingsActivity::class.java)
                .putExtra(SettingsActivity.EXTRA_FRAGMENT_HIGHLIGHT_KEY, DockPrefs.KEY_BACKGROUND)
        )
    }

    // --- Compactación de los fijos (el dock de Huawei empaqueta; Launcher3 deja los huecos del arrastre) ---

    private val compactRunnable = Runnable { compactFixed() }
    private var compactRetries = 0

    private fun scheduleCompaction() {
        removeCallbacks(compactRunnable)
        postDelayed(compactRunnable, COMPACT_DELAY_MS)
    }

    /** Vistas de apps/carpetas fijas con su ItemInfo; null si hay algo raro (span > 1, fuera del dock) y no se debe tocar. */
    private fun fixedItems(): List<Triple<View, ItemInfo, com.android.launcher3.celllayout.CellLayoutLayoutParams>>? {
        val container = launcher.hotseat.shortcutsAndWidgets
        val out = ArrayList<Triple<View, ItemInfo, com.android.launcher3.celllayout.CellLayoutLayoutParams>>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is com.android.launcher3.qsb.OseWidgetView) continue // el hueco del buscador vacío no es un fijo
            val info = child.tag as? ItemInfo ?: continue
            val lp = child.layoutParams as? com.android.launcher3.celllayout.CellLayoutLayoutParams ?: continue
            if (info.container != Favorites.CONTAINER_HOTSEAT || lp.cellHSpan != 1 || lp.cellVSpan != 1 || lp.cellY != 0 || lp.useTmpCoords) return null
            out += Triple(child, info, lp)
        }
        return out
    }

    private fun needsCompaction(): Boolean {
        if (dragging || launcher.isWorkspaceLoading || launcher.deviceProfile.isVerticalBarLayout) return false
        val items = fixedItems() ?: return false
        return DockLogic.needsCompaction(items.map { it.third.cellX }, launcher.deviceProfile.hotseatProfile.numShownIcons)
    }

    /**
     * Empaqueta los fijos hacia la izquierda sin cambiar su orden y lo guarda en UNA transacción del modelo
     * (`scheduleTransaction`). No hace nada durante un arrastre, con el espacio de trabajo cargando o con un menú/carpeta
     * abierto (reintenta unas veces).
     */
    private fun compactFixed() {
        if (!isAttachedToWindow || launcher.isWorkspaceLoading || launcher.deviceProfile.isVerticalBarLayout) return
        if (dragging || launcher.dragController.isDragging || AbstractFloatingView.getTopOpenView(launcher) != null) {
            if (compactRetries++ < MAX_COMPACT_RETRIES) postDelayed(compactRunnable, COMPACT_DELAY_MS)
            return
        }
        compactRetries = 0
        val items = fixedItems() ?: return
        val capacity = launcher.deviceProfile.hotseatProfile.numShownIcons
        val cells = items.map { it.third.cellX }
        if (!DockLogic.needsCompaction(cells, capacity)) return
        val targets = DockLogic.compactTargets(cells, capacity)
        val layout = launcher.hotseat
        val moved = ArrayList<ItemInfo>()
        // De izquierda a derecha: cada destino queda siempre libre (los destinos nunca son mayores que el origen).
        for (i in items.indices.sortedBy { cells[it] }) {
            val (view, info, lp) = items[i]
            val target = targets[i]
            if (target == lp.cellX) continue
            if (view is com.android.launcher3.Reorderable) {
                layout.animateChildToPosition(view, target, 0, COMPACT_ANIM_MS, 0, true, true)
            } else {
                layout.markCellsAsUnoccupiedForView(view)
                lp.cellX = target
                layout.markCellsAsOccupiedForView(view)
                view.requestLayout()
            }
            info.cellX = target; info.cellY = 0; info.screenId = target // en el hotseat horizontal screenId = cellX
            moved += info
        }
        if (moved.isEmpty()) return
        launcher.modelWriter.scheduleTransaction<Unit>(null) { tx -> moved.forEach { tx.updateItemInDatabase(it) } }
    }

    /** Rango de huecos ocupados del hotseat (primer y último cellX) para ajustar la píldora aunque haya huecos libres. */
    private fun occupiedRange(): IntArray {
        // Durante un arrastre la píldora abarca TODOS los huecos del hotseat: así se ven (y se pueden usar) los libres,
        // incluidos los que quedan fuera del rango ocupado (p. ej. el hueco izquierdo del buscador).
        if (dragging) return intArrayOf(0, launcher.deviceProfile.hotseatProfile.numShownIcons - 1)
        val container = launcher.hotseat.shortcutsAndWidgets
        val cells = ArrayList<Int>()
        val spans = ArrayList<Int>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            // El hotseat de tablet lleva un hueco del buscador (OseWidgetView, vacío sin GMS): no cuenta como app fija.
            if (child is com.android.launcher3.qsb.OseWidgetView || child.tag !is com.android.launcher3.model.data.ItemInfo) continue
            val lp = child.layoutParams as? com.android.launcher3.celllayout.CellLayoutLayoutParams ?: continue
            cells += lp.cellX; spans += lp.cellHSpan
        }
        return DockLogic.occupiedRange(cells, spans)
    }

    private var dragging = false
    private val dragListener = object : com.android.launcher3.dragndrop.DragController.DragListener {
        override fun onDragStart(dragObject: com.android.launcher3.DropTarget.DragObject, options: com.android.launcher3.dragndrop.DragOptions) = setDragging(true)
        override fun onDragEnd() { setDragging(false); scheduleCompaction() }
    }

    private fun setDragging(value: Boolean) {
        if (dragging == value) return
        dragging = value
        requestLayout(); invalidate()
    }

    private var lastRange = -1
    private val hotseatListener = OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val r = occupiedRange()
        val key = r[0] * 100 + r[1]
        if (key != lastRange) { lastRange = key; post { requestLayout(); invalidate() } }
        // Hueco intermedio tras quitar un icono por otra vía (desinstalar, menú «Eliminar»): empaquetar.
        if (needsCompaction()) scheduleCompaction()
    }

    /** Las píldoras siguen la opacidad del hotseat (se desvanecen con el cajón abierto). */
    private val alphaSync = android.view.ViewTreeObserver.OnPreDrawListener {
        val a = launcher.hotseat.alpha
        if (alpha != a) alpha = a
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(alphaSync)
        launcher.hotseat.addOnLayoutChangeListener(hotseatListener)
        launcher.dragController.addDragListener(dragListener)
        RecentApps.prefs(context).registerOnSharedPreferenceChangeListener(listener)
        DockPrefs.prefs(context).registerOnSharedPreferenceChangeListener(settingsListener)
        refresh()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(compactRunnable)
        viewTreeObserver.removeOnPreDrawListener(alphaSync)
        launcher.hotseat.removeOnLayoutChangeListener(hotseatListener)
        launcher.dragController.removeDragListener(dragListener)
        RecentApps.prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
        DockPrefs.prefs(context).unregisterOnSharedPreferenceChangeListener(settingsListener)
        super.onDetachedFromWindow()
    }

    private fun fixedPackages(): Set<String> {
        val out = HashSet<String>()
        val container = launcher.hotseat.shortcutsAndWidgets
        for (i in 0 until container.childCount) {
            (container.getChildAt(i).tag as? com.android.launcher3.model.data.ItemInfo)?.targetComponent?.packageName?.let { out += it }
        }
        return out
    }

    /** Reconstruye los iconos de recientes (hasta ul_dock_recents_max) sin repetir los de la zona fija. */
    fun refresh() {
        val fixed = fixedPackages()
        val la = context.getSystemService(LauncherApps::class.java)
        val shape = ThemeManager.INSTANCE.get(context).iconShapeData.value
        removeAllViews()
        addView(handleAnchor, LayoutParams(1, 1))
        val shown = ArrayList<ComponentName>()
        // Poda persistente de apps desinstaladas (antes solo se saltaban al pintar y se quedaban guardadas).
        RecentApps.prune(context) { cn -> la.getActivityList(cn.packageName, Process.myUserHandle()).any { it.componentName == cn } }
        val li = LauncherIcons.obtain(context)
        try {
            val wanted = if (DockPrefs.recentsEnabled(context)) RecentApps.load(context) else emptyList()
            for (cn in wanted) {
                if (shown.size >= RecentApps.max(context)) break
                if (cn.packageName in fixed) continue
                val info = la.getActivityList(cn.packageName, Process.myUserHandle()).firstOrNull { it.componentName == cn } ?: continue
                val bmp = li.createBadgedIconBitmap(
                    // Mismo origen e iconDpi que el resto del launcher (IconLoader), no getIcon(0): en EMUI daba otro icono.
                    com.android.launcher3.icons.IconProvider(context).getIcon(info.activityInfo, launcher.deviceProfile.inv.fillResIconDpi))
                val v = ImageView(context).apply {
                    setImageDrawable(bmp.newIcon(context, 0, shape))
                    contentDescription = info.label
                    layoutParams = LayoutParams(iconPx, iconPx)
                    setOnClickListener { la.startMainActivity(cn, Process.myUserHandle(), null, null) }
                    setOnLongClickListener { showRecentMenu(this, cn); true }
                }
                addView(v)
                shown += cn
            }
        } finally {
            li.recycle()
        }
        recents = shown
        requestLayout()
        invalidate()
    }

    /**
     * Pulsación larga en un reciente: menú contextual que sale del propio icono con «Quitar de recientes» y «Borrar todos
     * los recientes». Mismo estilo y comportamiento que el resto de menús del launcher (ver [ContextMenuStyle]).
     */
    private fun showRecentMenu(anchor: View, cn: ComponentName) {
        ContextMenuStyle.showRows(
            anchor,
            listOf(
                ContextMenuStyle.Row(R.string.ul_dock_recent_remove, false) { RecentApps.remove(context, cn) },
                ContextMenuStyle.Row(R.string.ul_dock_recent_clear_all, true) { RecentApps.clear(context) },
            ),
        ) { refresh() } // al instante (el listener de preferencias también lo haría, pero asíncrono)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Coordenadas relativas a esta vista; el hotseat es hermano y ocupa todo el ancho de pantalla (esta vista puede
        // llevar márgenes por muescas/insets), así que se centra respecto al hotseat y no respecto a sí misma.
        // (Esta vista se coloca ANTES que el hotseat: no se puede leer su tamaño; se usan las medidas del perfil.)
        val dp = launcher.deviceProfile
        val w = dp.deviceProperties.widthPx.toFloat()
        val x0 = -l.toFloat()
        val h = (dp.deviceProperties.heightPx - t).toFloat()
        val n = launcher.deviceProfile.hotseatProfile.numShownIcons
        // La píldora izquierda abarca solo las apps que hay (mínimo 1), no todos los huecos del hotseat.
        val range = occupiedRange()
        val first = range[0].coerceIn(0, n - 1)
        val k = (range[1] - range[0] + 1).coerceIn(1, n)
        val leftW = k * cell + 2 * pad
        val rightW = if (recents.isEmpty()) 0f else recents.size * cell + 2 * pad
        // El asa se muestra siempre (sin recientes queda a la derecha de la píldora de fijos) para poder abrir su menú.
        val g = gap
        val total = leftW + g + rightW
        val startX = x0 + (w - total) / 2f
        val bottom = h - bottomMargin
        val top = bottom - pillH
        leftRect.set(startX, top, startX + leftW, bottom)
        rightRect.set(leftRect.right + g, top, leftRect.right + g + rightW, bottom)
        val hw = 4f * res.displayMetrics.density
        val hh = 35f * res.displayMetrics.density
        handleRect.set(leftRect.right + g / 2 - hw / 2, top + (pillH - hh) / 2, leftRect.right + g / 2 + hw / 2, top + (pillH + hh) / 2)
        val hitHalf = maxOf(g / 2f, 22f * res.displayMetrics.density)
        handleHit.set(handleRect.centerX() - hitHalf, top, handleRect.centerX() + hitHalf, bottom)
        handleAnchor.layout(handleRect.left.toInt(), handleRect.top.toInt(), maxOf(handleRect.right.toInt(), handleRect.left.toInt() + 1), handleRect.bottom.toInt())
        // El hotseat se centra solo; se desplaza para que el conjunto (izquierda + asa + derecha) quede centrado.
        launcher.hotseat.translationX = (startX - x0) + pad - (w - n * cell) / 2f - first * cell
        for (i in 1 until childCount) { // el hijo 0 es el ancla del asa
            val v = getChildAt(i)
            val x = (rightRect.left + pad + (i - 1) * cell + (cell - iconPx) / 2).toInt()
            val y = (top + (pillH - iconPx) / 2).toInt()
            v.layout(x, y, x + iconPx, y + iconPx)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val a = DockPrefs.pillAlpha(context)
        if (a == 0) return // «sin fondo»: solo iconos
        pillPaint.color = Color.argb(a, 0xFF, 0xFF, 0xFF)
        handlePaint.color = Color.argb(a * 0x99 / 0xB8, 0xFF, 0xFF, 0xFF)
        canvas.drawRoundRect(leftRect, radius, radius, pillPaint)
        if (recents.isNotEmpty()) canvas.drawRoundRect(rightRect, radius, radius, pillPaint)
        canvas.drawRoundRect(handleRect, handleRect.width() / 2, handleRect.width() / 2, handlePaint)
    }

    companion object {
        private const val COMPACT_DELAY_MS = 600L // deja terminar la animación de soltar antes de empaquetar
        private const val COMPACT_ANIM_MS = 200
        private const val MAX_COMPACT_RETRIES = 5

        /** Gancho desde Launcher.setupViews (parche 0040). Solo en tablets (ul_huawei_dock). */
        @JvmStatic
        fun attach(launcher: Launcher) {
            if (!launcher.resources.getBoolean(R.bool.ul_huawei_dock)) return
            val parent = launcher.hotseat.parent as? ViewGroup ?: return
            val dock = UlDockView(launcher)
            parent.addView(dock, parent.indexOfChild(launcher.hotseat), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }
}
