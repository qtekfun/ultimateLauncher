package com.qtekfun.ultimatelauncher.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ScrollView
import kotlin.math.min

/** Cálculos puros de la maquetación de pantallas de texto (copia, importación, apps ocultas, asistente). */
object ScreenLayoutLogic {
    /** Ancho máximo de columna de lectura en tablet (dp). */
    const val MAX_CONTENT_WIDTH_DP = 640

    /** Ancho que ocupa el contenido: todo el disponible en móvil y [maxPx] como máximo en pantallas anchas. */
    @JvmStatic fun contentWidth(availablePx: Int, maxPx: Int): Int = if (maxPx <= 0) availablePx else min(availablePx, maxPx)
}

/** Contenedor que limita su ancho (no su alto) a [maxWidthPx]; dentro de un `ScrollView` se centra con `layout_gravity`. */
class MaxWidthFrameLayout(context: Context, private val maxWidthPx: Int) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val w = ScreenLayoutLogic.contentWidth(size, maxWidthPx)
        val mode = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) MeasureSpec.UNSPECIFIED else MeasureSpec.EXACTLY
        super.onMeasure(MeasureSpec.makeMeasureSpec(w, mode), heightMeasureSpec)
    }
}

object ScreenLayout {
    /**
     * Envuelve [content] en un `ScrollView` que respeta las barras del sistema y el recorte de pantalla (edge-to-edge) y
     * centra el contenido con un ancho máximo de [ScreenLayoutLogic.MAX_CONTENT_WIDTH_DP] dp. Así en tablet las pantallas de
     * texto no se estiran de borde a borde y en horizontal siempre se pueden desplazar.
     */
    @JvmStatic
    fun scrollColumn(context: Context, content: View): View {
        val density = context.resources.displayMetrics.density
        val column = MaxWidthFrameLayout(context, (ScreenLayoutLogic.MAX_CONTENT_WIDTH_DP * density).toInt())
        column.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(context).apply { isFillViewport = true; clipToPadding = false }
        scroll.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_HORIZONTAL or Gravity.TOP))
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
        return scroll
    }
}
