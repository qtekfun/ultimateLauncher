// SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
// SPDX-License-Identifier: Apache-2.0
package com.qtekfun.ultimatelauncher.wallpaper

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Lógica pura (sin tipos de Android, probada con pruebas unitarias) del selector «Fondos de UltimateLauncher»:
 * recorte «cover» centrado, desenfoque de caja y oscurecimiento sobre píxeles ARGB.
 */
object WallpaperLogic {
    /** Rectángulo de recorte en píxeles de la imagen origen (límites derecho e inferior exclusivos). */
    data class Crop(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left
        val height get() = bottom - top
    }

    /**
     * Recorte centrado que llena un destino de `dstW`x`dstH` sin deformar: se conserva todo el alto o todo el ancho
     * del origen y se recorta el exceso por ambos lados por igual. Vale para móvil vertical (recorte estrecho) y para
     * tablet apaisada (recorte ancho y bajo).
     */
    @JvmStatic
    fun coverCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Crop {
        require(srcW > 0 && srcH > 0 && dstW > 0 && dstH > 0) { "tamaños no positivos" }
        // srcW/srcH > dstW/dstH sin dividir (en Long por si hay tamaños grandes).
        val srcWider = srcW.toLong() * dstH > dstW.toLong() * srcH
        return if (srcWider) {
            val w = max(1, (srcH.toLong() * dstW / dstH).toInt())
            val l = (srcW - w) / 2
            Crop(l, 0, l + w, srcH)
        } else {
            val h = max(1, (srcW.toLong() * dstH / dstW).toInt())
            val t = (srcH - h) / 2
            Crop(0, t, srcW, t + h)
        }
    }

    /** Tamaño de salida: el de la pantalla (`dstW`x`dstH`), reducido manteniendo la proporción si su lado largo supera `maxSide`. */
    @JvmStatic
    fun outputSize(dstW: Int, dstH: Int, maxSide: Int): Pair<Int, Int> {
        val longSide = max(dstW, dstH)
        if (longSide <= maxSide) return dstW to dstH
        val f = maxSide.toFloat() / longSide
        return max(1, (dstW * f).roundToInt()) to max(1, (dstH * f).roundToInt())
    }

    /** Radio (en píxeles de la copia reducida de `smallW` de ancho) para un desenfoque de 0..100. */
    @JvmStatic
    fun blurRadius(blurPct: Int, smallW: Int): Int =
        if (blurPct <= 0) 0 else max(1, (blurPct.coerceIn(0, 100) / 100f * smallW * 0.06f).roundToInt())

    /** Factor de reducción antes de desenfocar: la copia pequeña tiene unos 360 px de ancho como mínimo. */
    @JvmStatic
    fun blurDownscale(w: Int): Int = max(1, w / 360)

    /**
     * Desenfoque de caja (tres pasadas por eje ≈ gaussiano) sobre `px` ARGB de `w`x`h`, con los bordes repetidos.
     * Modifica el array en su sitio.
     */
    @JvmStatic
    fun boxBlur(px: IntArray, w: Int, h: Int, radius: Int) {
        if (radius <= 0 || w <= 0 || h <= 0) return
        val tmp = IntArray(px.size)
        repeat(3) {
            blurAxis(px, tmp, w, h, radius, horizontal = true)
            blurAxis(tmp, px, w, h, radius, horizontal = false)
        }
    }

    private fun blurAxis(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (line in 0 until lines) {
            fun idx(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = src[idx(i)]
                a += p ushr 24; rr += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until len) {
                dst[idx(i)] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
                val out = src[idx(i - r)]
                val inn = src[idx(i + r + 1)]
                a += (inn ushr 24) - (out ushr 24)
                rr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inn and 0xFF) - (out and 0xFF)
            }
        }
    }

    /** Alfa (0..255) de la capa negra para un oscurecimiento de 0..100: el 100 % deja un 15 % de luz para no dar negro puro. */
    @JvmStatic
    fun dimAlpha(dimPct: Int): Int = (dimPct.coerceIn(0, 100) / 100f * 0.85f * 255f).roundToInt()

    /** Columnas de la rejilla de miniaturas: una cada ~160 dp de ancho, entre 2 y 6 (más en tablet y en horizontal). */
    @JvmStatic
    fun gridColumns(widthDp: Float): Int = (widthDp / 160f).toInt().coerceIn(2, 6)

    /** Alto/ancho de cada miniatura: la proporción real de la pantalla (vertical alta, horizontal baja), acotada a 0,55..1,45 (1,45 era la de móvil vertical). */
    @JvmStatic
    fun cellAspect(screenW: Int, screenH: Int): Float =
        if (screenW <= 0 || screenH <= 0) 1.45f else (screenH.toFloat() / screenW).coerceIn(0.55f, 1.45f)

    /**
     * Ancho (dp) del panel de desenfoque/oscurecer/aplicar de la vista previa: toda la pantalla en móvil vertical y una
     * tarjeta de 460 dp como máximo en horizontal y tablet (así no tapa la imagen de lado a lado ni se estira).
     */
    @JvmStatic
    fun panelWidthDp(screenWidthDp: Float): Int = minOf(screenWidthDp - 24f, 460f).toInt()
}
