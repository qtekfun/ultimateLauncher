#!/usr/bin/env python3
"""Parche 0050 (reaplicable, idempotente): icono de carpeta cerrada con la forma del icono (superelipse medida) y
fondo de cristal claro en vez de círculo oscuro.
 - ThemeManager.kt: con ul_icon_mask definido (y sin forma elegida por el usuario) folderShape = iconShape.
 - PreviewBackground.java: getBgColor() pasa por FolderStyle.closedIconColor (interruptor pref_ul_folder_style)."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
p = R / "graphics/ThemeManager.kt"; t = p.read_text()
old = """        val folderShape =
            if (oldState != null && oldState.folderRadius == folderRadius) {
                oldState.folderShape
            } else if (folderRadius == 1f) {"""
new = """        val folderShape =
            if (shapeModel == null && iconMask.isNotEmpty() && // UL 0050: la carpeta usa la forma del icono
                context.resources.getString(com.android.launcher3.R.string.ul_icon_mask).isNotEmpty()) {
                iconShape
            } else if (oldState != null && oldState.folderRadius == folderRadius) {
                oldState.folderShape
            } else if (folderRadius == 1f) {"""
if "UL 0050" not in t:
    assert old in t; p.write_text(t.replace(old, new, 1))
p = R / "folder/PreviewBackground.java"; t = p.read_text()
old = "    public int getBgColor() {\n        return mBgColor;\n"
new = ("    public int getBgColor() {\n"
       "        return com.qtekfun.ultimatelauncher.folder.FolderStyle.closedIconColor(mContext, mBgColor); // UltimateLauncher 0050\n")
if "0050" not in t:
    assert old in t; p.write_text(t.replace(old, new, 1))
