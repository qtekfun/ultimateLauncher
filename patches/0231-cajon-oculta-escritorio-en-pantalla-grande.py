#!/usr/bin/env python3
"""Parche 0231 (reaplicable): con el cajón abierto, el escritorio se oculta también en pantalla grande. Requiere 0020.

AOSP deja visibles las páginas del escritorio en pantalla grande porque allí el cajón es una hoja flotante. Con el cajón a
pantalla completa y velo translúcido (0020), los iconos del escritorio se veían atenuados encima de los del cajón (tablet y
pantalla interior de los plegables). Como en móvil, la opacidad de las páginas pasa a 0 en ALL_APPS.
"""
import pathlib
p = (pathlib.Path(__file__).resolve().parent.parent
     / "launcher3-base/src_no_quickstep/com/android/launcher3/uioverrides/states/AllAppsState.java")
old = ("                return launcher.getDeviceProfile().getDeviceProperties().isLargeScreen()\n"
       "                        ? superPageAlphaProvider.getPageAlpha(pageIndex)\n"
       "                        : 0;\n")
new = ("                return 0; // UL 0231: el cajón a pantalla completa (0020) tapa el escritorio en todos los tamaños\n")
t = p.read_text()
if new not in t:
    assert old in t
    t = t.replace(old, new, 1)
    t = t.replace("        PageAlphaProvider superPageAlphaProvider = super.getWorkspacePageAlphaProvider(launcher);\n", "", 1)
    p.write_text(t)
