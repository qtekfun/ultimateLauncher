#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
# SPDX-License-Identifier: GPL-3.0-or-later
"""Aplica los tokens de tablet (assets/themes/huawei-tablet-medido.json, clase tablet-landscape) a Launcher3 en compilación.

Genera: (1) una <grid-option> "ultimate_tablet" en launcher3-base/res/xml/device_profiles.xml (entre marcadores UL-TABLET),
(2) themetokens/res/values-sw600dp/ul_tablet_tokens.xml con márgenes, máscara de icono y medidas del dock, y
(3) los valores por defecto (móvil) de esos recursos en themetokens/res/values/ul_tablet_defaults.xml.
Un token null NO se escribe. Idempotente. Uso: tools/apply-tablet-tokens.py [id_tema]
"""
import json, math, pathlib, re, sys
root = pathlib.Path(__file__).resolve().parent.parent
tid = sys.argv[1] if len(sys.argv) > 1 else "huawei-tablet-medido"
theme = json.load(open(root / f"assets/themes/{tid}.json"))
tok = theme["classes"]["tablet-landscape"]
g, ic, cell, dock, cal = tok["grid"], tok["icon"], tok["cell"], tok["dock"], tok["calibration"]
ratio = cal.get("launcherVisibleRatio") or 1.0
def a(name, v, ind=12):
    return " " * ind + f'launcher:{name}="{v}"\n' if v is not None else ""
icon_dp = round(ic["sizeDp"] / ratio, 1)
grid = ('    <grid-option\n        launcher:name="ultimate_tablet"\n'
        f'        launcher:numRows="{g["rows"]}"\n        launcher:numColumns="{g["columns"]}"\n'
        '        launcher:numSearchContainerColumns="3"\n        launcher:numFolderRows="3"\n        launcher:numFolderColumns="4"\n'
        f'        launcher:numHotseatIcons="{g["dockColumns"]}"\n        launcher:numAllAppsColumns="{g["columns"]}"\n'
        '        launcher:isScalable="true"\n        launcher:inlineNavButtonsEndSpacing="@dimen/taskbar_button_margin_6_5"\n'
        '        launcher:devicePaddingId="@xml/paddings_6x5"\n        launcher:dbFile="launcher_ultimate_tablet.db"\n'
        '        launcher:defaultLayoutId="@xml/default_workspace_6x5"\n        launcher:deviceCategory="tablet" >\n\n'
        '        <display-option\n            launcher:name="Medido ' + tid + '"\n'
        '            launcher:minWidthDps="900"\n            launcher:minHeightDps="820"\n'
        f'            launcher:minCellHeight="{round(cell["heightDp"])}"\n            launcher:minCellWidth="{round(cell["widthDp"])}"\n'
        f'            launcher:minCellHeightLandscape="{round(cell["heightDp"])}"\n            launcher:minCellWidthLandscape="{round(cell["widthDp"])}"\n'
        '            launcher:borderSpaceHorizontal="0"\n            launcher:borderSpaceVertical="0"\n'
        '            launcher:borderSpaceLandscapeHorizontal="0"\n            launcher:borderSpaceLandscapeVertical="0"\n'
        f'            launcher:iconImageSize="{icon_dp}"\n            launcher:iconTextSize="14"\n'
        f'            launcher:horizontalMargin="{cell["horizontalMarginDp"] / 2}"\n            launcher:horizontalMarginLandscape="{cell["horizontalMarginDp"]}"\n'
        f'            launcher:allAppsCellWidth="{round(cell["widthDp"])}"\n            launcher:allAppsCellHeight="{round(cell["heightDp"] + 20)}"\n'
        f'            launcher:allAppsCellWidthLandscape="{round(cell["widthDp"])}"\n            launcher:allAppsCellHeightLandscape="{round(cell["heightDp"] + 20)}"\n'
        f'            launcher:allAppsIconSize="{icon_dp}"\n            launcher:allAppsIconTextSize="14"\n'
        '            launcher:allAppsBorderSpaceHorizontal="0"\n            launcher:allAppsBorderSpaceVertical="0"\n            launcher:allAppsBorderSpaceLandscape="0"\n'
        f'            launcher:hotseatBarBottomSpace="{cal.get("hotseatBottomSpaceDp", 12)}"\n            launcher:hotseatBarBottomSpaceLandscape="{cal.get("hotseatBottomSpaceDp", 12)}"\n'
        '            launcher:canBeDefault="true" />\n\n    </grid-option>\n')
