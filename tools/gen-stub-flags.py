#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
# SPDX-License-Identifier: Apache-2.0
"""Genera stubs com.qtekfun.stubs.*.Flags con los métodos que el código realmente usa (todos devuelven false)."""
import re, pathlib, glob
root = pathlib.Path(__file__).resolve().parent.parent
pk = {"window.flags": "com.qtekfun.stubs.window.flags", "wmshell": "com.qtekfun.stubs.wmshell",
      "sysuishared": "com.qtekfun.stubs.sysuishared", "media.flags": "com.qtekfun.stubs.media.flags",
      "multiuser": "com.qtekfun.stubs.multiuser",
      "security": "com.qtekfun.stubs.security", "appwidget.flags": "com.qtekfun.stubs.appwidget.flags"}
srcs = [p for d in ("src", "shared", "modules", "dagger", "src_no_quickstep", "tests/shared") for p in glob.glob(str(root / "launcher3-base" / d / "**/*.*"), recursive=True) if p.endswith((".java", ".kt"))]
ENABLED = {}
for k, pkg in pk.items():
    methods = set()
    for f in srcs:
        t = open(f).read()
        if pkg not in t: continue
        methods |= set(re.findall(re.escape(pkg) + r"\.Flags\.(\w+)", t))
        if re.search(r"import " + re.escape(pkg) + r"\.Flags;?", t):
            methods |= set(re.findall(r"\bFlags\.(\w+)\(", t))
    d = root / "platform-stubs/src/main/java" / pkg.replace(".", "/")
    d.mkdir(parents=True, exist_ok=True)
    body = "\n".join(f"    public static boolean {m}() {{ return {'true' if m in ENABLED else 'false'}; }}" for m in sorted(methods))
    (d / "Flags.java").write_text(f"// GENERADO por tools/gen-stub-flags.py\npackage {pkg};\n\n/** Stub de flags de plataforma (todo desactivado). */\npublic final class Flags {{\n    private Flags() {{}}\n{body}\n}}\n")
    print(pkg, sorted(methods))
