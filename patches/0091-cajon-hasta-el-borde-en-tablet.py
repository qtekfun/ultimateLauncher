#!/usr/bin/env python3
"""Parche 0091 (reaplicable): en tablet el panel/velo del cajon llega hasta los bordes de la pantalla.

En pantallas grandes AOSP centra la rejilla del cajon rellenando ActivityAllAppsContainerView con `leftRightMargin`
(en la MatePad 235 px por lado). Como el panel de fondo (`bottom_sheet_background`, match_parent) es hijo de ese contenedor, el velo
tambien quedaba recortado y se veia el fondo de pantalla a los lados. Ahora el panel recibe margenes negativos iguales al relleno:
el fondo ocupa todo el ancho y los iconos siguen donde estaban. Solo con `isLargeScreen` (no cambia el movil).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java"
t = p.read_text()
old = """            setPadding(grid.getAllAppsProfile().getLeftRightMargin(), topPadding,
                    grid.getAllAppsProfile().getLeftRightMargin(), 0);
        }
"""
new = old + """        // UltimateLauncher 0091: el panel de fondo del cajon a pantalla completa en tablet (compensa el relleno lateral)
        if (mBottomSheetBackground != null
                && mBottomSheetBackground.getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams plp = (MarginLayoutParams) mBottomSheetBackground.getLayoutParams();
            int extra = grid.getDeviceProperties().isLargeScreen() ? -getPaddingLeft() : 0;
            int extraEnd = grid.getDeviceProperties().isLargeScreen() ? -getPaddingRight() : 0;
            if (plp.leftMargin != extra || plp.rightMargin != extraEnd) {
                plp.leftMargin = extra;
                plp.rightMargin = extraEnd;
                mBottomSheetBackground.setLayoutParams(plp);
            }
        }
"""
if "UltimateLauncher 0091" not in t:
    assert t.count(old) == 1
    p.write_text(t.replace(old, new, 1))
