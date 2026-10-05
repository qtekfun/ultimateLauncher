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
