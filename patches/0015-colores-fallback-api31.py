#!/usr/bin/env python3
"""Parche 0015 (reaplicable): colores de sistema de API 34 -> alternativa con la paleta de API 31 (Android 12-13).

Los @android:color/system_<token>_{light,dark} (primary, on_surface, surface_container...) solo existen desde API 34. En Android 12
los vectores con tint="@color/materialColor..." fallan con «Can't convert to ComplexColor» y el launcher no arranca (hallado en la
tablet MRO-W09). Se usa la correspondencia estándar de Material 3 sobre la paleta tonal de API 31 (system_accent1/2/3, neutral1/2).
  - launcher3-base/res/values-v31 y values-night-v31: se sustituyen in situ (las versiones v34 ya traen los valores reales).
  - systemui-libs/dynamiccolors: res/values{,-night} pasa a values-v34{,-night-v34}; en values{,-night} queda la alternativa.
"""
import pathlib, re, shutil
root = pathlib.Path(__file__).resolve().parent.parent
A = lambda n, t: f"@android:color/system_{n}_{t}"
# token -> (claro, oscuro). Valor 'lit:#RRGGBB' = literal M3 baseline (error, scrim...).
M = {
 "primary": ("accent1_600", "accent1_200"), "on_primary": ("accent1_0", "accent1_800"),
 "primary_container": ("accent1_100", "accent1_700"), "on_primary_container": ("accent1_900", "accent1_100"),
 "secondary": ("accent2_600", "accent2_200"), "on_secondary": ("accent2_0", "accent2_800"),
 "secondary_container": ("accent2_100", "accent2_700"), "on_secondary_container": ("accent2_900", "accent2_100"),
 "tertiary": ("accent3_600", "accent3_200"), "on_tertiary": ("accent3_0", "accent3_800"),
 "tertiary_container": ("accent3_100", "accent3_700"), "on_tertiary_container": ("accent3_900", "accent3_100"),
 "error": ("lit:#BA1A1A", "lit:#FFB4AB"), "on_error": ("lit:#FFFFFF", "lit:#690005"),
 "error_container": ("lit:#FFDAD6", "lit:#93000A"), "on_error_container": ("lit:#410002", "lit:#FFDAD6"), "error_dim": ("lit:#BA1A1A", "lit:#FFB4AB"),
 "background": ("neutral1_10", "neutral1_900"), "on_background": ("neutral1_900", "neutral1_100"),
 "surface": ("neutral1_10", "neutral1_900"), "on_surface": ("neutral1_900", "neutral1_100"),
 "surface_variant": ("neutral2_100", "neutral2_700"), "on_surface_variant": ("neutral2_700", "neutral2_200"),
 "outline": ("neutral2_500", "neutral2_400"), "outline_variant": ("neutral2_200", "neutral2_700"),
 "inverse_surface": ("neutral1_800", "neutral1_100"), "inverse_on_surface": ("neutral1_50", "neutral1_800"), "inverse_primary": ("accent1_200", "accent1_600"),
 "surface_bright": ("neutral1_10", "neutral1_700"), "surface_dim": ("neutral1_200", "neutral1_900"),
 "surface_container_lowest": ("neutral1_0", "neutral1_1000"), "surface_container_low": ("neutral1_50", "neutral1_900"),
 "surface_container": ("neutral1_100", "neutral1_900"), "surface_container_high": ("neutral1_100", "neutral1_800"),
 "surface_container_highest": ("neutral1_200", "neutral1_700"),
 "primary_fixed": ("accent1_100", "accent1_100"), "primary_fixed_dim": ("accent1_200", "accent1_200"),
 "on_primary_fixed": ("accent1_900", "accent1_900"), "on_primary_fixed_variant": ("accent1_700", "accent1_700"),
 "secondary_fixed": ("accent2_100", "accent2_100"), "secondary_fixed_dim": ("accent2_200", "accent2_200"),
 "on_secondary_fixed": ("accent2_900", "accent2_900"), "on_secondary_fixed_variant": ("accent2_700", "accent2_700"),
 "tertiary_fixed": ("accent3_100", "accent3_100"), "tertiary_fixed_dim": ("accent3_200", "accent3_200"),
 "on_tertiary_fixed": ("accent3_900", "accent3_900"), "on_tertiary_fixed_variant": ("accent3_700", "accent3_700"),
 "surface_tint": ("accent1_600", "accent1_200"), "scrim": ("lit:#000000", "lit:#000000"), "shadow": ("lit:#000000", "lit:#000000"),
 "control_activated": ("accent1_600", "accent1_200"), "control_normal": ("neutral2_700", "neutral2_200"), "control_highlight": ("lit:#1F000000", "lit:#33FFFFFF"),
 "text_hint_inverse": ("neutral1_700", "neutral1_400"), "palette_key_color_primary": ("accent1_500", "accent1_500"),
 "palette_key_color_secondary": ("accent2_500", "accent2_500"), "palette_key_color_tertiary": ("accent3_500", "accent3_500"),
 "palette_key_color_neutral": ("neutral1_500", "neutral1_500"), "primary_dim": ("accent1_500", "accent1_300"), "secondary_dim": ("accent2_500", "accent2_300"), "tertiary_dim": ("accent3_500", "accent3_300"),
 "text_primary_inverse": ("neutral1_50", "neutral1_900"), "text_primary_inverse_disable_only": ("neutral1_400", "neutral1_600"),
 "text_secondary_and_tertiary_inverse": ("neutral2_200", "neutral2_700"), "text_secondary_and_tertiary_inverse_disabled": ("neutral2_400", "neutral2_600"),
 "palette_key_color_error": ("lit:#BA1A1A", "lit:#BA1A1A"), "palette_key_color_neutral_variant": ("neutral2_500", "neutral2_500"),
}
pat = re.compile(r"@android:color/system_([a-z0-9_]+?)_(light|dark)\b")
def fb(m):
    tok, mode = m.group(1), m.group(2)
    if tok.startswith(("accent", "neutral")): return m.group(0)
    if tok not in M: raise SystemExit(f"token sin correspondencia: {tok}")
    v = M[tok][0 if mode == "light" else 1]
    return v[4:] if v.startswith("lit:") else A(v, "")[:-1] if False else "@android:color/system_" + v
def conv(text):
    out = pat.sub(fb, text)
    return re.sub(r">lit:", ">", out)
# literales: <color ...>@android:color/system_lit:#XXXX -> #XXXX
def fix_lit(t): return re.sub(r"@android:color/system_lit:(#[0-9A-Fa-f]+)", r"\1", t)
def apply_inplace(p):
    t = p.read_text(); n = fix_lit(pat.sub(fb, t))
    if n != t: p.write_text(n)
for f in ("launcher3-base/res/values-v31/colors.xml", "launcher3-base/res/values-night-v31/colors.xml"):
    apply_inplace(root / f)
dc = root / "systemui-libs/dynamiccolors/res"
for src, v34 in (("values", "values-v34"), ("values-night", "values-night-v34")):
    s = dc / src / "colors.xml"; d = dc / v34 / "colors.xml"
    if not d.exists():
        d.parent.mkdir(parents=True, exist_ok=True); shutil.copy(s, d)
    s.write_text(fix_lit(pat.sub(fb, d.read_text())))
