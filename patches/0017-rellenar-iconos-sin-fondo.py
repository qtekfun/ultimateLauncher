#!/usr/bin/env python3
"""Parche 0017 (reaplicable): iconos adaptativos con fondo vacío/transparente se rellenan con la baldosa clara.

Huawei (y OPPO) dibujan los iconos sin fondo propio (p. ej. un engranaje negro) sobre una baldosa clara; AOSP los deja transparentes
y se ven «sueltos». Se detecta renderizando el fondo (24×24) y, si <50 % es opaco, se sustituye por DEFAULT_WRAPPER_BACKGROUND.
Archivo: systemui-libs/iconloaderlib/src/com/android/launcher3/icons/BaseIconFactory.kt
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "systemui-libs/iconloaderlib/src/com/android/launcher3/icons/BaseIconFactory.kt"
t = p.read_text()
if "fillTransparentBackground" not in t:
    old = "        if (options.wrapNonAdaptiveIcon) tempIcon = wrapToAdaptiveIcon(tempIcon, options)\n"
    assert old in t
    t = t.replace(old, old + "        tempIcon = fillTransparentBackground(tempIcon) // UltimateLauncher 0017\n", 1)
    fn = '''
    /** UltimateLauncher 0017: rellena con la baldosa clara los iconos adaptativos sin fondo visible. */
    private fun fillTransparentBackground(icon: Drawable): Drawable {
        if (icon !is AdaptiveIconDrawable) return icon
        val bg = icon.background
        if (bg != null && !isMostlyTransparent(bg)) return icon
        val fg = icon.foreground ?: return icon
        return AdaptiveIconDrawable(ColorDrawable(DEFAULT_WRAPPER_BACKGROUND), fg).apply { setBounds(0, 0, 1, 1) }
    }

    private fun isMostlyTransparent(d: Drawable): Boolean {
        val size = 24
        val bmp = Bitmap.createBitmap(size, size, ARGB_8888)
        val oldBounds = d.copyBounds()
        d.setBounds(0, 0, size, size)
        d.draw(android.graphics.Canvas(bmp))
        d.bounds = oldBounds
        var opaque = 0
        for (y in 0 until size) for (x in 0 until size) if (Color.alpha(bmp.getPixel(x, y)) > 200) opaque++
        bmp.recycle()
        return opaque < size * size / 2
    }
'''
    marker = "    fun getBitmapFlagOp(options: IconOptions?): FlagOp {"
    assert marker in t
    t = t.replace(marker, fn.lstrip("\n") + "\n" + marker, 1)
    p.write_text(t)

# 0017b: iconos heredados (no adaptativos) con fondo propio opaco -> llenan toda la baldosa (como los originales de Huawei),
# en vez de encogerse sobre un fondo blanco.
t = p.read_text()
if "fillOpaqueLegacy" not in t:
    old = "        if (options.wrapNonAdaptiveIcon) tempIcon = wrapToAdaptiveIcon(tempIcon, options)\n"
    assert old in t
    t = t.replace(old, "        tempIcon = fillOpaqueLegacy(tempIcon) // UltimateLauncher 0017b\n" + old, 1)
    fn = '''    /** UltimateLauncher 0017b: icono heredado opaco (≥ 75 % de píxeles) -> adaptativo a sangre, sin encoger. */
    private fun fillOpaqueLegacy(icon: Drawable): Drawable {
        if (icon is AdaptiveIconDrawable || !isMostlyOpaque(icon)) return icon
        var inset = AdaptiveIconDrawable.getExtraInsetFraction()
        inset /= (1 + 2 * inset)
        return AdaptiveIconDrawable(ColorDrawable(Color.BLACK), InsetDrawable(icon, inset, inset, inset, inset))
    }

    private fun isMostlyOpaque(d: Drawable): Boolean {
        val size = 24
        val bmp = Bitmap.createBitmap(size, size, ARGB_8888)
        val oldBounds = d.copyBounds()
        d.setBounds(0, 0, size, size)
        d.draw(android.graphics.Canvas(bmp))
        d.bounds = oldBounds
        var opaque = 0
        for (y in 0 until size) for (x in 0 until size) if (Color.alpha(bmp.getPixel(x, y)) > 200) opaque++
        bmp.recycle()
        return opaque * 4 >= size * size * 3
    }

'''
    marker = "    /** UltimateLauncher 0017: rellena con la baldosa clara"
    assert marker in t
    t = t.replace(marker, fn + marker, 1)
    p.write_text(t)

# 0017c: el fondo del icono heredado opaco es el color de su borde (no negro), para no dejar marco en las esquinas redondeadas.
t = p.read_text()
if "legacyEdgeColor" not in t:
    t = t.replace("AdaptiveIconDrawable(ColorDrawable(Color.BLACK), InsetDrawable(icon, inset, inset, inset, inset))\n    }\n\n    private fun isMostlyOpaque",
                  "AdaptiveIconDrawable(ColorDrawable(legacyEdgeColor(icon)), InsetDrawable(icon, inset, inset, inset, inset))\n    }\n\n"
                  "    /** Media del color en la mitad de cada lado (píxeles opacos); blanco si no hay. */\n"
                  "    private fun legacyEdgeColor(d: Drawable): Int {\n"
                  "        val size = 24\n        val bmp = Bitmap.createBitmap(size, size, ARGB_8888)\n        val oldBounds = d.copyBounds()\n"
                  "        d.setBounds(0, 0, size, size)\n        d.draw(android.graphics.Canvas(bmp))\n        d.bounds = oldBounds\n"
                  "        var r = 0; var g = 0; var b = 0; var n = 0\n"
                  "        for ((x, y) in listOf(12 to 2, 12 to 21, 2 to 12, 21 to 12)) {\n"
                  "            val c = bmp.getPixel(x, y)\n            if (Color.alpha(c) > 200) { r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++ }\n        }\n"
                  "        bmp.recycle()\n        return if (n == 0) Color.WHITE else Color.rgb(r / n, g / n, b / n)\n    }\n\n    private fun isMostlyOpaque", 1)
    assert "legacyEdgeColor" in t
    p.write_text(t)
