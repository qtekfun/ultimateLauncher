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
    t = t.replace(old, "            val ulPhone = minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density < 600f // UltimateLauncher 0049\n"
                       "            val folderCellHeightPx = if (ulPhone) pxFromDp(113.1f, metrics, scale)\n"
                       "                else folderChildIconSizePx + 2 * cellPaddingY + textHeight\n", 1)
    old = "                cellWidthPx = folderChildIconSizePx + 2 * cellPaddingX,\n"
    assert old in t
    t = t.replace(old, "                cellWidthPx = if (ulPhone) pxFromDp(114.3f, metrics, scale)\n"
                       "                    else folderChildIconSizePx + 2 * cellPaddingX,\n", 1)
p.write_text(t)
# --- Ampliación (2026-10-05): teléfono = lado corto < 600 dp (también en horizontal) y filas que caben (FolderStyle.rowsFor).
# Si el archivo ya se parcheó con la versión anterior, aplicar a mano lo que muestra FolderProfile.kt (marcas «UltimateLauncher 0049»).
t = p.read_text()
t = t.replace("metrics.widthPixels / metrics.density < 600f", "minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density < 600f")
if "FolderStyle.rowsFor" not in t:
    parts = t.split("                numRows = inv.numFolderRows[typeIndex],\n")
    assert len(parts) == 4
    t = (parts[0] + "                numRows = inv.numFolderRows[typeIndex],\n" + parts[1]
         + "                numRows = com.qtekfun.ultimatelauncher.folder.FolderStyle.rowsFor(inv.numFolderRows[typeIndex], folderCellHeightPx, roundPxValueFromFloat(folderFooterHeightPx * scale), metrics,\n                    com.qtekfun.ultimatelauncher.folder.FolderStyle.phoneCells(context)), // UltimateLauncher 0049\n" + parts[2]
         + "                numRows = com.qtekfun.ultimatelauncher.folder.FolderStyle.rowsFor(inv.numFolderRows[typeIndex], folderCellHeightPx,\n                    roundPxValueFromFloat(res.getDimensionPixelSize(R.dimen.folder_footer_height_default) * scale), metrics, ulPhone), // UltimateLauncher 0049\n" + parts[3])
p.write_text(t)

t = p.read_text()
old = "            val folderChildTextSizePx = pxFromSp(invIconTextSizeDp, metrics, scale)\n            val textHeight: Int"
if "0049b" not in t:
    assert old in t
    t = t.replace(old, "            // UltimateLauncher 0049b: etiqueta de las apps dentro de la carpeta de OPPO ≈ 0,87 de la del escritorio (teléfono)\n"
                       "            val folderChildTextSizePx = (pxFromSp(invIconTextSizeDp, metrics, scale) *\n"
                       "                (if (metrics.widthPixels / metrics.density < 600f) 0.87f else 1f)).toInt()\n"
                       "            val textHeight: Int", 1)
    p.write_text(t)
