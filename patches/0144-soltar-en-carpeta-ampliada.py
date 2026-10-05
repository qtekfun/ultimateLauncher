#!/usr/bin/env python3
"""Parche 0144 (reaplicable, idempotente): se pueden soltar iconos en una carpeta ampliada (2x2).

Workspace comprueba que el dedo esté a menos de getFolderCreationRadius (≈ medio icono) del CENTRO de la celda de
destino. En una carpeta ampliada solo la celda del centro de cada cuarto cumple esa distancia y el resto del
cuadrado rechaza la suelta. Con una carpeta ampliada bajo la celda de destino, cualquier punto de su baldosa vale
(FolderExpand.beyondFolderRadius). Afecta a manageFolderFeedback, willAddToExistingUserFolder y
addToExistingFolderIfNecessary (no a la creación de carpetas nuevas)."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/Workspace.java"
t = p.read_text()
H = "com.qtekfun.ultimatelauncher.folder.FolderExpand.beyondFolderRadius"
def sub(old, new):
    global t
    if new in t: return
    assert old in t, old
    t = t.replace(old, new, 1)
sub("""    boolean willAddToExistingUserFolder(ItemInfo dragInfo, CellLayout target, int[] targetCell,
                                        float distance) {
        if (distance > target.getFolderCreationRadius(targetCell)) return false;""",
    f"""    boolean willAddToExistingUserFolder(ItemInfo dragInfo, CellLayout target, int[] targetCell,
                                        float distance) {{
        if ({H}(target, targetCell, distance)) return false; // UltimateLauncher 0144""")
sub("""    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
            float distance, DragObject d, boolean external) {
        if (distance > target.getFolderCreationRadius(targetCell)) return false;""",
    f"""    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
            float distance, DragObject d, boolean external) {{
        if ({H}(target, targetCell, distance)) return false; // UltimateLauncher 0144""")
sub("""    private void manageFolderFeedback(float distance, DragObject dragObject) {
        if (distance > mDragTargetLayout.getFolderCreationRadius(mTargetCell)) {""",
    f"""    private void manageFolderFeedback(float distance, DragObject dragObject) {{
        if ({H}(mDragTargetLayout, mTargetCell, distance)) {{ // UltimateLauncher 0144""")
p.write_text(t)
