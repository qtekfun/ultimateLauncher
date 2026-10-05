#!/usr/bin/env python3
"""Parche 0100 (reaplicable): el hueco del buscador (OseWidgetView) sin proveedor (sin GMS) no captura el táctil.

CAUSA de «las apps del dock no son editables» en la tablet: Hotseat añade el QSB como hijo POR ENCIMA de las celdas; en
tablet mide casi todo el ancho del dock (275..2348 px en la MatePad) y, aunque no pinta nada sin GMS, es long-clickable y
recibía todos los toques: ni la pulsación corta (abrir app), ni la larga (menú/arrastre) llegaban a los iconos fijos.
Arreglo: sin proveedor, dispatchTouchEvent devuelve false y el ViewGroup pasa el gesto al siguiente hijo (las celdas).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/qsb/OseWidgetView.kt"
t = p.read_text()
old = "    override fun shouldDelayChildPressedState(): Boolean {\n"
new = ("    // UltimateLauncher 0100: sin proveedor (sin GMS) el hueco está vacío y no debe tapar los iconos del dock\n"
       "    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean =\n"
       "        if (appWidgetInfo == null) false else super.dispatchTouchEvent(ev)\n\n" + old)
if "UltimateLauncher 0100" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
