#!/usr/bin/env python3
"""Parche 0018 (reaplicable): la máscara de icono puede venir de R.string.ul_icon_mask (tokens) en lugar de la del sistema.

Huawei dibuja los iconos con su propia superelipse (la máscara del sistema es un cuadrado). tools/apply-tablet-tokens.py genera
ul_icon_mask para sw600dp; en móvil queda vacío y se usa la máscara del sistema (comportamiento de AOSP).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/graphics/ThemeManager.kt"
t = p.read_text()
old = "                shapeModel != null -> shapeModel.pathString\n"
new = old + "                context.resources.getString(com.android.launcher3.R.string.ul_icon_mask).isNotEmpty() ->\n                    context.resources.getString(com.android.launcher3.R.string.ul_icon_mask) // UL 0018\n"
if "ul_icon_mask" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
