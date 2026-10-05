#!/usr/bin/env python3
"""Parche 0032 (reaplicable): los `@color/system_*` PROPIOS de Launcher3 solo existían en values-v34.

`launcher3-base/res/values-v34/colors.xml` define colores de la app llamados `system_primary_light`, `system_surface_container_dark`...
(alias de `@android:color/system_*`, que son de API 34). `values/colors.xml` y `values-night/colors.xml` los usan
(selector de widgets: cursor, botón «añadir», carpeta de archivos...). En Android 12-13 no había definición para esa configuración:
`Resources.NotFoundException` al abrir el selector de widgets. Aquí se genera `values/ul_system_colors_fallback.xml` con los mismos
nombres y la correspondencia M3 -> paleta de API 31 del parche 0015 (en API >= 34 manda `values-v34`).
Requiere haber aplicado antes 0008 y 0015 (usa la tabla `M` de 0015).
"""
import ast, pathlib, re
root = pathlib.Path(__file__).resolve().parent.parent
src15 = (root / "patches/0015-colores-fallback-api31.py").read_text()
M = None
for node in ast.parse(src15).body:
    if isinstance(node, ast.Assign) and getattr(node.targets[0], "id", "") == "M":
        M = ast.literal_eval(node.value)
assert M, "tabla M de 0015 no encontrada"
v34 = (root / "launcher3-base/res/values-v34/colors.xml").read_text()
pat = re.compile(r'^\s*<color name="([^"]+)">(.*?)</color>\s*$', re.M)
ref = re.compile(r"^@android:color/system_([a-z0-9_]+?)(?:_(light|dark))?$")  # los *_fixed no llevan sufijo
lines = []
for name, val in pat.findall(v34):
    m = ref.match(val.strip())
    if m:
        tok, mode = m.groups()
        mode = mode or "light"  # *_fixed: mismo valor en claro y oscuro (tabla M)
        if tok.startswith(("accent", "neutral")):
            new = val.strip()
        else:
            v = M[tok][0 if mode == "light" else 1]
            new = v[4:] if v.startswith("lit:") else "@android:color/system_" + v
    else:
        new = val.strip()  # literal (p. ej. los de API 37 ya sustituidos por 0008)
    lines.append(f'    <color name="{name}">{new}</color>')
out = root / "launcher3-base/res/values/ul_system_colors_fallback.xml"
out.write_text('<?xml version="1.0" encoding="utf-8"?>\n'
               '<!-- Parche 0032: alternativa (Android 12-13) de los colores system_* de Launcher3; en API 34+ manda values-v34. Generado. -->\n'
               '<resources>\n' + "\n".join(lines) + "\n</resources>\n")
