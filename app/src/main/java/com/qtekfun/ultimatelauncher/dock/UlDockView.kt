package com.qtekfun.ultimatelauncher.dock

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.graphics.ThemeManager
import com.android.launcher3.icons.LauncherIcons

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
        override fun onDragEnd() = setDragging(false)
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
     * Pulsación larga en un reciente: menú contextual que sale del propio icono (encima, con zoom desde él), con
     * «Quitar de recientes» y «Borrar todos los recientes». Estilo cristal oscuro redondeado; se cierra al tocar fuera.
     */
    private fun showRecentMenu(anchor: View, cn: ComponentName) {
        val d = res.displayMetrics.density
        fun dp(v: Float) = (v * d).toInt()
        val card = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xF02C2C2E.toInt()); cornerRadius = dp(18f).toFloat()
            }
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }
        var popup: android.widget.PopupWindow? = null
        fun row(textRes: Int, destructive: Boolean, action: () -> Unit) {
            val tv = android.widget.TextView(context).apply {
                setText(textRes)
                setTextColor(if (destructive) 0xFFFF6B63.toInt() else Color.WHITE)
                textSize = 16f
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(18f), 0, dp(18f), 0)
                minHeight = dp(50f)
                minWidth = dp(220f)
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), android.graphics.drawable.ColorDrawable(0x33FFFFFF))
                    addState(intArrayOf(), android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                }
                setOnClickListener {
                    popup?.dismiss()
                    action()
                    refresh() // al instante (el listener de preferencias también lo haría, pero asíncrono)
                }
            }
            if (card.childCount > 0) card.addView(android.view.View(context).apply {
                setBackgroundColor(0x33FFFFFF)
            }, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1))
            card.addView(tv, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        row(R.string.ul_dock_recent_remove, false) { RecentApps.remove(context, cn) }
        row(R.string.ul_dock_recent_clear_all, true) { RecentApps.clear(context) }

        card.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
        )
        val w = card.measuredWidth
        val h = card.measuredHeight
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val screenW = res.displayMetrics.widthPixels
        val margin = dp(8f)
        val iconCx = loc[0] + anchor.width / 2
        val x = (iconCx - w / 2).coerceIn(margin, maxOf(margin, screenW - w - margin))
        val y = maxOf(margin, loc[1] - h - dp(10f))

        // Hueco alrededor de la tarjeta para que la sombra no se recorte.
        val frame = android.widget.FrameLayout(context).apply {
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            clipToPadding = false
            clipChildren = false
            addView(card)
        }
        popup = android.widget.PopupWindow(frame, w + dp(28f), h + dp(28f), true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            isClippingEnabled = false
        }
        popup.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, x - dp(14f), y - dp(14f))
        // Sale del icono: zoom + fundido con el pivote en la base, centrado sobre el icono.
        card.pivotX = (iconCx - x).toFloat().coerceIn(0f, w.toFloat())
        card.pivotY = h.toFloat()
        card.scaleX = 0.4f; card.scaleY = 0.4f; card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
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
        val g = if (recents.isEmpty()) 0f else gap
        val total = leftW + g + rightW
        val startX = x0 + (w - total) / 2f
        val bottom = h - bottomMargin
        val top = bottom - pillH
        leftRect.set(startX, top, startX + leftW, bottom)
        rightRect.set(leftRect.right + g, top, leftRect.right + g + rightW, bottom)
        val hw = 4f * res.displayMetrics.density
        val hh = 35f * res.displayMetrics.density
        handleRect.set(leftRect.right + g / 2 - hw / 2, top + (pillH - hh) / 2, leftRect.right + g / 2 + hw / 2, top + (pillH + hh) / 2)
        // El hotseat se centra solo; se desplaza para que el conjunto (izquierda + asa + derecha) quede centrado.
        launcher.hotseat.translationX = (startX - x0) + pad - (w - n * cell) / 2f - first * cell
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val x = (rightRect.left + pad + i * cell + (cell - iconPx) / 2).toInt()
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
        if (recents.isNotEmpty()) {
            canvas.drawRoundRect(rightRect, radius, radius, pillPaint)
            canvas.drawRoundRect(handleRect, handleRect.width() / 2, handleRect.width() / 2, handlePaint)
        }
    }

    companion object {
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
