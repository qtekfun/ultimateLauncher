#!/usr/bin/env python3
"""Parche 0049 (reaplicable, idempotente): celda de la carpeta con las medidas de OPPO en teléfono
(assets/themes/oppo-medido.json, folder.cellWidthDp 114,3 y cellHeightDp 113,1) cuando el estilo de carpeta está activo
y la pantalla es de teléfono (<600 dp). Sin el parche la celda era la del escritorio (≈80 dp de ancho)."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/deviceprofile/FolderProfile.kt"
t = p.read_text()
if "UltimateLauncher 0049" not in t:
    for axis, dp in (("x", "114.3f"), ("y", "113.1f")):
        old = f"                    roundPxValueFromFloat(cellSize.{axis} * scale)\n"
        assert old in t, axis
        new = (f"                    (if (com.qtekfun.ultimatelauncher.folder.FolderStyle.phoneCells(context)) // UltimateLauncher 0049\n"
               f"                        pxFromDp({dp}, metrics, scale) else roundPxValueFromFloat(cellSize.{axis} * scale))\n")
        t = t.replace(old, new, 1)
if "ulPhone" not in t:
    # Variante no escalable (la usada por la cuadrícula ultimate_phone): sin contexto, se decide por el ancho en dp.
    old = "            val folderCellHeightPx = folderChildIconSizePx + 2 * cellPaddingY + textHeight\n"
    assert old in t
    t = t.replace(old, "            val ulPhone = metrics.widthPixels / metrics.density < 600f // UltimateLauncher 0049\n"
                       "            val folderCellHeightPx = if (ulPhone) pxFromDp(113.1f, metrics, scale)\n"
                       "                else folderChildIconSizePx + 2 * cellPaddingY + textHeight\n", 1)
    old = "                cellWidthPx = folderChildIconSizePx + 2 * cellPaddingX,\n"
    assert old in t
    t = t.replace(old, "                cellWidthPx = if (ulPhone) pxFromDp(114.3f, metrics, scale)\n"
                       "                    else folderChildIconSizePx + 2 * cellPaddingX,\n", 1)
p.write_text(t)
