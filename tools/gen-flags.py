#!/usr/bin/env python3
"""Genera com.android.launcher3.Flags a partir de launcher3-base/aconfig/*.aconfig.

aconfig no está disponible fuera del árbol de AOSP y los valores de release viven en otro repositorio,
así que todos los flags quedan en su valor por defecto 'false' salvo los listados en ENABLED.
"""
import re, glob, pathlib
ENABLED = set()   # nombres (snake_case) que se fuerzan a true
root = pathlib.Path(__file__).resolve().parent.parent
flags = []
for f in sorted(glob.glob(str(root / "launcher3-base/aconfig/*.aconfig"))):
    txt = open(f).read()
    pkg = re.search(r'package:\s*"([^"]+)"', txt).group(1)
    for m in re.finditer(r'flag\s*\{(.*?)\n\s*\}', txt, re.S):
        n = re.search(r'name:\s*"([^"]+)"', m.group(1)).group(1)
        flags.append((pkg, n))
def camel(n):
    p = n.split("_"); return p[0] + "".join(x.capitalize() for x in p[1:])
out = ["// GENERADO por tools/gen-flags.py. No editar.", "package com.android.launcher3;", "", "/** Sustituto de la clase que genera aconfig. */", "public final class Flags {", "    private Flags() {}"]
for pkg, n in flags:
    out.append(f'    public static final String FLAG_{n.upper()} = "{pkg}.{n}";')
for pkg, n in flags:
    out.append(f"    public static boolean {camel(n)}() {{ return {'true' if n in ENABLED else 'false'}; }}")
out.append("}")
d = root / "platform-stubs/src/main/java/com/android/launcher3"
d.mkdir(parents=True, exist_ok=True)
(d / "Flags.java").write_text("\n".join(out) + "\n")
print(len(flags), "flags")
