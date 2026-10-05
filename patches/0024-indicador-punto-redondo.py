#!/usr/bin/env python3
"""Parche 0024 (reaplicable): con UNA sola página el indicador se dibuja como un punto redondo (en AOSP la página
activa es una píldora de ancho 2*diámetro; OPPO muestra un punto). Con varias páginas no cambia nada.
El diámetro del punto sale de R.dimen.page_indicator_dot_size (redefinido en themetokens/res con la medida de OPPO).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/pageindicators/PageIndicatorDots.java"
t = p.read_text()
old = "                canvas.drawRoundRect(sTempRect, mDotRadius, mDotRadius, mPaginationPaint);\n\n                sTempRect.left = x;"
new = ("                if (mNumPages == 1) { // UltimateLauncher 0024: punto redondo con una sola página\n"
       "                    float ulCx = (sTempRect.left + sTempRect.right) / 2\n"
       "                            + getResources().getDimension(R.dimen.ul_page_indicator_single_dx);\n"
       "                    sTempRect.left = ulCx - mDotRadius;\n"
       "                    sTempRect.right = ulCx + mDotRadius;\n"
       "                }\n") + old
if "UltimateLauncher 0024" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
