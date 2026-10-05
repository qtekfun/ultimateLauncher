#!/usr/bin/env python3
"""Parche 0140 (reaplicable, idempotente): carpetas ampliables, modelo.
Antes el cargador forzaba spanX = spanY = 1 a todas las carpetas. Ahora una carpeta del escritorio puede estar guardada
como 2x2 en `favorites` (columnas spanX/spanY que ya existen: no hay migración de esquema). FolderExpand.loadedSpan
(app/.../folder/FolderExpand.kt, lógica en FolderExpandLogic.kt) valida el tamaño: solo 2x2 dentro de la rejilla y en
el escritorio; cualquier otra cosa (hotseat, fuera de rejilla, 1x2...) se carga como 1x1.
Archivo: WorkspaceItemProcessor.kt (processFolderOrAppPair)."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
p = R / "model/WorkspaceItemProcessor.kt"
t = p.read_text()
old = "        collection.spanX = 1\n        collection.spanY = 1\n"
new = ("        // UltimateLauncher 0140: una carpeta puede estar ampliada a 2x2 (el tamaño guardado se valida)\n"
       "        val ulSpan = if (collection is FolderInfo) {\n"
       "            com.qtekfun.ultimatelauncher.folder.FolderExpand.loadedSpan(\n"
       "                c.spanX, c.spanY, c.cellX, c.cellY, c.container, idp.numColumns, idp.numRows)\n"
       "        } else 1\n"
       "        collection.spanX = ulSpan\n"
       "        collection.spanY = ulSpan\n")
if "UltimateLauncher 0140" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
