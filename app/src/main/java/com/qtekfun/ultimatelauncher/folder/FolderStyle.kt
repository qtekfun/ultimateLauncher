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
        name.setTextColor(Color.WHITE)
        name.setHintTextColor(0xB3FFFFFF.toInt())
    }

    /** Celda de carpeta medida en OPPO: solo con el estilo activo y en pantallas de teléfono. */
    @JvmStatic fun phoneCells(context: Context): Boolean =
        enabled(context) && context.resources.configuration.smallestScreenWidthDp < 600

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
    @JvmStatic fun position(launcher: Launcher, width: Int, footerHeight: Int, out: IntArray) {
        if (!enabled(launcher)) return
        val dl = launcher.dragLayer
        out[0] = (dl.width - width) / 2
        out[1] = (dl.height * TITLE_CENTER_Y - footerHeight / 2f).toInt()
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
        covered(launcher).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 0f).setDuration(FADE_MS).start() }
    }

    @JvmStatic fun onClose(launcher: Launcher) {
        if (!enabled(launcher)) return
        blur(launcher, 0)
        covered(launcher).forEach { ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 1f).setDuration(FADE_MS).start() }
    }
}
