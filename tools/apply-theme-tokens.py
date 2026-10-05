#!/usr/bin/env python3
"""Aplica los tokens medidos (assets/themes/<id>.json, clase 'phone') a Launcher3 en tiempo de compilación.

Genera una <grid-option> "ultimate_phone" dentro de launcher3-base/res/xml/device_profiles.xml (entre marcadores)
y deja que InvariantDeviceProfile la use por defecto (parche 0009). Un token null NO se escribe: se conserva el valor de AOSP.
Uso: tools/apply-theme-tokens.py [id_tema]   (por defecto oppo-medido). Es idempotente.
"""
import json, pathlib, re, sys
root = pathlib.Path(__file__).resolve().parent.parent
tid = sys.argv[1] if len(sys.argv) > 1 else "oppo-medido"
tok = json.load(open(root / f"assets/themes/{tid}.json"))["classes"]["phone"]
if not tok or not tok.get("grid"):
    sys.exit("la clase phone no tiene tokens")
g, ic, dr, fo = tok["grid"], tok.get("icon") or {}, tok.get("drawer") or {}, tok.get("folder") or {}
cal = tok.get("calibration") or {}
ratio = cal.get("launcherVisibleRatio") or 1.0
src = json.load(open(root / f"assets/themes/{tid}.json"))["source"]
w_dp = round(int(src["screenPx"].split("x")[0]) / (src["densityDpi"] / 160))
h_dp = round(int(src["screenPx"].split("x")[1]) / (src["densityDpi"] / 160))
def attr(name, v, ind=8):
    return ' ' * ind + f'launcher:{name}="{v}"\n' if v is not None else ""
grid = ('    <grid-option\n        launcher:name="ultimate_phone"\n'
        + attr("numRows", g.get("rows")) + attr("numColumns", g.get("columns"))
        + attr("numFolderColumns", fo.get("columns"))
        + '        launcher:numFolderRows="4"\n'
        + attr("numHotseatIcons", g.get("dockColumns"))
        + '        launcher:numExtendedHotseatIcons="6"\n        launcher:dbFile="launcher.db"\n'
        + '        launcher:inlineNavButtonsEndSpacing="@dimen/taskbar_button_margin_split"\n'
        + '        launcher:defaultLayoutId="@xml/default_workspace_5x5"\n        launcher:deviceCategory="phone" >\n\n'
        + '        <display-option\n            launcher:name="Medido ' + tid + '"\n'
        + f'            launcher:minWidthDps="{w_dp - 20}"\n            launcher:minHeightDps="{h_dp - 20}"\n'
        + attr("iconImageSize", round(ic["sizeDp"] / ratio, 1) if ic.get("sizeDp") else None, 12) + attr("iconTextSize", ic.get("labelSizeSp") or 14.4, 12)
        + attr("allAppsIconSize", round(ic["sizeDp"] / (cal.get("allAppsVisibleRatio") or ratio), 1) if ic.get("sizeDp") else None, 12)
        + attr("allAppsBorderSpace", dr.get("borderSpaceDp", 16), 12)
        + attr("allAppsCellHeight", round(dr["cellHeightDp"]) if dr.get("cellHeightDp") else None, 12)
        + '            launcher:canBeDefault="true" />\n\n    </grid-option>\n')
grid = re.sub(r"^        launcher:(\w+)=", r"        launcher:\1=", grid, flags=re.M)
begin, end = "    <!-- UL-TOKENS-BEGIN: generado por tools/apply-theme-tokens.py -->\n", "    <!-- UL-TOKENS-END -->\n"
p = root / "launcher3-base/res/xml/device_profiles.xml"
t = p.read_text()
block = begin + grid + end + "\n"
if begin in t:
    t = re.sub(re.escape(begin) + r".*?" + re.escape(end) + r"\n", lambda m: block, t, flags=re.S)
else:
    t = t.replace("    <grid-option\n", block + "    <grid-option\n", 1)
p.write_text(t)
# parche 0009: la rejilla por defecto es la de los tokens
idp = root / "launcher3-base/src/com/android/launcher3/InvariantDeviceProfile.java"
s = idp.read_text()
old = "        List<DisplayOption> profiles = DisplayOption.getPredefinedDisplayOptions(\n                displayInfo, isFixedLandscapeMode);\n"
new = "        // UltimateLauncher 0009: rejilla por defecto = tokens medidos (tools/apply-theme-tokens.py).\n        if (TextUtils.isEmpty(gridName)) gridName = displayInfo.getDeviceType() == TYPE_TABLET ? \"ultimate_tablet\" : \"ultimate_phone\";\n" + old
if "UltimateLauncher 0009" not in s:
    assert old in s; idp.write_text(s.replace(old, new))
res = root / "themetokens/res/values/ul_theme_tokens.xml"
res.parent.mkdir(parents=True, exist_ok=True)
res.write_text('<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERADO por tools/apply-theme-tokens.py -->\n<resources>\n'
               f'    <dimen name="ul_workspace_top_padding">{cal.get("workspaceTopPaddingDp", 0)}dp</dimen>\n'
               f'    <dimen name="ul_workspace_bottom_padding">{cal.get("workspaceBottomPaddingDp", 0)}dp</dimen>\n'
               f'    <dimen name="ul_workspace_side_margin">{cal.get("workspaceSideMarginDp", 8)}dp</dimen>\n'
               f'    <dimen name="ul_page_indicator_lift">{cal.get("pageIndicatorLiftDp", 0)}dp</dimen>\n</resources>\n')
print("ok", g, ic.get("sizeDp"), dr.get("cellHeightDp"))
