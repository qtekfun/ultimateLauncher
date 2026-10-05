#!/usr/bin/env python3
"""Parche 0042 (reaplicable): el espacio entre iconos del hotseat de tablet se acota a (celda del dock − tamaño de icono).

AOSP reparte el hotseat sobre las columnas del workspace (maxIconSpacePx = Int.MAX_VALUE sin botones de navegación en línea);
con R.bool.ul_huawei_dock la celda mide R.dimen.ul_dock_cell (175 px en la tablet de referencia).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/deviceprofile/HotseatProfileInitialValues.kt"
t = p.read_text()
old = "else Int.MAX_VALUE,\n"
new = ("else if (res.getBoolean(R.bool.ul_huawei_dock)) // UltimateLauncher 0042\n"
       "                        max(0, res.getDimensionPixelSize(R.dimen.ul_dock_cell) - pxFromDp(inv.iconSize[typeIndex], res.displayMetrics))\n"
       "                    else Int.MAX_VALUE,\n")
if "UltimateLauncher 0042" not in t:
    assert old in t
    p.write_text(t.replace(old, new))
