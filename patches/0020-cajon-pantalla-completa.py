#!/usr/bin/env python3
"""Parche 0020 (reaplicable): cajón de aplicaciones a pantalla completa con fondo desenfocado (como OPPO).

1) ActivityAllAppsContainerView: la hoja se pinta desde y=0 (bajo la barra de estado), sin esquinas redondeadas y con
   los colores de velo de los tokens (R.color.ul_drawer_scrim_blur / ul_drawer_scrim_fallback).
2) all_apps_bottom_sheet_background.xml: sin asa visible (el área táctil del asa se conserva para cerrar arrastrando).
3) AllAppsState: sin velo propio sobre el escritorio (el velo lo pone la hoja).
4) Launcher: isAllAppsBackgroundBlurEnabled() = WindowManager.isCrossWindowBlurEnabled() (la base no trae el servicio
   de estado de desenfoque: devolvía siempre false) y onAllAppsTransition() aplica el desenfoque «blur behind» de la ventana (setBlurBehindRadius)
   proporcional al progreso. Sin desenfoque disponible (ahorro de batería, ROM) se usa el velo más opaco.
"""
import pathlib
R = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base"
J = R / "src/com/android/launcher3"
def sub(p, old, new):
    p = pathlib.Path(p); t = p.read_text()
    if new in t: return
    assert old in t, (p.name, old)
    p.write_text(t.replace(old, new, 1))

c = J / "allapps/ActivityAllAppsContainerView.java"
sub(c, "float cornerRadius = Themes.getDialogCornerRadius(getContext());",
    "float cornerRadius = 0f; // UltimateLauncher 0020: sin esquinas redondeadas (pantalla completa)")
sub(c, "mBottomSheetBackgroundColorOverBlur = ColorUtils.compositeColors(layerFg, layerBg);",
    "mBottomSheetBackgroundColorOverBlur = getContext().getColor(R.color.ul_drawer_scrim_blur); // UL 0020")
sub(c, "        mBottomSheetBackgroundColorBlurFallback = getContext().getColor(\n                Utilities.isDarkTheme(getContext()) ? android.R.color.system_accent2_800\n                        : android.R.color.system_accent2_200);",
    "        mBottomSheetBackgroundColorBlurFallback = getContext().getColor(\n                R.color.ul_drawer_scrim_fallback); // UL 0020")
sub(c, "final float topNoScale = panel.getTop() + translationY;",
    "final float topNoScale = translationY; // UL 0020: la hoja cubre también la barra de estado")

sub(R / "res/layout/all_apps_bottom_sheet_background.xml",
    '        android:id="@+id/bottom_sheet_handle"\n',
    '        android:id="@+id/bottom_sheet_handle"\n        android:visibility="gone"\n')

sub(R / "src_no_quickstep/com/android/launcher3/uioverrides/states/AllAppsState.java",
    "                        : Themes.getAttrColor(launcher, R.attr.allAppsScrimColor),",
    "                        : Color.TRANSPARENT, // UL 0020: el velo lo pinta la hoja a pantalla completa")

l = J / "Launcher.java"
sub(l, "    public void onAllAppsTransition(float progress) {\n        // No-Op\n    }",
    """    public void onAllAppsTransition(float progress) {
        // UltimateLauncher 0020: desenfoque de lo que hay detrás de la ventana (el fondo de pantalla),
        // proporcional al progreso del cajón y cuantizado en 6 pasos para no redimensionar la ventana en cada fotograma.
        int max = getResources().getDimensionPixelSize(R.dimen.ul_drawer_blur_radius);
        int radius = isAllAppsBackgroundBlurEnabled()
                ? Math.round(Math.round(Math.min(1f, Math.max(0f, progress)) * 6) / 6f * max) : 0;
        if (radius != mUlBlurRadius) {
            mUlBlurRadius = radius;
            LayoutParams lp = getWindow().getAttributes();
            lp.setBlurBehindRadius(radius);
            if (radius > 0) {
                lp.flags |= LayoutParams.FLAG_BLUR_BEHIND;
            } else {
                lp.flags &= ~LayoutParams.FLAG_BLUR_BEHIND;
            }
            getWindow().setAttributes(lp);
        }
    }

    private int mUlBlurRadius = 0;

    /** UltimateLauncher 0020: la base no enlaza el estado de desenfoque; se consulta el sistema. */
    @Override
    public boolean isAllAppsBackgroundBlurEnabled() {
        android.view.WindowManager wm = getSystemService(android.view.WindowManager.class);
        return wm != null && wm.isCrossWindowBlurEnabled();
    }""")

# 5) Hueco superior (OPPO reserva ahí las pestañas Todos/Categorías; aquí queda como aire): R.dimen.ul_drawer_top_gap
sub(c, "int padding = mHeader.getMaxTranslation();",
    "int padding = mHeader.getMaxTranslation()\n                + getResources().getDimensionPixelSize(R.dimen.ul_drawer_top_gap); // UL 0020")
