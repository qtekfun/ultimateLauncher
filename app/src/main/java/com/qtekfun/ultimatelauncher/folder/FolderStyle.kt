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
    private const val BLUR_PX = 90
    private const val FADE_MS = 220L
    /** Posición vertical del centro del título (OPPO: 782 de 3168 px = 24,7 %). */
    private const val TITLE_CENTER_Y = 0.247f
    /** Hueco extra entre el pie (título) y la fila de iconos, en dp: (1084-1014 px)/3,5 = 20. */
    private const val TITLE_GAP_DP = 20f
    private const val BOTTOM_MARGIN_DP = 16f
    private const val TOP_MARGIN_DP = 24f

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
        if (!enabled(folder.context)) return
        // El título pasa encima de los iconos.
        (footer.parent as? ViewGroup)?.let { p -> p.removeView(footer); p.addView(footer, 0) }
        // Separación título-iconos: la referencia tiene la fila de iconos 302 px bajo el título; sin esto quedaba ~70 px
        // más arriba (20 dp a 560 dpi). Se suma al relleno superior del contenido (cuenta en la altura deseada).
        folder.findViewById<View>(com.android.launcher3.R.id.folder_content)?.let { c ->
            c.setPadding(c.paddingLeft, c.paddingTop + (TITLE_GAP_DP * c.resources.displayMetrics.density).toInt(),
                c.paddingRight, c.paddingBottom)
        }
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
    private const val CARD_ARGB = 0x38FFFFFF
    /** Oscurecimiento del fondo desenfocado (el blanco sobre fondos claros no se lee). */
    private const val DIM = 0.32f

    @JvmStatic fun cardColor(context: Context): Int = CARD_ARGB

    /** Fondo de la carpeta cerrada: cristal claro, como el de OPPO/iOS, en lugar del azul oscuro del tema. */
    @JvmStatic fun closedIconColor(context: Context, themeColor: Int): Int =
        if (enabled(context)) 0x66FFFFFF else themeColor

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
        blur(launcher, 0)
        covered(launcher).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 1f).setDuration(FADE_MS).start() }
    }
}
