// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.ui

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import com.android.launcher3.R
import com.android.launcher3.popup.PopupData
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.shortcuts.DeepShortcutView

/**
 * Estilo y comportamiento ÚNICOS de todos los menús contextuales del launcher (parche 0150): tarjeta de cristal oscuro
 * redondeada, texto blanco, filas de 50 dp con separadores finos, estado pulsado translúcido, acción destructiva en
 * rojo, sin flecha, y aparición con zoom + fundido desde el propio icono. Lo usan:
 *  - `ArrowPopup` (menús de apps/accesos/carpetas/widgets y del fondo del escritorio) mediante el parche 0150,
 *  - el menú de recientes del dock ([showRows], sobre un `PopupWindow`).
 * Los valores viven en `res/values/ul_menu.xml`; aquí solo hay lógica compartida (colocación, animación, rojo).
 */
object ContextMenuStyle {
    /** Zoom inicial al aparecer (y final al cerrar). */
    const val START_SCALE = 0.4f

    /** Posición de la tarjeta: esquina superior izquierda, si queda encima del ancla y si cabe en pantalla. */
    data class Placement(val x: Int, val y: Int, val above: Boolean, val fits: Boolean)

    /**
     * Coloca una tarjeta de `w`x`h` px centrada sobre el ancla y encima de ella (`gap` px) si cabe en `bounds` con
     * `margin` px de margen; si no, debajo; si tampoco cabe, `fits=false` (quien llama decide, p. ej. centrar).
     * `x` queda acotado a la pantalla con `margin`. Con `maxH>0` se usa esa altura prevista para decidir.
     */
    @JvmStatic
    @JvmOverloads
    fun place(anchor: Rect, w: Int, h: Int, bounds: Rect, gap: Int, margin: Int, maxH: Int = 0): Placement =
        place(anchor.centerX(), anchor.top, anchor.bottom, w, h, bounds.left, bounds.top, bounds.right, bounds.bottom, gap, margin, maxH)

    /** Versión con enteros (sin tipos de Android; es la que prueban las pruebas unitarias). */
    @JvmStatic
    fun place(
        anchorCx: Int, anchorTop: Int, anchorBottom: Int, w: Int, h: Int,
        left: Int, top: Int, right: Int, bottom: Int, gap: Int, margin: Int, maxH: Int = 0,
    ): Placement {
        val minX = left + margin
        val maxX = right - margin - w
        val x = if (maxX < minX) (left + right - w) / 2 else (anchorCx - w / 2).coerceIn(minX, maxX)
        val need = if (maxH > 0) maxH else h
        if (anchorTop - gap - need >= top + margin) return Placement(x, anchorTop - gap - h, above = true, fits = true)
        val belowY = anchorBottom + gap
        return Placement(x, belowY, above = false, fits = belowY + h <= bottom - margin)
    }

    /** Pivote horizontal de la animación: centro del ancla relativo a la tarjeta, dentro de ella. */
    @JvmStatic
    fun pivotX(anchorCx: Int, cardX: Int, w: Int): Float = (anchorCx - cardX).coerceIn(0, maxOf(0, w)).toFloat()

    @JvmStatic
    fun cornerRadiusPx(c: Context): Float = c.resources.getDimension(R.dimen.ul_menu_corner_radius)

    @JvmStatic
    fun cardColor(c: Context): Int = c.getColor(R.color.ul_menu_card)

    @JvmStatic
    fun cardDrawable(c: Context) = GradientDrawable().apply {
        setColor(cardColor(c)); cornerRadius = cornerRadiusPx(c)
    }

    /** Textos cuya acción es destructiva (se pintan en rojo). */
    private val destructiveLabels = intArrayOf(
        R.string.remove_system_shortcut_label,
        R.string.uninstall_private_system_shortcut_label,
        R.string.uninstall_drop_target_label,
    )

    @JvmStatic
    fun isDestructive(@StringRes labelRes: Int): Boolean = labelRes != 0 && labelRes in destructiveLabels

    /** Recorre las filas de un menú de ArrowPopup y pinta en rojo (texto e icono) las destructivas. */
    @JvmStatic
    fun styleRows(root: View) {
        if (root is DeepShortcutView) {
            val res = when (val t = root.tag) {
                is SystemShortcut<*> -> t.labelResId
                is PopupData -> t.labelResId
                else -> 0
            }
            if (isDestructive(res)) {
                val red = root.context.getColor(R.color.ul_menu_destructive)
                root.bubbleText.setTextColor(red)
                root.iconView.backgroundTintList = ColorStateList.valueOf(red)
            }
            return
        }
        if (root is ViewGroup) for (i in 0 until root.childCount) styleRows(root.getChildAt(i))
    }

