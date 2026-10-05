// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: GPL-3.0-or-later
package com.qtekfun.ultimatelauncher.folder

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherFiles
import com.android.launcher3.folder.Folder
import com.qtekfun.ultimatelauncher.dock.UlDockView

/**
 * Carpeta abierta al estilo medido en OPPO (assets/themes/oppo-medido.json, «folder»): sin tarjeta, título arriba,
 * iconos en fila sobre el fondo de pantalla desenfocado y el resto de la pantalla de inicio oculto. Parche 0048.
 */
object FolderStyle {
    const val KEY = "pref_ul_folder_style"
    private const val BLUR_PX = 220
    private const val FADE_MS = 220L
    /** Posición vertical del centro del título (OPPO: 782 de 3168 px = 24,7 %). */
    private const val TITLE_CENTER_Y = 0.247f
    /** Ajuste de la separación título-iconos, en dp (negativo = más juntos, estilo iOS). */
    private const val TITLE_GAP_DP = 20f
    /** Jerarquía discreta estilo iOS: título ≈ 1,1× la etiqueta de los iconos, en negrita. */
    private const val TITLE_SCALE = 1.7f
    private const val BOTTOM_MARGIN_DP = 16f
    private const val TOP_MARGIN_DP = 24f

    /** Estado del interruptor para código sin contexto (organizador de la rejilla); se refresca al crear cada carpeta. */
    @Volatile private var active = true

    @JvmStatic fun fixedColumns(): Boolean = active

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getBoolean(KEY, true)

    /** Hijos de la pantalla de inicio que se ocultan mientras hay una carpeta abierta. */
    private fun covered(l: Launcher): List<View> {
        val dl = l.dragLayer
        val dock = (0 until dl.childCount).map { dl.getChildAt(it) }.filterIsInstance<UlDockView>()
        return listOfNotNull<View>(l.workspace, l.hotseat) + dock
    }

