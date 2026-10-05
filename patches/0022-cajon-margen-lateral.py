#!/usr/bin/env python3
"""Parche 0022 (reaplicable): margen lateral de la rejilla del cajón en móvil desde tokens (drawer.sidePaddingDp).

AllAppsProfile.calculateNonResponsivePadding (rama no grande) lo derivaba del margen del escritorio; OPPO usa 14,9 dp.
Se lee R.dimen.ul_drawer_side_padding (generado por tools/apply-theme-tokens.py).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/deviceprofile/AllAppsProfile.kt"
t = p.read_text()
old = "            allAppsStyle.recycle()\n            return Rect(\n                /* left */ leftAndRight,"
new = ("            if (!deviceProperties.isLargeScreen) {\n"
       "                leftAndRight = context.resources.getDimensionPixelSize(R.dimen.ul_drawer_side_padding) // UL 0022\n"
       "            }\n") + old
if "UL 0022" not in t:
    assert old in t
    p.write_text(t.replace(old, new, 1))
