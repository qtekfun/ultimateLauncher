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
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(0xB8, 0xFF, 0xFF, 0xFF) }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(0x99, 0xFF, 0xFF, 0xFF) }
    private val leftRect = RectF()
    private val rightRect = RectF()
    private val handleRect = RectF()
    private var recents: List<ComponentName> = emptyList()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == RecentApps.CHANGED_KEY) post { refresh() }
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        RecentApps.prefs(context).registerOnSharedPreferenceChangeListener(listener)
        refresh()
    }

    override fun onDetachedFromWindow() {
        RecentApps.prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
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
        val li = LauncherIcons.obtain(context)
        try {
            for (cn in RecentApps.load(context)) {
                if (shown.size >= RecentApps.max(context)) break
                if (cn.packageName in fixed) continue
                val info = la.getActivityList(cn.packageName, Process.myUserHandle()).firstOrNull { it.componentName == cn } ?: continue
                val bmp = li.createBadgedIconBitmap(info.getIcon(0))
                val v = ImageView(context).apply {
                    setImageDrawable(bmp.newIcon(context, 0, shape))
                    contentDescription = info.label
                    layoutParams = LayoutParams(iconPx, iconPx)
                    setOnClickListener { la.startMainActivity(cn, Process.myUserHandle(), null, null) }
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

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = (r - l).toFloat()
        val h = (b - t).toFloat()
        val n = launcher.deviceProfile.hotseatProfile.numShownIcons
        val leftW = n * cell + 2 * pad
        val rightW = if (recents.isEmpty()) 0f else recents.size * cell + 2 * pad
        val g = if (recents.isEmpty()) 0f else gap
        val total = leftW + g + rightW
        val startX = (w - total) / 2f
        val bottom = h - launcher.deviceProfile.insets.bottom - bottomMargin
        val top = bottom - pillH
        leftRect.set(startX, top, startX + leftW, bottom)
        rightRect.set(leftRect.right + g, top, leftRect.right + g + rightW, bottom)
        val hw = 4f * res.displayMetrics.density
        val hh = 35f * res.displayMetrics.density
        handleRect.set(leftRect.right + g / 2 - hw / 2, top + (pillH - hh) / 2, leftRect.right + g / 2 + hw / 2, top + (pillH + hh) / 2)
        // El hotseat se centra solo; se desplaza para que el conjunto (izquierda + asa + derecha) quede centrado.
        launcher.hotseat.translationX = startX - (w - leftW) / 2f
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val x = (rightRect.left + pad + i * cell + (cell - iconPx) / 2).toInt()
            val y = (top + (pillH - iconPx) / 2).toInt()
            v.layout(x, y, x + iconPx, y + iconPx)
        }
    }

    override fun onDraw(canvas: Canvas) {
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
