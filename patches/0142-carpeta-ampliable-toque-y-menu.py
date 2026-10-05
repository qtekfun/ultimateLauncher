#!/usr/bin/env python3
"""Parche 0142 (reaplicable, idempotente): carpetas ampliables, toque y menú de pulsación larga.
 - ItemClickHandler.onClickFolderIcon: FolderExpand.onFolderClick lanza la app tocada en una carpeta ampliada; si no
   hay app bajo el dedo (fondo, borde, nombre, «+N») devuelve false y la carpeta se abre como siempre.
 - PopupDataSource.FolderSystemShortcuts: añade «Ampliar carpeta» / «Reducir carpeta» (FolderExpand.popupData).
 - Workspace.beginDragShared: con `homeScreenEditImprovements` apagado (esta build) las carpetas no abrían menú; ahora
   FolderExpand.wantsPopup(child) lo abre para las carpetas del escritorio (con el ajuste pref_ul_folder_expand activo
   o si ya están ampliadas)."""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3"
FE = "com.qtekfun.ultimatelauncher.folder.FolderExpand"


def sub(f, old, new, marker):
    p = R / f
    t = p.read_text()
    if marker in t:
        return
    assert t.count(old) == 1, (f, old, t.count(old))
    p.write_text(t.replace(old, new, 1))


sub("touch/ItemClickHandler.java",
    "    private static void onClickFolderIcon(View v) {\n",
    "    private static void onClickFolderIcon(View v) {\n"
    "        if (" + FE + ".onFolderClick((FolderIcon) v)) return; // UltimateLauncher 0142\n",
    "FolderExpand.onFolderClick")
sub("popup/PopupDataSource.kt",
    "        if (itemInfo.itemType == ITEM_TYPE_FOLDER) listOf(PopupDataSource.removePopupData) else null\n",
    "        if (itemInfo.itemType == ITEM_TYPE_FOLDER) {\n"
    "            " + FE + ".popupData(itemInfo) + listOf(PopupDataSource.removePopupData) // UltimateLauncher 0142\n"
    "        } else null\n",
    "FolderExpand.popupData")
sub("Workspace.java",
    "            } else if (((Flags.homeScreenEditImprovements() && child instanceof Poppable)\n"
    "                    || HomeScreenFilesUtilsKt.isFileSystemItem(item))\n",
    "            } else if (((Flags.homeScreenEditImprovements() && child instanceof Poppable)\n"
    "                    || " + FE + ".wantsPopup(child) // UltimateLauncher 0142\n"
    "                    || HomeScreenFilesUtilsKt.isFileSystemItem(item))\n",
    "FolderExpand.wantsPopup")
