#!/usr/bin/env python3
"""Parche 0019 (reaplicable): la forma de icono propia se aplica también fuera de iconos temáticos (cajón, carpetas…).

AOSP recorta con la forma del launcher solo si creationFlags incluye FLAG_THEMED (inicio); en el cajón se pintaba con la forma
por defecto (cuadrada en Huawei). Se elimina esa condición en ItemInfoWithIcon.supportsCustomShapes.
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/model/data/ItemInfoWithIcon.java"
t = p.read_text()
old = "        return Flags.enableLauncherIconShapes()\n                && (creationFlags & FLAG_THEMED) != 0\n                && bitmap.isFullBleed();"
new = "        return Flags.enableLauncherIconShapes()\n                && bitmap.isFullBleed(); // UltimateLauncher 0019: sin exigir FLAG_THEMED"
if new not in t:
    assert old in t
    p.write_text(t.replace(old, new))
