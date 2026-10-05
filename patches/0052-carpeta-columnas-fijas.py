#!/usr/bin/env python3
"""Parche 0052 (reaplicable, idempotente): carpeta con columnas fijas como en OPPO.

AOSP calcula la rejilla de la carpeta como ceil(sqrt(n)) (4 apps = 2x2). OPPO mantiene siempre 3 columnas
(assets/themes/oppo-medido.json, folder.columns = 3): 4 apps = 3 + 1. Con el estilo de carpeta activo la rejilla usa
mMaxCountX columnas y las filas necesarias."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/folder/FolderGridOrganizer.java"
t = p.read_text()
old = "        mCountX = gridCountX;\n        mCountY = gridCountY;\n    }\n"
new = ("        if (com.qtekfun.ultimatelauncher.folder.FolderStyle.fixedColumns()) { // UltimateLauncher 0052\n"
       "            gridCountX = mMaxCountX;\n"
       "            gridCountY = Math.max(1, Math.min(mMaxCountY, (count + gridCountX - 1) / gridCountX));\n"
       "        }\n" + old)
if "0052" not in t:
    assert old in t; p.write_text(t.replace(old, new, 1))
