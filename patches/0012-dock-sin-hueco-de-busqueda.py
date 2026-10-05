#!/usr/bin/env python3
"""Parche 0012 (reaplicable): el dock no reserva el hueco de la barra de búsqueda cuando no hay QSB.

Motivo: HotseatProfileInitialValues.calculateHotseatBarSizePx suma qsbSpace + qsbVisualHeight (≈ 64 dp) aunque
UltimateLauncher no tiene QSB (BuildConfig.QSB_ON_FIRST_SCREEN=false), lo que levantaba el dock respecto a OPPO.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/deviceprofile/HotseatProfileInitialValues.kt"
t = p.read_text()
old = "                return (hotseatIconSizePx + qsbSpace + qsbVisualHeight + barBottomSpacePx)\n"
new = ("                // UltimateLauncher 0012: sin QSB no se reserva su hueco.\n"
       "                return if (com.android.launcher3.BuildConfig.QSB_ON_FIRST_SCREEN)\n"
       "                    (hotseatIconSizePx + qsbSpace + qsbVisualHeight + barBottomSpacePx)\n"
       "                else (hotseatIconSizePx + barBottomSpacePx)\n")
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))