begin, end = "    <!-- UL-TABLET-BEGIN: generado por tools/apply-tablet-tokens.py -->\n", "    <!-- UL-TABLET-END -->\n"
p = root / "launcher3-base/res/xml/device_profiles.xml"
t = p.read_text(); block = begin + grid + end + "\n"
if begin in t:
    t = re.sub(re.escape(begin) + r".*?" + re.escape(end) + r"\n", lambda m: block, t, flags=re.S)
else:
    marker = "    <!-- UL-TOKENS-BEGIN"
    t = t.replace(marker, block + marker, 1) if marker in t else t.replace("    <grid-option\n", block + "    <grid-option\n", 1)
p.write_text(t)
# máscara de icono: superelipse |x|^n + |y|^n = 1 en 100x100 como trazado poligonal
n = ic.get("exponent", 5)
pts = []
for i in range(0, 96):
    th = 2 * math.pi * i / 96
    c, s = math.cos(th), math.sin(th)
    pts.append((50 + 50 * math.copysign(abs(c) ** (2 / n), c), 50 + 50 * math.copysign(abs(s) ** (2 / n), s)))
mask = "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z"
px = lambda dp: dp  # ya en dp
tabx = ('<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERADO por tools/apply-tablet-tokens.py -->\n<resources>\n'
        f'    <string name="ul_icon_mask" translatable="false">{mask}</string>\n'
        f'    <bool name="ul_huawei_dock">true</bool>\n'
        f'    <integer name="ul_dock_recents_max">{dock["recentsMax"]}</integer>\n'
        f'    <dimen name="ul_dock_pill_height">{dock["pillHeightDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_pill_radius">{dock["pillCornerRadiusDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_pill_padding">{dock["pillPaddingDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_cell">{dock["cellDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_gap">{dock["gapDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_icon">{dock["iconDp"]}dp</dimen>\n'
        f'    <dimen name="ul_dock_bottom_margin">{dock["bottomMarginDp"]}dp</dimen>\n'
        f'    <dimen name="ul_workspace_top_padding">{cal.get("workspaceTopPaddingDp", 0)}dp</dimen>\n'
        f'    <dimen name="ul_workspace_bottom_padding">{cal.get("workspaceBottomPaddingDp", 0)}dp</dimen>\n')
if cal.get("workspaceSideMarginDp") is not None:
    tabx += f'    <dimen name="ul_workspace_side_margin">{cal["workspaceSideMarginDp"]}dp</dimen>\n'
tabx += '</resources>\n'
o = root / "themetokens/res/values-sw600dp/ul_tablet_tokens.xml"; o.parent.mkdir(parents=True, exist_ok=True); o.write_text(tabx)
d = root / "themetokens/res/values/ul_tablet_defaults.xml"
d.write_text('<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERADO por tools/apply-tablet-tokens.py: valores por defecto (móvil) -->\n<resources>\n'
             '    <string name="ul_icon_mask" translatable="false"></string>\n    <bool name="ul_huawei_dock">false</bool>\n'
             '    <integer name="ul_dock_recents_max">0</integer>\n'
             + "".join(f'    <dimen name="ul_dock_{k}">0dp</dimen>\n' for k in ("pill_height", "pill_radius", "pill_padding", "cell", "gap", "icon", "bottom_margin"))
             + '</resources>\n')
print("ok tablet", g, "icono dp", icon_dp)
