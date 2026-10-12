#!/usr/bin/env python3
"""Parche 0232 (reaplicable): el punto de página única no sale cortado. Requiere 0024.

0024 dibuja el punto de una sola página desplazado `ul_page_indicator_single_dx` (medido en OPPO), pero la vista de los
puntos mide de ancho solo «dos puntos» y el lienzo recortaba la mitad derecha (visto en el Pixel 9 Pro Fold y en el
Pixel 8). Con una página, `onMeasure` suma a cada lado el desplazamiento, así el centro no se mueve y el punto cabe.
"""
import pathlib
p = (pathlib.Path(__file__).resolve().parent.parent
     / "launcher3-base/src/com/android/launcher3/pageindicators/PageIndicatorDots.java")
old = ("                (int) (((mNumPages - 1) * mGapWidth) + ((1 + mNumPages) * mDotRadius * 2));\n")
new = ("                (int) (((mNumPages - 1) * mGapWidth) + ((1 + mNumPages) * mDotRadius * 2)\n"
       "                        + (mNumPages == 1 ? 2 * Math.ceil(Math.abs(getResources().getDimension(\n"
       "                                R.dimen.ul_page_indicator_single_dx))) : 0)); // UltimateLauncher 0232\n")
t = p.read_text()
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
