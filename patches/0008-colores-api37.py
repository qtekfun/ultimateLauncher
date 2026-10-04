#!/usr/bin/env python3
"""Parche 0008 (reaplicable): colores @android:color/system_* que solo existen desde API 37.

Afecta a: inverse_on_surface, inverse_primary, inverse_surface, scrim, shadow, surface_tint (light y dark).
En Android 16 (API 36) fallarían con Resources.NotFoundException al usarse. Se mueven a values-v37[-night] (valor
original) y en el resto se deja la línea base de Material 3 como alternativa.
Archivos: launcher3-base/res/values-v34/colors.xml, systemui-libs/dynamiccolors/res/values{,-night}/colors.xml
"""
import pathlib, re
root = pathlib.Path(__file__).resolve().parent.parent
BASE = {  # línea base Material 3 (valores de la especificación pública de M3)
 "inverse_on_surface": ("#F4EFF4", "#313033"), "inverse_primary": ("#D0BCFF", "#6750A4"),
 "inverse_surface": ("#313033", "#E6E1E5"), "scrim": ("#000000", "#000000"),
 "shadow": ("#000000", "#000000"), "surface_tint": ("#6750A4", "#D0BCFF"),
}
pat = re.compile(r'^(\s*)<color name="([^"]+)">@android:color/system_(inverse_on_surface|inverse_primary|inverse_surface|scrim|shadow|surface_tint)_(light|dark)</color>\s*$', re.M)
def process(src, v37dir):
    t = src.read_text()
    moved = []
    def rep(m):
        ind, name, key, mode = m.groups()
        moved.append(f'    <color name="{name}">@android:color/system_{key}_{mode}</color>')
        return f'{ind}<color name="{name}">{BASE[key][0 if mode == "light" else 1]}</color>'
    new = pat.sub(rep, t)
    if not moved: return
    src.write_text(new)
    out = root / v37dir / "colors.xml"; out.parent.mkdir(parents=True, exist_ok=True)
    head = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Parche 0008: colores del sistema que solo existen desde API 37 -->\n<resources>\n'
    prev = out.read_text().split("<resources>\n")[1].rsplit("</resources>", 1)[0] if out.exists() else ""
    out.write_text(head + prev + "\n".join(moved) + "\n</resources>\n")
process(root / "launcher3-base/res/values-v34/colors.xml", "launcher3-base/res/values-v37")
process(root / "systemui-libs/dynamiccolors/res/values/colors.xml", "systemui-libs/dynamiccolors/res/values-v37")
process(root / "systemui-libs/dynamiccolors/res/values-night/colors.xml", "systemui-libs/dynamiccolors/res/values-night-v37")