    @JvmStatic fun styleFolder(folder: Folder, footer: ViewGroup, name: android.widget.TextView) {
        active = enabled(folder.context)
        if (!active) return
        // El título pasa encima de los iconos.
        (footer.parent as? ViewGroup)?.let { p -> p.removeView(footer); p.addView(footer, 0) }
        // Separación título-iconos: la referencia tiene la fila de iconos 302 px bajo el título; sin esto quedaba ~70 px
        // más arriba (20 dp a 560 dpi). Se suma al relleno superior del contenido (cuenta en la altura deseada).
        folder.findViewById<View>(com.android.launcher3.R.id.folder_content)?.let { c ->
            c.setPadding(c.paddingLeft, c.paddingTop + (TITLE_GAP_DP * c.resources.displayMetrics.density).toInt(),
                c.paddingRight, c.paddingBottom)
        }
        name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, name.textSize * TITLE_SCALE)
        name.setTextColor(Color.WHITE)
        name.setHintTextColor(0xB3FFFFFF.toInt())
    }

    /** Celda de carpeta medida en OPPO: solo con el estilo activo y en pantallas de teléfono. */
    @JvmStatic fun phoneCells(context: Context): Boolean =
        enabled(context) && context.resources.configuration.smallestScreenWidthDp < 600

    /**
     * Filas por página de la carpeta: en horizontal (≈411 dp de alto a 560 dpi) 4 filas de 113 dp no caben, así que se
     * reducen hasta que quepan (con título, hueco y relleno) y el resto de apps pasa a páginas. En vertical no cambia.
     */
    @JvmStatic fun rowsFor(rows: Int, cellHeightPx: Int, footerPx: Int, metrics: android.util.DisplayMetrics, phone: Boolean): Int {
        if (!phone || cellHeightPx <= 0) return rows
        val usable = metrics.heightPixels * 0.80f - footerPx - (TITLE_GAP_DP + 24f) * metrics.density
        return (usable / cellHeightPx).toInt().coerceIn(1, rows)
    }

    /** Panel traslúcido tipo iOS tras el título y los iconos: aclara lo justo para leer sobre cualquier fondo. */
    private const val CARD_ARGB = 0x00000000
    /** Oscurecimiento del fondo desenfocado (el blanco sobre fondos claros no se lee). */
    private const val DIM = 0.1f

    @JvmStatic fun cardColor(context: Context): Int = CARD_ARGB

    /** Fondo de la carpeta cerrada: cristal claro, como el de OPPO/iOS, en lugar del azul oscuro del tema. */
    @JvmStatic fun closedIconColor(context: Context, themeColor: Int): Int =
        if (enabled(context)) 0xB3C0B6A6.toInt() else themeColor

    @JvmStatic fun transparentCard(context: Context): Boolean = enabled(context)

    /** Devuelve [x, y] de la esquina de la carpeta: centrada en horizontal y con el título en la banda de OPPO. */
    @JvmStatic fun position(launcher: Launcher, width: Int, height: Int, footerHeight: Int, out: IntArray) {
        if (!enabled(launcher)) return
        val dl = launcher.dragLayer
        out[0] = (dl.width - width) / 2
        val wanted = (dl.height * TITLE_CENTER_Y - footerHeight / 2f).toInt()
        // En horizontal la banda de OPPO (24,7 %) deja la carpeta fuera por abajo: se sube hasta que quepa sobre la barra de gestos.
        val bottom = dl.height - (BOTTOM_MARGIN_DP * dl.resources.displayMetrics.density).toInt()
        out[1] = minOf(wanted, bottom - height).coerceAtLeast((TOP_MARGIN_DP * dl.resources.displayMetrics.density).toInt())
    }

    /** Desenfoca lo que hay detrás de la ventana (el fondo de pantalla). Dos vías por si ColorOS ignora una. */
    private fun blur(l: Launcher, px: Int) {
        val w = l.window ?: return
        softClose?.cancel()
        softClose = null
        blurNow = px
        w.setBackgroundBlurRadius(px)
        val a = w.attributes
        a.dimAmount = if (px > 0) DIM else 0f
        a.blurBehindRadius = px
        w.attributes = a
        if (px > 0) w.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        else w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    @JvmStatic fun onOpen(launcher: Launcher) {
        if (!enabled(launcher)) return
        blur(launcher, BLUR_PX)
        // Tema claro: las etiquetas de las apps son oscuras (pensadas para la tarjeta clara); sobre el fondo oscurecido
        // deben ser blancas, como el título. Se aplica una vez que la carpeta está en el árbol de vistas.
        launcher.dragLayer.post {
            Folder.getOpen(launcher)?.iconsInReadingOrder?.filterIsInstance<android.widget.TextView>()
                ?.forEach { it.setTextColor(Color.WHITE) }
        }
        covered(launcher).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 0f).setDuration(FADE_MS).start() }
    }

    @JvmStatic fun onClose(launcher: Launcher) {
        if (!enabled(launcher)) return
        if (launcher.resources.configuration.smallestScreenWidthDp >= 600) {
            closeSoft(launcher) // tablet (parche 0121)
            return
        }
        blur(launcher, 0)
        covered(launcher).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 1f).setDuration(FADE_MS).start() }
    }

    // ---- Tablet (parche 0121): cierre sin saltos ----------------------------------------------------------------------
    // En el teléfono el desenfoque y el oscurecimiento se quitan de golpe al empezar el cierre. En la MatePad eso se veía
    // como un salto del fondo (y cada cambio de WindowManager.LayoutParams fuerza un relayout de la ventana, que tiraba
    // un fotograma justo al arrancar la animación). Además onClose se llamaba dos veces (inicio de animateClosed y
    // closeComplete), con un segundo relayout al final. Aquí: idempotente, el radio de desenfoque baja por fotograma
    // (setBackgroundBlurRadius no hace relayout), el oscurecimiento baja en pocos pasos y los indicadores de la ventana
    // se quitan una sola vez, antes de que termine la animación de la carpeta (200 ms).

    private const val SOFT_CLOSE_MS = 170L
    private const val DIM_STEPS = 5
    @Volatile private var blurNow = 0
    private var softClose: android.animation.ValueAnimator? = null

    private fun closeSoft(l: Launcher) {
        val w = l.window ?: return
        if (softClose != null) return // el cierre suave ya está en marcha
        if (blurNow == 0) return // ya cerrado: no repetir relayout ni fundidos
        covered(l).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 1f).setDuration(FADE_MS).start() }
        val startBlur = blurNow
        var lastStep = -1
        val va = android.animation.ValueAnimator.ofFloat(1f, 0f).setDuration(SOFT_CLOSE_MS)
        va.interpolator = android.view.animation.LinearInterpolator()
        va.addUpdateListener { anim ->
            val f = anim.animatedValue as Float
            w.setBackgroundBlurRadius((startBlur * f).toInt())
            val step = Math.round(f * DIM_STEPS)
            if (step != lastStep) { // pocos cambios de atributos de ventana (cada uno es un relayout)
                lastStep = step
                val a = w.attributes
                a.dimAmount = DIM * step / DIM_STEPS
                a.blurBehindRadius = (startBlur * step / DIM_STEPS)
                w.attributes = a
            }
        }
        va.addListener(object : android.animation.AnimatorListenerAdapter() {
            private var cancelled = false
            override fun onAnimationCancel(animation: android.animation.Animator) { cancelled = true }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (cancelled) return
                softClose = null
                blurNow = 0
                w.setBackgroundBlurRadius(0)
                val a = w.attributes
                a.dimAmount = 0f
                a.blurBehindRadius = 0
                w.attributes = a
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        })
        softClose = va
        va.start()
    }
}
