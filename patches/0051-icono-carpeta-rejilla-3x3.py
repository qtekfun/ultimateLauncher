#!/usr/bin/env python3
"""Parche 0051 (reaplicable, idempotente): vista previa de la carpeta cerrada como rejilla 3x3 de iconos pequeños.

Medido en el launcher de OPPO (captura propia, 1440 px de ancho, icono de carpeta 198 px): mini-iconos de ≈40 px
(20,2 % del lado), paso 54 px (27,3 %), margen 25 px (12,6 %), orden de lectura, hasta 9 elementos.
AOSP: círculo de 4 elementos. ClippedFolderIconLayoutRule: MAX_NUM_ITEMS_IN_PREVIEW 4 -> 9, getPosition/getGridPosition
en rejilla y escala fija para la página 0."""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/folder/ClippedFolderIconLayoutRule.java"
t = p.read_text()
if "UltimateLauncher 0051" in t:
    raise SystemExit
def sub(old, new):
    global t
    assert old in t, old
    t = t.replace(old, new, 1)
sub("public static final int MAX_NUM_ITEMS_IN_PREVIEW = 4;",
    "public static final int MAX_NUM_ITEMS_IN_PREVIEW = 9; // UltimateLauncher 0051\n"
    "    // Rejilla 3x3 medida en OPPO: fracciones del lado del icono de carpeta.\n"
    "    private static final float UL_GRID_PAD = 0.126f;\n"
    "    private static final float UL_GRID_PITCH = 0.273f;\n"
    "    private static final float UL_GRID_ICON = 0.202f;")
sub("""    private void getGridPosition(int row, int col, float[] result) {
""", """    private void getGridPosition(int row, int col, float[] result) {
        if (true) { // UltimateLauncher 0051
            result[0] = mAvailableSpace * (UL_GRID_PAD + col * UL_GRID_PITCH);
            result[1] = mAvailableSpace * (UL_GRID_PAD + row * UL_GRID_PITCH);
            return;
        }
""")
sub("""    private void getPosition(int index, int curNumItems, float[] result) {
""", """    private void getPosition(int index, int curNumItems, float[] result) {
        if (true) { // UltimateLauncher 0051
            getGridPosition(index / 3, index % 3, result);
            return;
        }
""")
sub("""        float scale;
        if (page > 0) {""", """        float scale;
        if (page == 0) { // UltimateLauncher 0051
            return UL_GRID_ICON * mBaselineIconScale;
        }
        if (page > 0) {""")
p.write_text(t)