    /** Entrada: zoom + fundido con el pivote en la base de la tarjeta (en el borde superior si va debajo). */
    @JvmStatic
    fun animateIn(card: View, pivotX: Float, above: Boolean, durationMs: Long) {
        card.pivotX = pivotX
        card.pivotY = if (above) (if (card.height > 0) card.height else card.measuredHeight).toFloat() else 0f
        card.scaleX = START_SCALE; card.scaleY = START_SCALE; card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(durationMs)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    /**
     * Diálogo de elección única con el mismo aspecto que los menús (tarjeta de cristal oscuro, filas de 50 dp con
     * separadores, texto blanco, marca en la fila elegida y zoom + fundido al aparecer). Se cierra al elegir o al tocar
     * fuera; `onPick` recibe el índice. Pensado para listas cortas o medianas (hace scroll si no caben).
     */
    fun showChoiceDialog(
        context: Context,
        title: CharSequence,
        items: List<CharSequence>,
        selected: Int,
        onPick: (Int) -> Unit,
    ): Dialog {
        val res = context.resources
        val rowH = res.getDimensionPixelSize(R.dimen.ul_menu_row_height)
        val pad = res.getDimensionPixelSize(R.dimen.ul_menu_text_padding)
        val dm = res.displayMetrics
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardDrawable(context)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        card.addView(TextView(context).apply {
            text = title; textSize = 18f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(context.getColor(R.color.ul_menu_text)); setPadding(pad, pad, pad, pad / 2)
        })
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = context.getDrawable(R.drawable.ul_menu_divider)
        }
        items.forEachIndexed { i, label ->
            list.addView(TextView(context).apply {
                text = if (i == selected) "$label  \u2713" else label
                textSize = 16f
                setTextColor(context.getColor(R.color.ul_menu_text))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(pad, 0, pad, 0)
                minHeight = rowH
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(context.getColor(R.color.ul_menu_pressed)))
                    addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
                }
                setOnClickListener { dialog.dismiss(); onPick(i) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val listH = minOf(items.size * rowH + items.size, (dm.heightPixels * 0.6f).toInt())
        card.addView(ScrollView(context).apply { addView(list) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listH))
        val maxW = minOf(dm.widthPixels - 2 * res.getDimensionPixelSize(R.dimen.ul_menu_screen_margin) * 3,
            res.getDimensionPixelSize(R.dimen.ul_menu_min_width) * 3 / 2 + pad * 2)
        dialog.setContentView(card, ViewGroup.LayoutParams(maxOf(maxW, res.getDimensionPixelSize(R.dimen.ul_menu_min_width)), ViewGroup.LayoutParams.WRAP_CONTENT))
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.45f }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        card.post {
            animateIn(card, card.width / 2f, above = false, res.getInteger(R.integer.ul_menu_open_ms).toLong())
            card.pivotY = card.height / 2f
        }
        return dialog
    }

    /** Una fila de un menú de `PopupWindow` (el del dock). */
    class Row(@StringRes val labelRes: Int, val destructive: Boolean, val action: () -> Unit)

    /**
     * Menú sobre un `PopupWindow` con el mismo aspecto que los de ArrowPopup. Sale encima de `anchor` (debajo si no
     * cabe), centrado y acotado a la pantalla, y se cierra al tocar fuera o al elegir una fila.
     */
    fun showRows(anchor: View, rows: List<Row>, afterAction: () -> Unit = {}): PopupWindow {
        val c = anchor.context
        val res = c.resources
        val rowH = res.getDimensionPixelSize(R.dimen.ul_menu_row_height)
        val minW = res.getDimensionPixelSize(R.dimen.ul_menu_min_width)
        val pad = res.getDimensionPixelSize(R.dimen.ul_menu_text_padding)
        val inset = res.getDimensionPixelSize(R.dimen.ul_menu_shadow_inset)
        val margin = res.getDimensionPixelSize(R.dimen.ul_menu_screen_margin)
        val card = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = cardDrawable(c)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            dividerDrawable = c.getDrawable(R.drawable.ul_menu_divider)
        }
        var popup: PopupWindow? = null
        for (r in rows) {
            card.addView(TextView(c).apply {
                setText(r.labelRes)
                setTextColor(c.getColor(if (r.destructive) R.color.ul_menu_destructive else R.color.ul_menu_text))
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(pad, 0, pad, 0)
                minHeight = rowH
                minWidth = minW
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(c.getColor(R.color.ul_menu_pressed)))
                    addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
                }
                setOnClickListener { popup?.dismiss(); r.action(); afterAction() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        card.measure(unspecified, unspecified)
        val w = card.measuredWidth
        val h = card.measuredHeight
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val dm = res.displayMetrics
        val anchorRect = Rect(loc[0], loc[1], loc[0] + anchor.width, loc[1] + anchor.height)
        val p = place(anchorRect, w, h, Rect(0, 0, dm.widthPixels, dm.heightPixels),
            res.getDimensionPixelSize(R.dimen.ul_menu_anchor_gap), margin)
        // Hueco alrededor de la tarjeta para que la sombra no se recorte.
        val frame = FrameLayout(c).apply {
            setPadding(inset, inset, inset, inset); clipToPadding = false; clipChildren = false; addView(card)
        }
        popup = PopupWindow(frame, w + 2 * inset, h + 2 * inset, true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isClippingEnabled = false
        }
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, p.x - inset, maxOf(margin, p.y) - inset)
        animateIn(card, pivotX(anchorRect.centerX(), p.x, w), p.above, res.getInteger(R.integer.ul_menu_open_ms).toLong())
        return popup
    }
}
