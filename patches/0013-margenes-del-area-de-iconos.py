#!/usr/bin/env python3
"""Parche 0013 (reaplicable): márgenes superior/inferior del área de iconos desde recursos de tokens.

WorkspaceProfileNonResponsiveFactory pasa `workspaceTopPadding = 0` y `workspaceBottomPadding = 0` (4 sitios).
Se leen de R.dimen.ul_workspace_top_padding / ul_workspace_bottom_padding / ul_workspace_side_margin (generados por tools/apply-theme-tokens.py).
"""
import pathlib
p = pathlib.Path(__file__).resolve().parent.parent / "launcher3-base/src/com/android/launcher3/deviceprofile/WorkspaceProfileNonResponsiveFactory.kt"
t = p.read_text()
t = t.replace("workspaceTopPadding = 0,", "workspaceTopPadding = res.getDimensionPixelSize(R.dimen.ul_workspace_top_padding), // UL 0013")
t = t.replace("workspaceBottomPadding = 0,", "workspaceBottomPadding = res.getDimensionPixelSize(R.dimen.ul_workspace_bottom_padding), // UL 0013")
t = t.replace("else -> res.getDimensionPixelSize(R.dimen.dynamic_grid_left_right_margin)", "else -> res.getDimensionPixelSize(R.dimen.ul_workspace_side_margin) // UL 0013")
p.write_text(t)
